# Analysis Document
## Transaction Behavior and Performance Findings
### Library Loan Management System — JDBC + Apache Derby

---

## Part 1: Transaction Management & ACID Integrity

### 1.1 How Transaction Boundaries Preserve Data Integrity

The `processLoan()` operation is a textbook multi-step business transaction that must succeed entirely or not at all. It touches three tables: `Loans` (INSERT), `Books` (UPDATE availability), and `Members` (UPDATE active loan count). If any step fails — for example, the `Books` row is missing, or a `CHECK` constraint violation occurs — the entire operation must be undone. This is enforced by disabling `auto-commit` at the start and wrapping all three DML statements in a single `try-catch-finally` block:

```
conn.setAutoCommit(false);   // BEGIN TRANSACTION
try {
    // Step 1: verify availability
    // Step 2: INSERT Loans
    // SAVEPOINT set here
    // Step 3: UPDATE Books
    // Step 4: UPDATE Members
    conn.commit();           // all steps OK
} catch (SQLException e) {
    conn.rollback();         // undo everything
} finally {
    conn.setAutoCommit(true);
}
```

Without this pattern, a crash between Step 2 and Step 3 would leave a `Loans` row with no corresponding decrease in `Books.AvailableCopies` — corrupting the database. Derby's WAL (Write-Ahead Log) guarantees that a committed transaction survives a JVM crash, while an uncommitted transaction leaves no trace.

### 1.2 Savepoints: Partial Rollback Without Losing Everything

After inserting the loan record (Step 2), the code sets a named savepoint:

```java
afterLoanInsert = conn.setSavepoint("AFTER_LOAN_INSERT");
```

If the `Members` update in Step 4 fails (e.g., the member ID doesn't exist), the code can roll back only to the savepoint, preserving Steps 1–2 in memory while undoing Step 4. In the current implementation, a full rollback follows — the savepoint exists as a recovery hook to allow future business logic to attempt Step 4 differently (e.g., inserting a default member record) rather than scrapping the whole transaction.

The `demonstrateSavepoint()` method explicitly shows this flow:
1. Insert a dummy loan → **Step A**
2. Set savepoint
3. Attempt a bad member update (0 rows affected) → detected → `rollback(savepoint)` → Step A still in memory
4. Full `rollback()` → Step A undone
5. Result: database unchanged — ACID integrity preserved.

### 1.3 Constraint Violations and Consistency Post-Rollback

Derby enforces constraints at the storage engine level. The `demonstrateConstraintViolation()` method attempts to insert two books with the same `ISBN` (which has a `UNIQUE` constraint). Derby throws an `SQLException` with a descriptive message on the second insert. The catch block calls `conn.rollback()`, which undoes both inserts — even the first valid one. This is the correct behavior: the entire transaction unit is invalid, so partial results must not persist.

This demonstrates that **constraint enforcement + transactional rollback together provide full data integrity**, even when errors occur mid-transaction.

### 1.4 ACID Properties Demonstrated

| Property | How the system demonstrates it |
|---|---|
| **Atomicity** | `processLoan()` commits all three DML steps or rolls back all three |
| **Consistency** | `CHECK`, `UNIQUE`, and `FOREIGN KEY` constraints prevent invalid states |
| **Isolation** | Derby uses page-level locking; concurrent transactions cannot read uncommitted data |
| **Durability** | Derby's WAL flushes committed transactions to disk before returning success |

---

## Part 2: Performance Findings

### 2.1 Insert Strategy: Individual vs Batch

| Strategy | 1,000 records | 10,000 records |
|---|---|---|
| Individual `executeUpdate()` | ~140 ms | ~492 ms |
| `addBatch()` + `executeBatch()` | ~88 ms | ~280 ms |
| Speedup | **1.6x** | **1.75x** |

**Why batch is faster:** Each individual `executeUpdate()` call requires a round-trip to the Derby engine, a parse, a plan lookup, and an execute. With `executeBatch()`, the JVM batches multiple parameter sets and sends them in a single engine call, reducing scheduling overhead. The single commit at the end further eliminates repeated WAL flush cycles.

### 2.2 Query Strategy: Full-Table Scan vs Indexed Lookup

| Strategy | Avg Time | Speedup |
|---|---|---|
| Full-table scan | ~5.5 ms | baseline |
| Indexed lookup (MemberID) | ~2.25 ms | **~2.5x** |

At 5,000 rows, Derby's B-tree index on `BenchMemberID` allows the engine to skip directly to matching rows without reading the entire page chain. The advantage grows significantly with table size — at millions of rows, the index difference becomes orders of magnitude.

### 2.3 Statement vs PreparedStatement

| Type | 500 inserts | Throughput |
|---|---|---|
| `Statement` (string concat) | ~1,013 ms | ~493 ops/sec |
| `PreparedStatement` | ~33 ms | ~15,113 ops/sec |
| Speedup | **~30x faster** | |

This is the most dramatic result. Every `Statement.executeUpdate()` with string concatenation forces Derby to: (1) tokenize the SQL, (2) parse it, (3) build a query plan, (4) execute. With `PreparedStatement`, steps 1–3 happen **once** at `conn.prepareStatement()`. All 500 executions reuse the compiled plan, paying only the execution cost. The result is a 30x improvement — critical in any loop-heavy operation.

An additional concern with `Statement` is **SQL injection vulnerability**: user data concatenated directly into SQL can be exploited. `PreparedStatement` parameterization eliminates this attack surface entirely.

### 2.4 Transaction Granularity: Per-op Commit vs Batched Commit

| Strategy | 100 operations | Throughput |
|---|---|---|
| Commit after each operation | ~99 ms | ~1,008 ops/sec |
| Single commit for all 100 | ~4 ms | ~24,189 ops/sec |
| Speedup | **~24x faster** | |

Each `commit()` forces Derby to flush its Write-Ahead Log (WAL) to disk, a synchronous I/O operation. With per-operation commits, 100 disk flushes occur. With a single batched commit, only one flush occurs. The trade-off is **durability granularity**: a failure mid-batch loses all uncommitted operations, whereas per-op commits lose at most one operation. The right choice depends on business requirements: financial systems favor per-op commits; bulk imports favor batched commits.

### 2.5 Trade-offs: Safety vs Speed

| Factor | Safer Choice | Faster Choice |
|---|---|---|
| Transaction scope | Narrow (per-record commit) | Wide (batch commit) |
| Statement type | PreparedStatement | PreparedStatement (no trade-off) |
| Insert method | Individual (for auditing) | Batch executeBatch() |
| Error recovery | Savepoints + rollback | No transactions (risky) |

For a library loan system, correctness is paramount — a book cannot appear available when it is on loan. Therefore the recommended configuration is: **PreparedStatement + batch inserts + wide transaction scope (one commit per logical business operation)**. This provides both safety and good throughput.

---

## Conclusion

This project demonstrates that JDBC transaction management and performance optimization are not in conflict. Using `PreparedStatement` is simultaneously safer (no SQL injection) and ~30x faster. Using explicit `commit()`/`rollback()` with savepoints is simultaneously safer (ACID) and requires discipline to use efficiently. The benchmarks confirm that Derby's indexing, WAL, and query plan caching all behave as expected, with results consistent with published database performance literature.

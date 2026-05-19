# Library Loan Management System
**End-to-End JDBC Application with Transaction Management & Performance Evaluation**  
**Database Engine:** Apache Derby (Embedded Mode)  
**Language:** Java 11+

---

## Project Structure

```
LibraryLoanSystem/
├── src/main/java/com/library/
│   ├── model/              # Data model classes
│   │   ├── Member.java
│   │   ├── Book.java
│   │   └── Loan.java
│   ├── connection/         # Derby DB lifecycle
│   │   └── ConnectionManager.java
│   ├── transaction/        # ACID transaction boundaries
│   │   └── TransactionService.java
│   ├── business/           # CRUD / domain logic
│   │   └── BusinessLogic.java
│   ├── benchmark/          # Performance evaluator
│   │   └── PerformanceEvaluator.java
│   └── ui/                 # CLI interface
│       └── MainApp.java
├── out/                    # Compiled .class files
├── docs/
│   └── analysis.md         # Transaction & performance analysis
├── performance_report.csv  # Generated after benchmarks
└── LibraryDB/              # Derby database directory (auto-created)
```

---

## Dependencies

| Dependency | Version | Source |
|---|---|---|
| Java JDK | 11+ | `apt install default-jdk` |
| Apache Derby | 10.14.2+ | `apt install libderby-java` |

Derby JAR location (Ubuntu/Debian): `/usr/share/java/derby.jar`

---

## Build Instructions

### 1. Install dependencies
```bash
sudo apt install default-jdk libderby-java
```

### 2. Compile
```bash
cd LibraryLoanSystem
find src -name "*.java" > sources.txt
mkdir -p out
javac -cp /usr/share/java/derby.jar -d out @sources.txt
```

### 3. Run
```bash
java -cp "out:/usr/share/java/derby.jar" com.library.ui.MainApp
```

---

## Sample CLI Session

```
LIBRARY LOAN MANAGEMENT SYSTEM
JDBC + Apache Derby - Mini Project

[ConnectionManager] Creating schema...
[ConnectionManager] Tables created: Members, Books, Loans.
[ConnectionManager] Indexes created.
[ConnectionManager] Seed data inserted (5 members, 10 books).

============ MAIN MENU ============
  1. Member Management
  2. Book Catalog
  3. Loan Processing (Borrow / Return)
  4. Query Reports
  5. Transaction Demonstrations (ACID)
  6. Run Performance Benchmarks
  7. Verify Indexes (DatabaseMetaData)
  8. Exit
Enter choice: 3

-- Loan Processing --
  1. Borrow a book
  2. Return a book
  3. View all loans
Choice: 1
Book ID: 1
Member ID: 2
[TransactionService] Savepoint 'AFTER_LOAN_INSERT' set (LoanID=1).
[TransactionService] Loan committed. LoanID=1
OK Loan processed. Loan ID=1  Due date: 2026-05-26
```

---

## Key Design Decisions

- **Auto-commit disabled** for all data-modifying operations; only re-enabled in `finally` blocks.
- **PreparedStatement** used throughout — prevents SQL injection and allows query plan reuse.
- **try-with-resources** used for all Connection, Statement, and ResultSet objects.
- **Singleton ConnectionManager** — one physical connection shared across all services.
- **Savepoints** named descriptively (`AFTER_LOAN_INSERT`, `DEMO_SAVEPOINT`) for clarity.
- **Indexes** created on `Books.ISBN`, `Loans.MemberID`, `Loans.ReturnDate`, `Loans.Status`, `Loans.DueDate`.

---

## Derby-Specific Notes

- Embedded mode URL: `jdbc:derby:LibraryDB;create=true`
- Shutdown URL: `jdbc:derby:LibraryDB;shutdown=true` — always throws `SQLState=08006` on success (expected).
- Derby caches pages in memory — the benchmark includes a warm-up phase to stabilize JIT and buffer cache.
- `DatabaseMetaData` used to verify indexes and check table existence before creation.

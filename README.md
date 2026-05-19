# Library Loan Management System
### Student: Subham Kumar Sahoo | Roll No: 2341019060 | Serial No: 35 | Section:23412C3

---

## What is this Project?
A complete console-based **Library Loan Management System** built using **Java JDBC** and **Apache Derby** embedded database. It simulates a real library counter where members can borrow and return books, with full database transaction support.

---

## Project Structure
Subham_Kumar_Sahoo_2341019060_35/
├── LibraryLoanSystem/
│   ├── src/main/java/com/library/
│   │   ├── connection/
│   │   │   └── ConnectionManager.java   → Derby DB setup, connection, indexes
│   │   ├── transaction/
│   │   │   └── TransactionService.java  → Commit, Rollback, Savepoints
│   │   ├── business/
│   │   │   └── BusinessLogic.java       → CRUD operations (Members, Books, Loans)
│   │   ├── benchmark/
│   │   │   └── PerformanceEvaluator.java → JDBC performance benchmarks
│   │   ├── model/
│   │   │   ├── Member.java
│   │   │   ├── Book.java
│   │   │   └── Loan.java
│   │   └── ui/
│   │       └── MainApp.java             → Main menu CLI interface
│   └── docs/
│       └── analysis.md                  → Transaction & performance analysis
├── performance_report.csv               → Auto-generated benchmark results
├── flow_diagram.txt                     → System flow diagram
└── start.bat                            → Windows run script

---

## Database Schema
Three normalized tables created automatically on first run:

| Table | Columns | Key Constraints |
|-------|---------|-----------------|
| **Members** | MemberID, Name, Email, Phone, JoinDate, ActiveLoans | PRIMARY KEY, UNIQUE(Email), CHECK(ActiveLoans>=0) |
| **Books** | BookID, Title, Author, ISBN, Genre, Available, TotalCopies, AvailableCopies | PRIMARY KEY, UNIQUE(ISBN) |
| **Loans** | LoanID, MemberID, BookID, LoanDate, DueDate, ReturnDate, Status | PRIMARY KEY, FOREIGN KEY(MemberID, BookID) |

### Indexes Created:
- `idx_books_isbn` → Books(ISBN)
- `idx_loans_member` → Loans(MemberID)
- `idx_loans_due` → Loans(DueDate)
- `idx_loans_status` → Loans(Status)
- `idx_loans_return` → Loans(ReturnDate)

---

## Features

### 1. Member Management
- Register new members with name, email, phone
- View all members and their active loan count

### 2. Book Catalog
- Add books with title, author, ISBN, genre, copies
- Search books by ISBN (uses index for fast lookup)
- View available books only

### 3. Loan Processing (ACID Transactions)
- **Borrow a book** → 4-step transaction:
  1. Verify book availability
  2. Insert loan record → **Savepoint set here**
  3. Decrease book available copies
  4. Increase member active loan count
  - If any step fails → complete **rollback**
- **Return a book** → Updates loan status, restores book copy, decrements member loans

### 4. Query Reports
- Active loans by member ID
- All overdue loans
- Mark overdue loans automatically

### 5. ACID Transaction Demonstrations
- **Savepoint Demo** → Shows partial rollback behavior
- **Constraint Violation Demo** → Shows duplicate ISBN rejection and full rollback

### 6. Performance Benchmarks
| Test | Comparison | Result |
|------|-----------|--------|
| Insert Strategy | Individual vs Batch INSERT (1K & 10K records) | Batch ~1.75x faster |
| Query Strategy | Full table scan vs Indexed lookup | Index ~2.5x faster |
| Statement Type | Plain Statement vs PreparedStatement | PreparedStatement ~30x faster |
| Transaction | Per-operation commit vs Batched commit | Batched ~24x faster |

Results saved automatically to `performance_report.csv`

---

## How to Run

### Requirements
- Java JDK 11 or above
- Apache Derby 10.16.1.1 (`derby.jar`)

### Setup Steps

**1. Clone the repository**
```bash
git clone https://github.com/subhamsahoo77037-droid/Subham_Kumar_Sahoo_2341019060_35.git
```

**2. Download Apache Derby**
- Go to: https://db.apache.org/derby/derby_downloads.html
- Download `db-derby-10.16.1.1-bin.zip`
- Extract and copy `derby.jar` into the `lib/` folder

**3. Compile**
```bash
javac -cp "lib\derby.jar" -d out src\main\java\com\library\model\*.java src\main\java\com\library\connection\*.java src\main\java\com\library\transaction\*.java src\main\java\com\library\business\*.java src\main\java\com\library\benchmark\*.java src\main\java\com\library\ui\*.java
```

**4. Run**
```bash
java -cp "out;lib\derby.jar" com.library.ui.MainApp
```

---

## Application Menu

============ MAIN MENU ============

Member Management
Book Catalog
Loan Processing (Borrow / Return)
Query Reports
Transaction Demonstrations (ACID)
Run Performance Benchmarks
Verify Indexes (DatabaseMetaData)
Exit
===================================

---

## Key Concepts Demonstrated

| Concept | Implementation |
|---------|---------------|
| **ACID Transactions** | processLoan() and processReturn() use explicit commit/rollback |
| **Savepoints** | Named savepoint after loan insert for partial rollback |
| **PreparedStatement** | Used in all queries — prevents SQL injection, faster execution |
| **Batch Processing** | executeBatch() for bulk inserts in benchmarks |
| **Indexes** | 5 indexes created and verified via DatabaseMetaData |
| **try-with-resources** | All Connection, Statement, ResultSet properly closed |
| **Embedded Derby** | Auto-creates DB on first run, clean shutdown via SQLState 08006 |

---

## Technologies Used
- **Language:** Java (JDK 11+)
- **Database:** Apache Derby 10.16.1.1 (Embedded Mode)
- **API:** JDBC (Java Database Connectivity)
- **IDE:** Visual Studio Code
- **Version Control:** Git & GitHub

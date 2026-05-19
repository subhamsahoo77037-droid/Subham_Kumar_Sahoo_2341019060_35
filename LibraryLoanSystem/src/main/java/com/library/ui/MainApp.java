package com.library.ui;

import com.library.benchmark.PerformanceEvaluator;
import com.library.business.BusinessLogic;
import com.library.connection.ConnectionManager;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Member;
import com.library.transaction.TransactionService;

import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;

/**
 * MainApp — CLI interface and workflow orchestrator.
 * All user interaction passes through this class.
 */
public class MainApp {

    private static ConnectionManager  connManager;
    private static BusinessLogic      business;
    private static TransactionService txService;
    private static PerformanceEvaluator evaluator;
    private static Scanner scanner;

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║     LIBRARY LOAN MANAGEMENT SYSTEM         ║");
        System.out.println("║     JDBC + Apache Derby — Mini Project      ║");
        System.out.println("╚════════════════════════════════════════════╝\n");

        scanner     = new Scanner(System.in);
        connManager = ConnectionManager.getInstance();

        try {
            // ── Initialize DB ──────────────────────────────────────
            connManager.initializeDatabase();

            business  = new BusinessLogic(connManager);
            txService = new TransactionService(connManager);
            evaluator = new PerformanceEvaluator(connManager);
            evaluator.ensureBenchmarkTables();

            // ── Main menu loop ─────────────────────────────────────
            boolean running = true;
            while (running) {
                printMainMenu();
                int choice = readInt("Enter choice: ");

                switch (choice) {
                    case 1  -> memberMenu();
                    case 2  -> bookMenu();
                    case 3  -> loanMenu();
                    case 4  -> queryMenu();
                    case 5  -> transactionDemoMenu();
                    case 6  -> runBenchmarks();
                    case 7  -> connManager.verifyIndexes();
                    case 8  -> running = false;
                    default -> System.out.println("Invalid choice. Try again.");
                }
            }

        } catch (SQLException e) {
            System.err.println("\n[FATAL] Database error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("\nShutting down...");
            if (connManager != null) connManager.shutdown();
            System.out.println("Goodbye!");
        }
    }

    // ════════════════════════════════════════════════
    // MENUS
    // ════════════════════════════════════════════════

    private static void printMainMenu() {
        System.out.println("\n══════════════ MAIN MENU ══════════════");
        System.out.println("  1. Member Management");
        System.out.println("  2. Book Catalog");
        System.out.println("  3. Loan Processing (Borrow / Return)");
        System.out.println("  4. Query Reports");
        System.out.println("  5. Transaction Demonstrations (ACID)");
        System.out.println("  6. Run Performance Benchmarks");
        System.out.println("  7. Verify Indexes (DatabaseMetaData)");
        System.out.println("  8. Exit");
        System.out.println("═══════════════════════════════════════");
    }

    // ─── Member Menu ───────────────────────────────────────

    private static void memberMenu() throws SQLException {
        System.out.println("\n── Member Management ──");
        System.out.println("  1. Register new member");
        System.out.println("  2. List all members");
        System.out.println("  3. View member by ID");
        int ch = readInt("Choice: ");

        switch (ch) {
            case 1 -> {
                String name  = readString("Name: ");
                String email = readString("Email: ");
                String phone = readString("Phone: ");
                try {
                    int id = business.registerMember(name, email, phone);
                    System.out.println("✔ Member registered with ID=" + id);
                } catch (SQLException e) {
                    System.out.println("✘ Failed: " + e.getMessage());
                }
            }
            case 2 -> {
                List<Member> members = business.getAllMembers();
                printLine();
                System.out.printf("%-5s %-25s %-30s %-15s %5s%n",
                        "ID", "Name", "Email", "Phone", "Loans");
                printLine();
                for (Member m : members) {
                    System.out.printf("%-5d %-25s %-30s %-15s %5d%n",
                            m.getMemberId(), m.getName(), m.getEmail(),
                            m.getPhone(), m.getActiveLoans());
                }
                printLine();
                System.out.println("Total members: " + members.size());
            }
            case 3 -> {
                int id = readInt("Member ID: ");
                Member m = business.getMemberById(id);
                if (m != null) System.out.println(m);
                else System.out.println("Member not found.");
            }
        }
    }

    // ─── Book Menu ─────────────────────────────────────────

    private static void bookMenu() throws SQLException {
        System.out.println("\n── Book Catalog ──");
        System.out.println("  1. Add new book");
        System.out.println("  2. List all books");
        System.out.println("  3. List available books");
        System.out.println("  4. Search by ISBN");
        int ch = readInt("Choice: ");

        switch (ch) {
            case 1 -> {
                String title  = readString("Title: ");
                String author = readString("Author: ");
                String isbn   = readString("ISBN: ");
                String genre  = readString("Genre: ");
                int copies    = readInt("Number of copies: ");
                try {
                    int id = business.addBook(title, author, isbn, genre, copies);
                    System.out.println("✔ Book added with ID=" + id);
                } catch (SQLException e) {
                    System.out.println("✘ Failed: " + e.getMessage());
                }
            }
            case 2 -> {
                printBookTable(business.getAllBooks());
            }
            case 3 -> {
                printBookTable(business.getAvailableBooks());
            }
            case 4 -> {
                String isbn = readString("ISBN: ");
                Book b = business.getBookByIsbn(isbn);
                if (b != null) System.out.println(b);
                else System.out.println("Book not found.");
            }
        }
    }

    // ─── Loan Menu ─────────────────────────────────────────

    private static void loanMenu() throws SQLException {
        System.out.println("\n── Loan Processing ──");
        System.out.println("  1. Borrow a book");
        System.out.println("  2. Return a book");
        System.out.println("  3. View all loans");
        int ch = readInt("Choice: ");

        switch (ch) {
            case 1 -> {
                int bookId   = readInt("Book ID: ");
                int memberId = readInt("Member ID: ");

                // Duplicate loan check (edge case validation)
                if (business.hasDuplicateLoan(memberId, bookId)) {
                    System.out.println("✘ This member already has an active loan for this book.");
                    return;
                }

                try {
                    int loanId = txService.processLoan(bookId, memberId);
                    if (loanId > 0) {
                        System.out.println("✔ Loan processed. Loan ID=" + loanId);
                        System.out.println("  Due date: " + java.time.LocalDate.now().plusDays(14));
                    } else {
                        System.out.println("✘ Loan could not be processed (book unavailable or invalid member).");
                    }
                } catch (SQLException e) {
                    System.out.println("✘ Transaction failed: " + e.getMessage());
                }
            }
            case 2 -> {
                int loanId = readInt("Loan ID to return: ");
                try {
                    boolean ok = txService.processReturn(loanId);
                    System.out.println(ok ? "✔ Book returned successfully." : "✘ Return failed.");
                } catch (SQLException e) {
                    System.out.println("✘ Transaction failed: " + e.getMessage());
                }
            }
            case 3 -> {
                printLoanTable(business.getAllLoans());
            }
        }
    }

    // ─── Query Menu ────────────────────────────────────────

    private static void queryMenu() throws SQLException {
        System.out.println("\n── Query Reports ──");
        System.out.println("  1. Active loans by member ID");
        System.out.println("  2. Overdue loans");
        System.out.println("  3. Mark overdue loans");
        int ch = readInt("Choice: ");

        switch (ch) {
            case 1 -> {
                int memberId = readInt("Member ID: ");
                List<Loan> loans = business.getActiveLoansByMember(memberId);
                if (loans.isEmpty()) System.out.println("No active loans for this member.");
                else printLoanTable(loans);
            }
            case 2 -> {
                List<Loan> loans = business.getOverdueLoans();
                if (loans.isEmpty()) System.out.println("No overdue loans.");
                else printLoanTable(loans);
            }
            case 3 -> {
                int count = business.markOverdueLoans();
                System.out.println("✔ Marked " + count + " loan(s) as OVERDUE.");
            }
        }
    }

    // ─── Transaction Demo Menu ─────────────────────────────

    private static void transactionDemoMenu() throws SQLException {
        System.out.println("\n── ACID Transaction Demonstrations ──");
        System.out.println("  1. Savepoint demo (partial rollback)");
        System.out.println("  2. Constraint violation + full rollback");
        int ch = readInt("Choice: ");

        switch (ch) {
            case 1 -> txService.demonstrateSavepoint();
            case 2 -> txService.demonstrateConstraintViolation();
        }
    }

    // ─── Benchmarks ────────────────────────────────────────

    private static void runBenchmarks() throws SQLException {
        System.out.println("\nRunning all benchmarks — this may take 1–2 minutes...");
        evaluator.runAllBenchmarks();
    }

    // ════════════════════════════════════════════════
    // DISPLAY HELPERS
    // ════════════════════════════════════════════════

    private static void printBookTable(List<Book> books) {
        printLine();
        System.out.printf("%-5s %-40s %-25s %-22s %-12s %5s/%5s%n",
                "ID", "Title", "Author", "ISBN", "Genre", "Avail", "Total");
        printLine();
        for (Book b : books) {
            System.out.printf("%-5d %-40s %-25s %-22s %-12s %5d/%5d%n",
                    b.getBookId(),
                    truncate(b.getTitle(), 39),
                    truncate(b.getAuthor(), 24),
                    b.getIsbn(),
                    b.getGenre(),
                    b.getAvailableCopies(),
                    b.getTotalCopies());
        }
        printLine();
        System.out.println("Total: " + books.size() + " book(s).");
    }

    private static void printLoanTable(List<Loan> loans) {
        printLine();
        System.out.printf("%-6s %-20s %-30s %-12s %-12s %-9s%n",
                "LoanID", "Member", "Book", "LoanDate", "DueDate", "Status");
        printLine();
        for (Loan l : loans) {
            System.out.printf("%-6d %-20s %-30s %-12s %-12s %-9s%n",
                    l.getLoanId(),
                    truncate(l.getMemberName() != null ? l.getMemberName() : String.valueOf(l.getMemberId()), 19),
                    truncate(l.getBookTitle() != null ? l.getBookTitle() : String.valueOf(l.getBookId()), 29),
                    l.getLoanDate(),
                    l.getDueDate(),
                    l.getStatus());
        }
        printLine();
        System.out.println("Total: " + loans.size() + " loan(s).");
    }

    private static void printLine() {
        System.out.println("─".repeat(100));
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    // ════════════════════════════════════════════════
    // INPUT HELPERS
    // ════════════════════════════════════════════════

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static String readString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
}

package com.library.connection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * ConnectionManager — responsible for Derby DB initialization,
 * connection lifecycle, schema creation, seed data, and cleanup.
 */
public class ConnectionManager {

    // Embedded Derby URL — creates DB on first run
    private static final String DB_URL      = "jdbc:derby:LibraryDB;create=true";
    private static final String SHUTDOWN_URL = "jdbc:derby:LibraryDB;shutdown=true";
    private static final String DRIVER_CLASS = "org.apache.derby.jdbc.EmbeddedDriver";

    private static ConnectionManager instance;
    private Connection connection;

    private ConnectionManager() {}

    /** Singleton accessor */
    public static ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    // ─────────────────────────────────────────────
    // Get / Open Connection
    // ─────────────────────────────────────────────

    /**
     * Returns a live connection to the embedded Derby database.
     * Auto-commit is LEFT ON here; individual services disable it as needed.
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName(DRIVER_CLASS);
            } catch (ClassNotFoundException e) {
                throw new SQLException("Apache Derby driver not found on classpath.", e);
            }
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("[ConnectionManager] Connected to embedded Derby database.");
        }
        return connection;
    }

    // ─────────────────────────────────────────────
    // Schema Initialization
    // ─────────────────────────────────────────────

    /**
     * Creates tables, indexes, and seed data if they do not already exist.
     * Called once on application start-up.
     */
    public void initializeDatabase() throws SQLException {
        Connection conn = getConnection();

        if (!tablesExist(conn)) {
            System.out.println("[ConnectionManager] Creating schema...");
            createTables(conn);
            createIndexes(conn);
            insertSeedData(conn);
            System.out.println("[ConnectionManager] Schema and seed data ready.");
        } else {
            System.out.println("[ConnectionManager] Schema already exists. Skipping creation.");
        }
    }

    /** Checks whether the Members table exists (proxy for full schema check). */
    private boolean tablesExist(Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, "APP", "MEMBERS", new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    // ─────────────────────────────────────────────
    // DDL — Tables
    // ─────────────────────────────────────────────

    private void createTables(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            // Members table
            stmt.executeUpdate(
                "CREATE TABLE Members (" +
                "  MemberID    INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  Name        VARCHAR(100) NOT NULL," +
                "  Email       VARCHAR(150) NOT NULL UNIQUE," +
                "  Phone       VARCHAR(20)," +
                "  JoinDate    DATE NOT NULL," +
                "  ActiveLoans INTEGER DEFAULT 0 CHECK (ActiveLoans >= 0)" +
                ")"
            );

            // Books table
            stmt.executeUpdate(
                "CREATE TABLE Books (" +
                "  BookID          INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  Title           VARCHAR(200) NOT NULL," +
                "  Author          VARCHAR(100) NOT NULL," +
                "  ISBN            VARCHAR(20) NOT NULL UNIQUE," +
                "  Genre           VARCHAR(50)," +
                "  Available       SMALLINT DEFAULT 1 CHECK (Available IN (0,1))," +
                "  TotalCopies     INTEGER DEFAULT 1 CHECK (TotalCopies > 0)," +
                "  AvailableCopies INTEGER DEFAULT 1 CHECK (AvailableCopies >= 0)" +
                ")"
            );

            // Loans table
            stmt.executeUpdate(
                "CREATE TABLE Loans (" +
                "  LoanID     INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  MemberID   INTEGER NOT NULL REFERENCES Members(MemberID)," +
                "  BookID     INTEGER NOT NULL REFERENCES Books(BookID)," +
                "  LoanDate   DATE NOT NULL," +
                "  DueDate    DATE NOT NULL," +
                "  ReturnDate DATE," +
                "  Status     VARCHAR(10) DEFAULT 'ACTIVE' " +
                "             CHECK (Status IN ('ACTIVE','RETURNED','OVERDUE'))" +
                ")"
            );

            System.out.println("[ConnectionManager] Tables created: Members, Books, Loans.");
        }
    }

    // ─────────────────────────────────────────────
    // DDL — Indexes
    // ─────────────────────────────────────────────

    private void createIndexes(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE INDEX idx_books_isbn    ON Books(ISBN)");
            stmt.executeUpdate("CREATE INDEX idx_loans_member  ON Loans(MemberID)");
            stmt.executeUpdate("CREATE INDEX idx_loans_return  ON Loans(ReturnDate)");
            stmt.executeUpdate("CREATE INDEX idx_loans_status  ON Loans(Status)");
            stmt.executeUpdate("CREATE INDEX idx_loans_due     ON Loans(DueDate)");
            System.out.println("[ConnectionManager] Indexes created.");
        }
    }

    // ─────────────────────────────────────────────
    // DML — Seed Data
    // ─────────────────────────────────────────────

    private void insertSeedData(Connection conn) throws SQLException {
        conn.setAutoCommit(false);
        try (Statement stmt = conn.createStatement()) {

            // Members
            stmt.executeUpdate("INSERT INTO Members(Name,Email,Phone,JoinDate,ActiveLoans) VALUES('Alice Johnson','alice@example.com','9876543210','2023-01-15',0)");
            stmt.executeUpdate("INSERT INTO Members(Name,Email,Phone,JoinDate,ActiveLoans) VALUES('Bob Smith','bob@example.com','9123456780','2023-03-22',0)");
            stmt.executeUpdate("INSERT INTO Members(Name,Email,Phone,JoinDate,ActiveLoans) VALUES('Carol White','carol@example.com','9988776655','2023-06-10',0)");
            stmt.executeUpdate("INSERT INTO Members(Name,Email,Phone,JoinDate,ActiveLoans) VALUES('David Brown','david@example.com','8877665544','2024-01-05',0)");
            stmt.executeUpdate("INSERT INTO Members(Name,Email,Phone,JoinDate,ActiveLoans) VALUES('Eva Green','eva@example.com','7766554433','2024-04-18',0)");

            // Books
            stmt.executeUpdate("INSERT INTO Books(Title,Author,ISBN,Genre,Available,TotalCopies,AvailableCopies) VALUES('Clean Code','Robert C. Martin','978-0-13-235088-4','Technology',1,3,3)");
            stmt.executeUpdate("INSERT INTO Books(Title,Author,ISBN,Genre,Available,TotalCopies,AvailableCopies) VALUES('The Pragmatic Programmer','David Thomas','978-0-13-595705-9','Technology',1,2,2)");
            stmt.executeUpdate("INSERT INTO Books(Title,Author,ISBN,Genre,Available,TotalCopies,AvailableCopies) VALUES('Design Patterns','Gang of Four','978-0-20-163361-5','Technology',1,2,2)");
            stmt.executeUpdate("INSERT INTO Books(Title,Author,ISBN,Genre,Available,TotalCopies,AvailableCopies) VALUES('1984','George Orwell','978-0-45-228423-4','Fiction',1,4,4)");
            stmt.executeUpdate("INSERT INTO Books(Title,Author,ISBN,Genre,Available,TotalCopies,AvailableCopies) VALUES('To Kill a Mockingbird','Harper Lee','978-0-44-631078-1','Fiction',1,3,3)");
            stmt.executeUpdate("INSERT INTO Books(Title,Author,ISBN,Genre,Available,TotalCopies,AvailableCopies) VALUES('Sapiens','Yuval Noah Harari','978-0-06-231609-7','History',1,2,2)");
            stmt.executeUpdate("INSERT INTO Books(Title,Author,ISBN,Genre,Available,TotalCopies,AvailableCopies) VALUES('Effective Java','Joshua Bloch','978-0-13-468599-1','Technology',1,2,2)");
            stmt.executeUpdate("INSERT INTO Books(Title,Author,ISBN,Genre,Available,TotalCopies,AvailableCopies) VALUES('The Great Gatsby','F. Scott Fitzgerald','978-0-74-326851-6','Fiction',1,3,3)");
            stmt.executeUpdate("INSERT INTO Books(Title,Author,ISBN,Genre,Available,TotalCopies,AvailableCopies) VALUES('Thinking Fast and Slow','Daniel Kahneman','978-0-37-453355-7','Psychology',1,2,2)");
            stmt.executeUpdate("INSERT INTO Books(Title,Author,ISBN,Genre,Available,TotalCopies,AvailableCopies) VALUES('Introduction to Algorithms','Cormen et al.','978-0-26-204630-5','Technology',1,2,2)");

            conn.commit();
            System.out.println("[ConnectionManager] Seed data inserted (5 members, 10 books).");
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ─────────────────────────────────────────────
    // Graceful Shutdown
    // ─────────────────────────────────────────────

    /**
     * Closes the active connection and shuts down the embedded Derby engine,
     * releasing all file locks on the database directory.
     */
    public void shutdown() {
        // Close connection first
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("[ConnectionManager] Error closing connection: " + e.getMessage());
            }
        }

        // Derby shutdown — always throws SQLState "08006" on success
        try {
            DriverManager.getConnection(SHUTDOWN_URL);
        } catch (SQLException e) {
            if ("08006".equals(e.getSQLState())) {
                System.out.println("[ConnectionManager] Derby shutdown cleanly.");
            } else {
                System.err.println("[ConnectionManager] Unexpected shutdown error: " + e.getMessage());
            }
        }
    }

    /**
     * Verifies indexes exist via DatabaseMetaData — used for validation.
     */
    public void verifyIndexes() throws SQLException {
        Connection conn = getConnection();
        DatabaseMetaData meta = conn.getMetaData();
        System.out.println("\n[ConnectionManager] Index verification:");
        for (String table : new String[]{"BOOKS", "LOANS"}) {
            try (ResultSet rs = meta.getIndexInfo(null, "APP", table, false, false)) {
                while (rs.next()) {
                    String idxName = rs.getString("INDEX_NAME");
                    String col     = rs.getString("COLUMN_NAME");
                    if (idxName != null && !idxName.startsWith("SQL")) {
                        System.out.printf("  Table=%-8s  Index=%-25s  Column=%s%n", table, idxName, col);
                    }
                }
            }
        }
    }
}

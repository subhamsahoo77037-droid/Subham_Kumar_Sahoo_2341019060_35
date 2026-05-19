package com.library.business;

import com.library.connection.ConnectionManager;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Member;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * BusinessLogic — handles all CRUD operations using PreparedStatements
 * with try-with-resources for proper resource cleanup.
 */
public class BusinessLogic {

    private final ConnectionManager connManager;

    public BusinessLogic(ConnectionManager connManager) {
        this.connManager = connManager;
    }

    // ═══════════════════════════════════════════════════════
    // MEMBER OPERATIONS
    // ═══════════════════════════════════════════════════════

    /** Registers a new library member. Returns generated MemberID. */
    public int registerMember(String name, String email, String phone) throws SQLException {
        String sql = "INSERT INTO Members(Name, Email, Phone, JoinDate, ActiveLoans) VALUES(?,?,?,?,0)";
        Connection conn = connManager.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql, new String[]{"MEMBERID"})) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);
            ps.setDate(4, Date.valueOf(LocalDate.now()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    System.out.println("[BusinessLogic] Member registered: " + name + " (ID=" + id + ")");
                    return id;
                }
            }
        }
        return -1;
    }

    /** Fetches all members. */
    public List<Member> getAllMembers() throws SQLException {
        List<Member> list = new ArrayList<>();
        String sql = "SELECT MemberID, Name, Email, Phone, JoinDate, ActiveLoans FROM Members ORDER BY MemberID";

        try (PreparedStatement ps = connManager.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapMember(rs));
            }
        }
        return list;
    }

    /** Fetches a member by ID. */
    public Member getMemberById(int memberId) throws SQLException {
        String sql = "SELECT MemberID, Name, Email, Phone, JoinDate, ActiveLoans FROM Members WHERE MemberID = ?";
        try (PreparedStatement ps = connManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapMember(rs);
            }
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════
    // BOOK OPERATIONS
    // ═══════════════════════════════════════════════════════

    /** Adds a new book to the catalog. Returns generated BookID. */
    public int addBook(String title, String author, String isbn, String genre, int copies) throws SQLException {
        String sql = "INSERT INTO Books(Title,Author,ISBN,Genre,Available,TotalCopies,AvailableCopies) VALUES(?,?,?,?,1,?,?)";
        Connection conn = connManager.getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql, new String[]{"BOOKID"})) {
            ps.setString(1, title);
            ps.setString(2, author);
            ps.setString(3, isbn);
            ps.setString(4, genre);
            ps.setInt(5, copies);
            ps.setInt(6, copies);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    System.out.println("[BusinessLogic] Book added: '" + title + "' (ID=" + id + ")");
                    return id;
                }
            }
        }
        return -1;
    }

    /** Fetches all books. */
    public List<Book> getAllBooks() throws SQLException {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT BookID,Title,Author,ISBN,Genre,Available,TotalCopies,AvailableCopies FROM Books ORDER BY BookID";

        try (PreparedStatement ps = connManager.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapBook(rs));
        }
        return list;
    }

    /** Fetches only available books. */
    public List<Book> getAvailableBooks() throws SQLException {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT BookID,Title,Author,ISBN,Genre,Available,TotalCopies,AvailableCopies " +
                     "FROM Books WHERE AvailableCopies > 0 ORDER BY Title";

        try (PreparedStatement ps = connManager.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapBook(rs));
        }
        return list;
    }

    /** Looks up a book by ISBN (uses the idx_books_isbn index). */
    public Book getBookByIsbn(String isbn) throws SQLException {
        String sql = "SELECT BookID,Title,Author,ISBN,Genre,Available,TotalCopies,AvailableCopies " +
                     "FROM Books WHERE ISBN = ?";
        try (PreparedStatement ps = connManager.getConnection().prepareStatement(sql)) {
            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapBook(rs);
            }
        }
        return null;
    }

    /** Fetches a book by ID. */
    public Book getBookById(int bookId) throws SQLException {
        String sql = "SELECT BookID,Title,Author,ISBN,Genre,Available,TotalCopies,AvailableCopies " +
                     "FROM Books WHERE BookID = ?";
        try (PreparedStatement ps = connManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapBook(rs);
            }
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════
    // LOAN QUERY OPERATIONS
    // ═══════════════════════════════════════════════════════

    /** Returns all active loans for a specific member (uses idx_loans_member). */
    public List<Loan> getActiveLoansByMember(int memberId) throws SQLException {
        List<Loan> list = new ArrayList<>();
        String sql =
            "SELECT l.LoanID, l.MemberID, l.BookID, l.LoanDate, l.DueDate, l.ReturnDate, l.Status, " +
            "       m.Name AS MemberName, b.Title AS BookTitle " +
            "FROM Loans l " +
            "JOIN Members m ON l.MemberID = m.MemberID " +
            "JOIN Books   b ON l.BookID   = b.BookID " +
            "WHERE l.MemberID = ? AND l.Status = 'ACTIVE' " +
            "ORDER BY l.DueDate";

        try (PreparedStatement ps = connManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapLoan(rs));
            }
        }
        return list;
    }

    /** Returns all overdue loans (uses idx_loans_status + idx_loans_due). */
    public List<Loan> getOverdueLoans() throws SQLException {
        List<Loan> list = new ArrayList<>();
        String sql =
            "SELECT l.LoanID, l.MemberID, l.BookID, l.LoanDate, l.DueDate, l.ReturnDate, l.Status, " +
            "       m.Name AS MemberName, b.Title AS BookTitle " +
            "FROM Loans l " +
            "JOIN Members m ON l.MemberID = m.MemberID " +
            "JOIN Books   b ON l.BookID   = b.BookID " +
            "WHERE l.Status = 'ACTIVE' AND l.DueDate < CURRENT_DATE " +
            "ORDER BY l.DueDate";

        try (PreparedStatement ps = connManager.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapLoan(rs));
        }
        return list;
    }

    /** Returns all loans (all statuses) with member and book details. */
    public List<Loan> getAllLoans() throws SQLException {
        List<Loan> list = new ArrayList<>();
        String sql =
            "SELECT l.LoanID, l.MemberID, l.BookID, l.LoanDate, l.DueDate, l.ReturnDate, l.Status, " +
            "       m.Name AS MemberName, b.Title AS BookTitle " +
            "FROM Loans l " +
            "JOIN Members m ON l.MemberID = m.MemberID " +
            "JOIN Books   b ON l.BookID   = b.BookID " +
            "ORDER BY l.LoanID DESC";

        try (PreparedStatement ps = connManager.getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapLoan(rs));
        }
        return list;
    }

    /** Marks overdue loans in the DB (DueDate < today AND still ACTIVE). */
    public int markOverdueLoans() throws SQLException {
        String sql = "UPDATE Loans SET Status = 'OVERDUE' WHERE DueDate < CURRENT_DATE AND Status = 'ACTIVE'";
        try (PreparedStatement ps = connManager.getConnection().prepareStatement(sql)) {
            int rows = ps.executeUpdate();
            System.out.println("[BusinessLogic] Marked " + rows + " loan(s) as OVERDUE.");
            return rows;
        }
    }

    // ═══════════════════════════════════════════════════════
    // EDGE-CASE VALIDATION HELPERS
    // ═══════════════════════════════════════════════════════

    /** Checks if a member has already borrowed the same book (active loan exists). */
    public boolean hasDuplicateLoan(int memberId, int bookId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Loans WHERE MemberID=? AND BookID=? AND Status='ACTIVE'";
        try (PreparedStatement ps = connManager.getConnection().prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.setInt(2, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ═══════════════════════════════════════════════════════
    // ROW MAPPERS
    // ═══════════════════════════════════════════════════════

    private Member mapMember(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setMemberId(rs.getInt("MemberID"));
        m.setName(rs.getString("Name"));
        m.setEmail(rs.getString("Email"));
        m.setPhone(rs.getString("Phone"));
        Date d = rs.getDate("JoinDate");
        if (d != null) m.setJoinDate(d.toLocalDate());
        m.setActiveLoans(rs.getInt("ActiveLoans"));
        return m;
    }

    private Book mapBook(ResultSet rs) throws SQLException {
        Book b = new Book();
        b.setBookId(rs.getInt("BookID"));
        b.setTitle(rs.getString("Title"));
        b.setAuthor(rs.getString("Author"));
        b.setIsbn(rs.getString("ISBN"));
        b.setGenre(rs.getString("Genre"));
        b.setAvailable(rs.getInt("Available") == 1);
        b.setTotalCopies(rs.getInt("TotalCopies"));
        b.setAvailableCopies(rs.getInt("AvailableCopies"));
        return b;
    }

    private Loan mapLoan(ResultSet rs) throws SQLException {
        Loan l = new Loan();
        l.setLoanId(rs.getInt("LoanID"));
        l.setMemberId(rs.getInt("MemberID"));
        l.setBookId(rs.getInt("BookID"));
        Date ld = rs.getDate("LoanDate");  if (ld != null) l.setLoanDate(ld.toLocalDate());
        Date dd = rs.getDate("DueDate");   if (dd != null) l.setDueDate(dd.toLocalDate());
        Date rd = rs.getDate("ReturnDate");if (rd != null) l.setReturnDate(rd.toLocalDate());
        l.setStatus(rs.getString("Status"));
        // Optional joined columns
        try { l.setMemberName(rs.getString("MemberName")); } catch (SQLException ignored) {}
        try { l.setBookTitle(rs.getString("BookTitle"));  } catch (SQLException ignored) {}
        return l;
    }
}

package com.library.model;

import java.time.LocalDate;

public class Loan {
    private int loanId;
    private int memberId;
    private int bookId;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private String status;      // ACTIVE, RETURNED, OVERDUE

    // Joined fields for display
    private String memberName;
    private String bookTitle;

    public Loan() {}

    public Loan(int loanId, int memberId, int bookId, LocalDate loanDate,
                LocalDate dueDate, LocalDate returnDate, String status) {
        this.loanId = loanId;
        this.memberId = memberId;
        this.bookId = bookId;
        this.loanDate = loanDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.status = status;
    }

    // Getters and Setters
    public int getLoanId()                   { return loanId; }
    public void setLoanId(int id)            { this.loanId = id; }
    public int getMemberId()                 { return memberId; }
    public void setMemberId(int id)          { this.memberId = id; }
    public int getBookId()                   { return bookId; }
    public void setBookId(int id)            { this.bookId = id; }
    public LocalDate getLoanDate()           { return loanDate; }
    public void setLoanDate(LocalDate d)     { this.loanDate = d; }
    public LocalDate getDueDate()            { return dueDate; }
    public void setDueDate(LocalDate d)      { this.dueDate = d; }
    public LocalDate getReturnDate()         { return returnDate; }
    public void setReturnDate(LocalDate d)   { this.returnDate = d; }
    public String getStatus()                { return status; }
    public void setStatus(String s)          { this.status = s; }
    public String getMemberName()            { return memberName; }
    public void setMemberName(String n)      { this.memberName = n; }
    public String getBookTitle()             { return bookTitle; }
    public void setBookTitle(String t)       { this.bookTitle = t; }

    @Override
    public String toString() {
        return String.format("Loan[id=%d, member='%s', book='%s', due=%s, status=%s]",
                loanId, memberName != null ? memberName : memberId,
                bookTitle != null ? bookTitle : bookId, dueDate, status);
    }
}

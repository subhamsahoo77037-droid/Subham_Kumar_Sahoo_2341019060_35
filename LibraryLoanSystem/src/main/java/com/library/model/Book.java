package com.library.model;

public class Book {
    private int bookId;
    private String title;
    private String author;
    private String isbn;
    private String genre;
    private boolean available;
    private int totalCopies;
    private int availableCopies;

    public Book() {}

    public Book(int bookId, String title, String author, String isbn, String genre,
                boolean available, int totalCopies, int availableCopies) {
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.genre = genre;
        this.available = available;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    // Getters and Setters
    public int getBookId()                   { return bookId; }
    public void setBookId(int id)            { this.bookId = id; }
    public String getTitle()                 { return title; }
    public void setTitle(String t)           { this.title = t; }
    public String getAuthor()                { return author; }
    public void setAuthor(String a)          { this.author = a; }
    public String getIsbn()                  { return isbn; }
    public void setIsbn(String i)            { this.isbn = i; }
    public String getGenre()                 { return genre; }
    public void setGenre(String g)           { this.genre = g; }
    public boolean isAvailable()             { return available; }
    public void setAvailable(boolean av)     { this.available = av; }
    public int getTotalCopies()              { return totalCopies; }
    public void setTotalCopies(int t)        { this.totalCopies = t; }
    public int getAvailableCopies()          { return availableCopies; }
    public void setAvailableCopies(int a)    { this.availableCopies = a; }

    @Override
    public String toString() {
        return String.format("Book[id=%d, title='%s', author='%s', isbn='%s', available=%b, copies=%d/%d]",
                bookId, title, author, isbn, available, availableCopies, totalCopies);
    }
}

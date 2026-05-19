package com.library.model;

import java.time.LocalDate;

public class Member {
    private int memberId;
    private String name;
    private String email;
    private String phone;
    private LocalDate joinDate;
    private int activeLoans;

    public Member() {}

    public Member(int memberId, String name, String email, String phone, LocalDate joinDate, int activeLoans) {
        this.memberId = memberId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.joinDate = joinDate;
        this.activeLoans = activeLoans;
    }

    // Getters and Setters
    public int getMemberId()            { return memberId; }
    public void setMemberId(int id)     { this.memberId = id; }
    public String getName()             { return name; }
    public void setName(String n)       { this.name = n; }
    public String getEmail()            { return email; }
    public void setEmail(String e)      { this.email = e; }
    public String getPhone()            { return phone; }
    public void setPhone(String p)      { this.phone = p; }
    public LocalDate getJoinDate()      { return joinDate; }
    public void setJoinDate(LocalDate d){ this.joinDate = d; }
    public int getActiveLoans()         { return activeLoans; }
    public void setActiveLoans(int a)   { this.activeLoans = a; }

    @Override
    public String toString() {
        return String.format("Member[id=%d, name='%s', email='%s', activeLoans=%d]",
                memberId, name, email, activeLoans);
    }
}

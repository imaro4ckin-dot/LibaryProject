package org.example.libaryproject;

public class CheckoutRecord {
    private final int    id;
    private final String bookTitle;
    private final String username;
    private final String checkedOutAt;
    private final String dueDate;
    private final String returnedAt;

    public CheckoutRecord(int id, String bookTitle, String username,
                          String checkedOutAt, String dueDate, String returnedAt) {
        this.id           = id;
        this.bookTitle    = bookTitle;
        this.username     = username;
        this.checkedOutAt = checkedOutAt;
        this.dueDate      = dueDate;
        this.returnedAt   = returnedAt;
    }

    public int    getId()           { return id; }
    public String getBookTitle()    { return bookTitle; }
    public String getUsername()     { return username; }
    public String getCheckedOutAt() { return checkedOutAt; }
    public String getDueDate()      { return dueDate; }
    public String getReturnedAt()   { return returnedAt != null ? returnedAt : "—"; }
}

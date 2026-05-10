package org.example.libaryproject;

public class Book {
    private int id;
    private String title;
    private String author;
    private boolean isAvaliable;
    private String dueDate;
    private String isbn     = "";
    private String category = "";

    public Book(int id, String title, String author, boolean isAvaliable) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isAvaliable = isAvaliable;
    }

    public Book(String title, String author) {
        this(-1, title, author, true);
    }

    public int getId()              { return id; }
    public void setId(int id)       { this.id = id; }

    public String getTitle()                { return title; }
    public void setTitle(String title)      { this.title = title; }

    public String getAuthor()               { return author; }
    public void setAuthor(String author)    { this.author = author; }

    public boolean isAvaliable()            { return isAvaliable; }
    public boolean getAvailable()           { return isAvaliable; }
    public void setAvaliable(boolean a)     { this.isAvaliable = a; }

    public String getDueDate()              { return dueDate; }
    public void setDueDate(String dueDate)  { this.dueDate = dueDate; }

    public String getIsbn()                 { return isbn == null ? "" : isbn; }
    public void setIsbn(String isbn)        { this.isbn = isbn == null ? "" : isbn; }

    public String getCategory()             { return category == null ? "" : category; }
    public void setCategory(String cat)     { this.category = cat == null ? "" : cat; }
}

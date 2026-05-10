package org.example.libaryproject;

public class Book {
    private int id;
    private String title;
    private String author;
    private boolean isAvaliable;

    public Book(int id, String title, String author, boolean isAvaliable) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.isAvaliable = isAvaliable;
    }

    public Book(String title, String author) {
        this(-1, title, author, true);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public boolean isAvaliable() { return isAvaliable; }
    public boolean getAvailable() { return isAvaliable; }
    public void setAvaliable(boolean avaliable) { this.isAvaliable = avaliable; }
}

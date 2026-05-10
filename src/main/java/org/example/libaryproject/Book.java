package org.example.libaryproject;

public class Book {
    private String title;
    private String author;
    private boolean isAvaliable;


//constructor
public Book(String title, String author ){
    this.title= title;
    this.author=author;
    this.isAvaliable= true;

}

//getters and setters

    public String getTitle() {return title;}

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {return author;}

    public void setAuthor(String author){
    this.author= author;
    }

    public boolean isAvaliable() {
        return isAvaliable;
    }

    public boolean getAvailable(){
        return isAvaliable;
    }

    public void setAvaliable(boolean avaliable) {
        isAvaliable = avaliable;
    }


}



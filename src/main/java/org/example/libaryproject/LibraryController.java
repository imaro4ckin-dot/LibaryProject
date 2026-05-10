package org.example.libaryproject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class LibraryController {

    // These @FXML tags link directly to the fx:id attributes in your FXML file
    @FXML private TextField titleInput;
    @FXML private TextField authorInput;
    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;
    @FXML private TableColumn<Book, Boolean> statusColumn;

    // ObservableList automatically updates the UI when a book is added
    private ObservableList<Book> bookList = FXCollections.observableArrayList();

    // This runs automatically when the app starts
    @FXML
    public void initialize() {
        // Tell the columns which variables to look for in the Book class
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("available"));

        // Put our list into the table
        bookTable.setItems(bookList);

        // Add a dummy book just to test
        bookList.add(new Book("The Hobbit", "J.R.R. Tolkien"));
    }

    // This is called when the "Add Book" button is clicked
    @FXML
    public void handleAddBook() {
        String title = titleInput.getText();
        String author = authorInput.getText();

        if (!title.isEmpty() && !author.isEmpty()) {
            Book newBook = new Book(title, author);
            bookList.add(newBook); // Adds to list, table updates automatically!

            // Clear the text fields
            titleInput.clear();
            authorInput.clear();
        }
    }
}
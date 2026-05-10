package org.example.libaryproject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class AdminController {

    @FXML private TextField titleInput;
    @FXML private TextField authorInput;
    @FXML private TableView<Book> bookTable;
    @FXML private TableColumn<Book, String> titleColumn;
    @FXML private TableColumn<Book, String> authorColumn;
    @FXML private TableColumn<Book, Boolean> statusColumn;

    private final ObservableList<Book> bookList = FXCollections.observableArrayList();
    private final DatabaseManager db = new DatabaseManager();

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("available"));
        bookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        bookTable.setItems(bookList);
        bookList.addAll(db.loadBooks());
    }

    @FXML
    public void handleAddBook() {
        String title = titleInput.getText().trim();
        String author = authorInput.getText().trim();
        if (!title.isEmpty() && !author.isEmpty()) {
            Book book = new Book(title, author);
            book.setId(db.addBook(book));
            bookList.add(book);
            titleInput.clear();
            authorInput.clear();
        }
    }

    @FXML
    public void handleCheckout() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getAvailable()) {
            selected.setAvaliable(false);
            db.updateAvailability(selected);
            bookTable.refresh();
        }
    }

    @FXML
    public void handleReturn() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected != null && !selected.getAvailable()) {
            selected.setAvaliable(true);
            db.updateAvailability(selected);
            bookTable.refresh();
        }
    }

    @FXML
    public void handleDelete() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            db.deleteBook(selected);
            bookList.remove(selected);
        }
    }

    @FXML
    public void handleLogout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("login-view.fxml"));
            Stage stage = (Stage) bookTable.getScene().getWindow();
            stage.setScene(new Scene(root, 360, 340));
            stage.setTitle("Library — Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

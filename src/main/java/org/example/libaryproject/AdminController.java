package org.example.libaryproject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public class AdminController {

    static final List<String> CATEGORIES = List.of(
            "All", "Fiction", "Non-Fiction", "Science", "History",
            "Biography", "Children", "Technology", "Art", "Philosophy",
            "Psychology", "Travel", "Other");

    @FXML private TextField   titleInput;
    @FXML private TextField   authorInput;
    @FXML private TextField   isbnInput;
    @FXML private ChoiceBox<String> categoryInput;
    @FXML private TextField   searchField;
    @FXML private ChoiceBox<String> categoryFilter;
    @FXML private TextField   editTitleField;
    @FXML private TextField   editAuthorField;
    @FXML private TextField   editIsbnField;
    @FXML private ChoiceBox<String> editCategoryField;
    @FXML private HBox        editBar;

    @FXML private TableView<Book>            bookTable;
    @FXML private TableColumn<Book, String>  titleColumn;
    @FXML private TableColumn<Book, String>  authorColumn;
    @FXML private TableColumn<Book, String>  isbnColumn;
    @FXML private TableColumn<Book, String>  categoryColumn;
    @FXML private TableColumn<Book, Void>    statusColumn;
    @FXML private TableColumn<Book, String>  dueDateColumn;
    @FXML private TableColumn<Book, Void>    checkedOutColumn;
    @FXML private Label userLabel;

    // Dashboard stat labels
    @FXML private Label totalLabel;
    @FXML private Label checkedOutLabel;
    @FXML private Label overdueLabel;

    private final ObservableList<Book> allBooks = FXCollections.observableArrayList();
    private FilteredList<Book> filteredBooks;
    private final ApiClient db = new ApiClient();
    private User currentUser;
    private Map<Integer, String> activeCheckouts;

    public void setUser(User user) {
        this.currentUser = user;
        userLabel.setText("Admin: " + user.getUsername());
        refresh();
    }

    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));

        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow().getItem() == null) { setText(null); return; }
                Book b = (Book) getTableRow().getItem();
                setText(b.getAvailable() ? "Available" : "Checked Out");
            }
        });

        checkedOutColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow().getItem() == null) { setText(null); return; }
                Book b = (Book) getTableRow().getItem();
                setText(activeCheckouts != null ? activeCheckouts.getOrDefault(b.getId(), "") : "");
            }
        });

        // 3-colour row factory: red=overdue, yellow=due soon
        bookTable.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Book book, boolean empty) {
                super.updateItem(book, empty);
                if (empty || book == null) {
                    setStyle("");
                } else if (isOverdue(book)) {
                    setStyle("-fx-background-color: #fff0f0;");
                } else if (isDueSoon(book)) {
                    setStyle("-fx-background-color: #fffde7;");
                } else {
                    setStyle("");
                }
            }
        });

        bookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Category dropdowns for add/edit
        ObservableList<String> cats = FXCollections.observableArrayList(
                CATEGORIES.subList(1, CATEGORIES.size())); // no "All" in add/edit
        categoryInput.setItems(cats);
        categoryInput.setValue("Other");
        editCategoryField.setItems(cats);
        editCategoryField.setValue("Other");

        // Category filter (includes "All")
        categoryFilter.setItems(FXCollections.observableArrayList(CATEGORIES));
        categoryFilter.setValue("All");

        filteredBooks = new FilteredList<>(allBooks, b -> true);

        // Combined search + category filter predicate
        Runnable updatePredicate = () -> {
            String q   = searchField.getText().toLowerCase();
            String cat = categoryFilter.getValue();
            filteredBooks.setPredicate(b -> {
                boolean matchesSearch = q.isEmpty()
                        || b.getTitle().toLowerCase().contains(q)
                        || b.getAuthor().toLowerCase().contains(q)
                        || b.getIsbn().toLowerCase().contains(q)
                        || b.getCategory().toLowerCase().contains(q);
                boolean matchesCat = "All".equals(cat) || cat.equals(b.getCategory());
                return matchesSearch && matchesCat;
            });
        };
        searchField.textProperty().addListener((obs, o, n) -> updatePredicate.run());
        categoryFilter.valueProperty().addListener((obs, o, n) -> updatePredicate.run());

        // Wrap in SortedList for sortable columns
        SortedList<Book> sortedBooks = new SortedList<>(filteredBooks);
        sortedBooks.comparatorProperty().bind(bookTable.comparatorProperty());
        bookTable.setItems(sortedBooks);
    }

    private boolean isOverdue(Book book) {
        if (book.getAvailable() || book.getDueDate() == null || book.getDueDate().isEmpty()) return false;
        try { return LocalDate.parse(book.getDueDate()).isBefore(LocalDate.now()); }
        catch (Exception e) { return false; }
    }

    private boolean isDueSoon(Book book) {
        if (book.getAvailable() || book.getDueDate() == null || book.getDueDate().isEmpty()) return false;
        try {
            LocalDate due = LocalDate.parse(book.getDueDate());
            LocalDate now = LocalDate.now();
            return !due.isBefore(now) && due.isBefore(now.plusDays(4));
        } catch (Exception e) { return false; }
    }

    private void refresh() {
        activeCheckouts = db.loadActiveCheckouts();
        allBooks.setAll(db.loadBooks());
        bookTable.refresh();

        // Update dashboard stats
        Map<String, Object> stats = db.getAdminStats();
        if (totalLabel != null) {
            totalLabel.setText(String.valueOf(stats.getOrDefault("totalBooks", 0)));
            checkedOutLabel.setText(String.valueOf(stats.getOrDefault("checkedOut", 0)));
            overdueLabel.setText(String.valueOf(stats.getOrDefault("overdue", 0)));
        }
    }

    @FXML
    public void handleAddBook() {
        String title    = titleInput.getText().trim();
        String author   = authorInput.getText().trim();
        String isbn     = isbnInput.getText().trim();
        String category = categoryInput.getValue();
        if (!title.isEmpty() && !author.isEmpty()) {
            Book book = new Book(title, author);
            book.setIsbn(isbn);
            book.setCategory(category == null ? "Other" : category);
            book.setId(db.addBook(book));
            titleInput.clear();
            authorInput.clear();
            isbnInput.clear();
            categoryInput.setValue("Other");
            refresh();
        }
    }

    @FXML
    public void handleCheckout() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null || !selected.getAvailable()) return;
        OptionalInt days = askLoanDuration();
        if (days.isPresent()) {
            db.checkoutBook(selected.getId(), currentUser.getId(), days.getAsInt());
            refresh();
        }
    }

    private OptionalInt askLoanDuration() {
        ChoiceBox<String> choice = new ChoiceBox<>();
        choice.getItems().addAll("1 week (7 days)", "2 weeks (14 days)", "3 weeks (21 days)", "1 month (30 days)");
        choice.setValue("2 weeks (14 days)");
        choice.setMaxWidth(Double.MAX_VALUE);

        VBox content = new VBox(8, new Label("Select loan duration:"), choice);
        content.setStyle("-fx-padding: 10 0 0 0;");

        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION);
        dialog.setTitle("Checkout Book");
        dialog.setHeaderText("How long do you need the book?");
        dialog.getDialogPane().setContent(content);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return OptionalInt.empty();

        int days = switch (choice.getValue()) {
            case "1 week (7 days)"   -> 7;
            case "3 weeks (21 days)" -> 21;
            case "1 month (30 days)" -> 30;
            default                  -> 14;
        };
        return OptionalInt.of(days);
    }

    @FXML
    public void handleReturn() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getAvailable()) return;
        Optional<ButtonType> result = confirm(
                "Return Book", "Return \"" + selected.getTitle() + "\"?",
                "This will mark the book as available.");
        if (result.isPresent() && result.get() == ButtonType.OK) {
            db.adminReturnBook(selected.getId());
            refresh();
        }
    }

    @FXML
    public void handleDelete() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Optional<ButtonType> result = confirm(
                "Delete Book", "Delete \"" + selected.getTitle() + "\"?",
                "This action cannot be undone.");
        if (result.isPresent() && result.get() == ButtonType.OK) {
            db.deleteBook(selected);
            refresh();
        }
    }

    @FXML
    public void handleEditStart() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        editTitleField.setText(selected.getTitle());
        editAuthorField.setText(selected.getAuthor());
        editIsbnField.setText(selected.getIsbn());
        editCategoryField.setValue(selected.getCategory().isEmpty() ? "Other" : selected.getCategory());
        editBar.setVisible(true);
        editBar.setManaged(true);
    }

    @FXML
    public void handleEditSave() {
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        String title    = editTitleField.getText().trim();
        String author   = editAuthorField.getText().trim();
        String isbn     = editIsbnField.getText().trim();
        String category = editCategoryField.getValue();
        if (!title.isEmpty() && !author.isEmpty()) {
            db.updateBook(selected.getId(), title, author, isbn, category == null ? "Other" : category);
            hideEditBar();
            refresh();
        }
    }

    @FXML
    public void handleEditCancel() {
        hideEditBar();
    }

    private void hideEditBar() {
        editBar.setVisible(false);
        editBar.setManaged(false);
        editTitleField.clear();
        editAuthorField.clear();
        editIsbnField.clear();
        editCategoryField.setValue("Other");
    }

    @FXML
    public void handleHistory() {
        openModal("history-view.fxml", "Checkout History", 700, 480);
    }

    @FXML
    public void handleManageUsers() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("manage-users-view.fxml"));
            Parent root = loader.load();
            ManageUsersController ctrl = loader.getController();
            ctrl.setCurrentAdmin(currentUser);
            Stage modal = new Stage();
            modal.setTitle("Manage Users");
            modal.initModality(Modality.WINDOW_MODAL);
            modal.initOwner(bookTable.getScene().getWindow());
            modal.setScene(new Scene(root));
            modal.showAndWait();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void handleExportBooks() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Books");
        chooser.setInitialFileName("books.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showSaveDialog(bookTable.getScene().getWindow());
        if (file != null) {
            try { db.exportBooksCSV(file.getAbsolutePath()); }
            catch (Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    public void handleLogout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("login-view.fxml"));
            Stage stage = (Stage) bookTable.getScene().getWindow();
            stage.setScene(new Scene(root, 360, 340));
            stage.setTitle("Library — Login");
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void openModal(String fxml, String title, double w, double h) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();
            Stage modal = new Stage();
            modal.setTitle(title);
            modal.initModality(Modality.WINDOW_MODAL);
            modal.initOwner(bookTable.getScene().getWindow());
            modal.setScene(new Scene(root, w, h));
            modal.showAndWait();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private Optional<ButtonType> confirm(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        return alert.showAndWait();
    }
}

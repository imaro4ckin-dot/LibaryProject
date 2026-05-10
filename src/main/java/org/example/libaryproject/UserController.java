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
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

public class UserController {

    private static final int MAX_BOOKS = 5;

    // Available tab
    @FXML private TableView<Book>           availableTable;
    @FXML private TableColumn<Book, String> availTitleColumn;
    @FXML private TableColumn<Book, String> availAuthorColumn;
    @FXML private TableColumn<Book, String> availIsbnColumn;
    @FXML private TableColumn<Book, String> availCategoryColumn;

    // My Books tab
    @FXML private TableView<Book>           myBooksTable;
    @FXML private TableColumn<Book, String> myTitleColumn;
    @FXML private TableColumn<Book, String> myAuthorColumn;
    @FXML private TableColumn<Book, String> myDueDateColumn;
    @FXML private TableColumn<Book, Void>   myStatusColumn;

    @FXML private TabPane   tabPane;
    @FXML private TextField searchField;
    @FXML private Label     userLabel;

    private final ObservableList<Book> availableBooks = FXCollections.observableArrayList();
    private final ObservableList<Book> myBooks        = FXCollections.observableArrayList();

    private final ApiClient db = new ApiClient();
    private User currentUser;
    private Set<Integer> myCheckoutIds = new HashSet<>();

    public void setUser(User user) {
        this.currentUser = user;
        userLabel.setText(user.getUsername());
        refresh();
    }

    @FXML
    public void initialize() {
        // Available tab columns
        availTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        availAuthorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        availIsbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        availCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        availableTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // My Books tab columns
        myTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        myAuthorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        myDueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        myStatusColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getTableRow().getItem() == null) { setText(null); return; }
                Book b = (Book) getTableRow().getItem();
                setText(isOverdue(b) ? "Overdue" : "On loan");
            }
        });
        myBooksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 3-colour row factory for My Books: red=overdue, yellow=due within 3 days
        myBooksTable.setRowFactory(tv -> new TableRow<>() {
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

        // Search filters both tabs using the same search field
        FilteredList<Book> filteredAvailable = new FilteredList<>(availableBooks, b -> true);
        FilteredList<Book> filteredMyBooks   = new FilteredList<>(myBooks, b -> true);

        searchField.textProperty().addListener((obs, old, q) -> {
            String lower = q.toLowerCase();
            filteredAvailable.setPredicate(b ->
                    q.isEmpty() ||
                    b.getTitle().toLowerCase().contains(lower) ||
                    b.getAuthor().toLowerCase().contains(lower) ||
                    b.getIsbn().toLowerCase().contains(lower) ||
                    b.getCategory().toLowerCase().contains(lower));
            filteredMyBooks.setPredicate(b ->
                    q.isEmpty() ||
                    b.getTitle().toLowerCase().contains(lower) ||
                    b.getAuthor().toLowerCase().contains(lower));
        });

        // Wrap in SortedList so clicking column headers sorts the table
        SortedList<Book> sortedAvailable = new SortedList<>(filteredAvailable);
        SortedList<Book> sortedMyBooks   = new SortedList<>(filteredMyBooks);
        sortedAvailable.comparatorProperty().bind(availableTable.comparatorProperty());
        sortedMyBooks.comparatorProperty().bind(myBooksTable.comparatorProperty());

        availableTable.setItems(sortedAvailable);
        myBooksTable.setItems(sortedMyBooks);
    }

    private boolean isOverdue(Book book) {
        if (book.getDueDate() == null || book.getDueDate().isEmpty()) return false;
        try { return LocalDate.parse(book.getDueDate()).isBefore(LocalDate.now()); }
        catch (Exception e) { return false; }
    }

    private boolean isDueSoon(Book book) {
        if (book.getDueDate() == null || book.getDueDate().isEmpty()) return false;
        try {
            LocalDate due = LocalDate.parse(book.getDueDate());
            LocalDate now = LocalDate.now();
            return !due.isBefore(now) && due.isBefore(now.plusDays(4));
        } catch (Exception e) { return false; }
    }

    private void refresh() {
        myCheckoutIds = new HashSet<>(db.getCheckedOutBookIds(currentUser.getId()));
        availableBooks.setAll(db.loadBooks().stream()
                .filter(Book::getAvailable)
                .toList());
        myBooks.setAll(db.getBooksCheckedOutByUser(currentUser.getId()));
    }

    @FXML
    public void handleCheckout() {
        Book selected = availableTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (myBooks.size() >= MAX_BOOKS) {
            new Alert(Alert.AlertType.WARNING,
                    "You have reached the checkout limit of " + MAX_BOOKS + " books.\nPlease return a book before checking out another.",
                    ButtonType.OK).showAndWait();
            return;
        }

        OptionalInt days = askLoanDuration();
        if (days.isPresent()) {
            db.checkoutBook(selected.getId(), currentUser.getId(), days.getAsInt());
            refresh();
            tabPane.getSelectionModel().select(1);
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
        Book selected = myBooksTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION,
                "Return \"" + selected.getTitle() + "\"?", ButtonType.OK, ButtonType.CANCEL)
                .showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            db.returnBook(selected.getId(), currentUser.getId());
            refresh();
        }
    }

    @FXML
    public void handleChangePassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("change-password-view.fxml"));
            Parent root = loader.load();
            ChangePasswordController ctrl = loader.getController();
            ctrl.setUser(currentUser);
            Stage modal = new Stage();
            modal.setTitle("Change Password");
            modal.initModality(Modality.WINDOW_MODAL);
            modal.initOwner(availableTable.getScene().getWindow());
            modal.setScene(new Scene(root));
            modal.showAndWait();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    public void handleLogout() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("login-view.fxml"));
            Stage stage = (Stage) availableTable.getScene().getWindow();
            stage.setScene(new Scene(root, 360, 360));
            stage.setTitle("Library — Login");
        } catch (Exception e) { e.printStackTrace(); }
    }
}

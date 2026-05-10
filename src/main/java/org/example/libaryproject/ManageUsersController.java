package org.example.libaryproject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class ManageUsersController {

    @FXML private TableView<User>            userTable;
    @FXML private TableColumn<User, String>  usernameColumn;
    @FXML private TableColumn<User, String>  roleColumn;

    @FXML private TableView<Book>            userBooksTable;
    @FXML private TableColumn<Book, String>  bookTitleColumn;
    @FXML private TableColumn<Book, String>  bookDueColumn;
    @FXML private Label                      detailLabel;

    @FXML private TextField                  newUsername;
    @FXML private PasswordField              newPassword;
    @FXML private ChoiceBox<String>          roleChoice;
    @FXML private ChoiceBox<String>          questionChoice;
    @FXML private TextField                  securityAnswer;
    @FXML private Label                      errorLabel;

    private static final List<String> QUESTIONS = List.of(
            "What was the name of your first pet?",
            "What city were you born in?",
            "What is your mother's maiden name?",
            "What was the name of your primary school?",
            "What was the make of your first car?"
    );

    private final ObservableList<User> userList      = FXCollections.observableArrayList();
    private final ObservableList<Book> userBooksList = FXCollections.observableArrayList();
    private final ApiClient db = new ApiClient();
    private User currentAdmin;

    public void setCurrentAdmin(User admin) {
        this.currentAdmin = admin;
        refresh();
    }

    @FXML
    public void initialize() {
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        userTable.setItems(userList);

        bookTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        bookDueColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        userBooksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        userBooksTable.setItems(userBooksList);

        roleChoice.setItems(FXCollections.observableArrayList("user", "admin"));
        roleChoice.setValue("user");

        questionChoice.setItems(FXCollections.observableArrayList(QUESTIONS));
        questionChoice.setValue(QUESTIONS.get(0));

        userTable.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            userBooksList.clear();
            if (selected == null) {
                detailLabel.setText("Select a user to see their books");
            } else {
                detailLabel.setText("Books checked out by: " + selected.getUsername());
                userBooksList.setAll(db.getBooksCheckedOutByUser(selected.getId()));
            }
        });
    }

    private void refresh() {
        userList.setAll(db.loadUsers());
        userBooksList.clear();
        detailLabel.setText("Select a user to see their books");
        errorLabel.setText("");
    }

    @FXML
    public void handleAddUser() {
        String username = newUsername.getText().trim();
        String password = newPassword.getText();
        String role     = roleChoice.getValue();
        String question = questionChoice.getValue();
        String answer   = securityAnswer.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Username and password are required.");
            return;
        }
        if (answer.isEmpty()) {
            errorLabel.setText("Please provide a security answer.");
            return;
        }
        if (db.usernameExists(username)) {
            errorLabel.setText("Username \"" + username + "\" is already taken.");
            return;
        }
        if (db.addUser(username, password, role, question, answer) == -1) {
            errorLabel.setText("Failed to create user.");
            return;
        }
        newUsername.clear();
        newPassword.clear();
        securityAnswer.clear();
        roleChoice.setValue("user");
        questionChoice.setValue(QUESTIONS.get(0));
        refresh();
    }

    @FXML
    public void handleDeleteUser() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            errorLabel.setText("Select a user to delete.");
            return;
        }
        if (currentAdmin != null && selected.getId() == currentAdmin.getId()) {
            errorLabel.setText("You cannot delete your own account.");
            return;
        }
        db.deleteUser(selected.getId());
        refresh();
    }
}

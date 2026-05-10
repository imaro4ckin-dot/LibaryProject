package org.example.libaryproject;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private final DatabaseManager db = new DatabaseManager();

    @FXML
    public void initialize() {
        db.initDB();
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        String role = db.login(username, password);

        if (role == null) {
            errorLabel.setText("Invalid username or password.");
            return;
        }

        try {
            String fxml = role.equals("admin") ? "admin-view.fxml" : "user-view.fxml";
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 700, 500));
            stage.setTitle("Library — " + capitalize(role));
        } catch (Exception e) {
            errorLabel.setText("Failed to load screen.");
            e.printStackTrace();
        }
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

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

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label         messageLabel;

    private final ApiClient db = new ApiClient();

    @FXML
    public void initialize() {
        db.initDB();
    }

    /** Called by ForgotPasswordController to surface a success message on return. */
    public void showSuccess(String message) {
        messageLabel.setStyle("-fx-text-fill: #16a34a;"); // green
        messageLabel.setText(message);
    }

    @FXML
    public void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        User user;
        try {
            user = db.login(username, password);
        } catch (Exception e) {
            messageLabel.setStyle("-fx-text-fill: #c0392b;");
            messageLabel.setText("Cannot connect to server. Make sure LibraryServer is running.");
            return;
        }

        if (user == null) {
            messageLabel.setStyle("-fx-text-fill: #c0392b;");
            messageLabel.setText("Invalid username or password.");
            return;
        }

        try {
            String fxml = user.getRole().equals("admin") ? "admin-view.fxml" : "user-view.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Parent root = loader.load();

            if (user.getRole().equals("admin")) {
                ((AdminController) loader.getController()).setUser(user);
            } else {
                ((UserController) loader.getController()).setUser(user);
            }

            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 750, 520));
            stage.setTitle("Library — " + capitalize(user.getRole()));
        } catch (Exception e) {
            messageLabel.setText("Failed to load screen.");
            e.printStackTrace();
        }
    }

    @FXML
    public void handleForgotPassword() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("forgot-password-view.fxml"));
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 380, 340));
            stage.setTitle("Library — Reset Password");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

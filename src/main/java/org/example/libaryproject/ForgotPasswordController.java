package org.example.libaryproject;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ForgotPasswordController {

    @FXML private VBox          step1Box;
    @FXML private VBox          step2Box;
    @FXML private TextField     usernameField;
    @FXML private Label         questionLabel;
    @FXML private TextField     answerField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmField;
    @FXML private Label         messageLabel;

    private final ApiClient db = new ApiClient();

    @FXML
    public void handleLookup() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            messageLabel.setText("Please enter your username.");
            return;
        }

        String question = db.getSecurityQuestion(username);
        if (question == null) {
            messageLabel.setText("No account found with that username, or no security question is set.");
            return;
        }

        questionLabel.setText(question);
        messageLabel.setText("");
        step1Box.setVisible(false);
        step1Box.setManaged(false);
        step2Box.setVisible(true);
        step2Box.setManaged(true);
    }

    @FXML
    public void handleReset() {
        String answer     = answerField.getText().trim();
        String newPw      = newPasswordField.getText();
        String confirm    = confirmField.getText();
        String username   = usernameField.getText().trim();

        if (answer.isEmpty() || newPw.isEmpty() || confirm.isEmpty()) {
            messageLabel.setText("All fields are required.");
            return;
        }
        if (!newPw.equals(confirm)) {
            messageLabel.setText("Passwords do not match.");
            return;
        }
        if (newPw.length() < 4) {
            messageLabel.setText("Password must be at least 4 characters.");
            return;
        }

        boolean ok = db.resetPasswordIfAnswerCorrect(username, answer, newPw);
        if (!ok) {
            messageLabel.setText("Incorrect answer. Please try again.");
            return;
        }

        // Success — go back to login with a success message
        goToLogin("Password reset successfully. Please sign in.");
    }

    @FXML
    public void handleBack() {
        goToLogin(null);
    }

    private void goToLogin(String successMessage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("login-view.fxml"));
            Parent root = loader.load();
            if (successMessage != null) {
                LoginController ctrl = loader.getController();
                ctrl.showSuccess(successMessage);
            }
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 360, 340));
            stage.setTitle("Library — Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

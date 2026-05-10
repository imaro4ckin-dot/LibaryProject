package org.example.libaryproject;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

public class ChangePasswordController {

    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label         errorLabel;

    private final ApiClient db = new ApiClient();
    private User currentUser;

    public void setUser(User user) {
        this.currentUser = user;
    }

    @FXML
    public void handleSave() {
        String current = currentPasswordField.getText();
        String newPw   = newPasswordField.getText();
        String confirm = confirmPasswordField.getText();

        if (current.isEmpty() || newPw.isEmpty() || confirm.isEmpty()) {
            errorLabel.setText("All fields are required.");
            return;
        }
        if (!newPw.equals(confirm)) {
            errorLabel.setText("New passwords do not match.");
            return;
        }
        if (newPw.length() < 4) {
            errorLabel.setText("Password must be at least 4 characters.");
            return;
        }

        boolean ok = db.changePassword(currentUser.getId(), current, newPw);
        if (!ok) {
            errorLabel.setText("Current password is incorrect.");
            return;
        }
        ((Stage) currentPasswordField.getScene().getWindow()).close();
    }
}

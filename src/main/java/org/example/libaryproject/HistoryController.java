package org.example.libaryproject;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class HistoryController {

    @FXML private TableView<CheckoutRecord>            historyTable;
    @FXML private TableColumn<CheckoutRecord, String>  bookTitleColumn;
    @FXML private TableColumn<CheckoutRecord, String>  usernameColumn;
    @FXML private TableColumn<CheckoutRecord, String>  checkedOutAtColumn;
    @FXML private TableColumn<CheckoutRecord, String>  dueDateColumn;
    @FXML private TableColumn<CheckoutRecord, String>  returnedAtColumn;
    @FXML private TextField                            searchField;
    @FXML private Label                                countLabel;

    private final ObservableList<CheckoutRecord> allRecords = FXCollections.observableArrayList();
    private final ApiClient db = new ApiClient();

    @FXML
    public void initialize() {
        bookTitleColumn.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        checkedOutAtColumn.setCellValueFactory(new PropertyValueFactory<>("checkedOutAt"));
        dueDateColumn.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        returnedAtColumn.setCellValueFactory(new PropertyValueFactory<>("returnedAt"));

        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        allRecords.addAll(db.loadFullHistory());

        FilteredList<CheckoutRecord> filtered = new FilteredList<>(allRecords, r -> true);
        searchField.textProperty().addListener((obs, old, q) -> {
            String lower = q.toLowerCase();
            filtered.setPredicate(r ->
                    q.isEmpty() ||
                    r.getBookTitle().toLowerCase().contains(lower) ||
                    r.getUsername().toLowerCase().contains(lower));
            updateCount(filtered.size());
        });

        historyTable.setItems(filtered);
        updateCount(allRecords.size());
    }

    private void updateCount(int n) {
        countLabel.setText(n + " record" + (n == 1 ? "" : "s"));
    }

    @FXML
    public void handleExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Checkout History");
        chooser.setInitialFileName("checkout_history.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showSaveDialog((Stage) historyTable.getScene().getWindow());
        if (file != null) {
            try {
                db.exportHistoryCSV(file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

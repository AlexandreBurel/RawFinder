package fr.lsmbo.rawfinder;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class GuiSettings {

    protected static final Logger logger = LoggerFactory.getLogger(GuiSettings.class);
    protected Stage dialogStage;
    private final HashMap<Boolean, String> rawDataTypes = new HashMap<Boolean, String>() {{
        put(true, "Folder-like (ie. Bruker .d directories)");
        put(false, "File-like (ie. Thermo .raw files)");
    }};
    private Settings currentSettings;

    @FXML
    TextField txtRawDataDirectory;
    @FXML
    Button btnRawDataChooser;
    @FXML
    TextField txtArchiveDirectory;
    @FXML
    Button btnArchiveChooser;
    @FXML
    ChoiceBox<String> cmbRawDataFormat;
    @FXML
    TextField txtRawDataTemplate;
    @FXML
    TextField txtReportDirectory;
    @FXML
    Button btnReportChooser;
    @FXML
    Button btnSave;
    @FXML
    Button btnCancel;

    @FXML
    public void initialize(Stage _dialogStage) {
        dialogStage = _dialogStage;
        currentSettings = new Settings(Global.RAW_DATA_DIRECTORY, Global.RAW_DATA_ARCHIVES, Global.IS_FOLDER_LIKE, Global.FOLDER_LIKE_RAW_DATA_TEMPLATE, Global.FILE_LIKE_RAW_DATA_TEMPLATE);

        // fill the ChoiceBox
        cmbRawDataFormat.getItems().add(rawDataTypes.get(true));
        cmbRawDataFormat.getItems().add(rawDataTypes.get(false));

        // predefine fields with Settings
        txtRawDataDirectory.setText(currentSettings.getRawDataDirectory());
        txtArchiveDirectory.setText(currentSettings.getArchiveDirectory());
        cmbRawDataFormat.setValue(rawDataTypes.get(currentSettings.getFolderLike()));
        txtRawDataTemplate.setText(currentSettings.getFolderLike() ? currentSettings.getFolderLikeRawDataTemplate() : currentSettings.getFileLikeRawDataTemplate());
        txtReportDirectory.setText(currentSettings.getDefaultReportDirectory());

        cmbRawDataFormat.setOnAction((event -> {
            String selectedItem = cmbRawDataFormat.getSelectionModel().getSelectedItem();
            boolean isFolderLikeSelected = selectedItem.equals(rawDataTypes.get(true));
            // store the old value first, then set the other stored value
            if(isFolderLikeSelected) {
                currentSettings.setFileLikeRawDataTemplate(txtRawDataTemplate.getText());
                txtRawDataTemplate.setText(currentSettings.getFolderLikeRawDataTemplate());
            } else {
                currentSettings.setFolderLikeRawDataTemplate(txtRawDataTemplate.getText());
                txtRawDataTemplate.setText(currentSettings.getFileLikeRawDataTemplate());
            }
        }));
    }

    @FXML
    private void rawDataDirectoryButtonListener() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Raw Data main directory");
        dc.setInitialDirectory(getDirectory(txtRawDataDirectory));
        File directory = dc.showDialog(dialogStage);
        if(directory != null) {
            txtRawDataDirectory.setText(directory.getAbsolutePath());
        }
    }

    @FXML
    private void archiveDirectoryButtonListener() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Archive main directory");
        dc.setInitialDirectory(getDirectory(txtArchiveDirectory));
        File directory = dc.showDialog(dialogStage);
        if(directory != null) {
            txtArchiveDirectory.setText(directory.getAbsolutePath());
        }
    }

    @FXML
    private void reportDirectoryButtonListener() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Reports default directory");
        dc.setInitialDirectory(getDirectory(txtReportDirectory));
        File directory = dc.showDialog(dialogStage);
        if(directory != null) {
            txtReportDirectory.setText(directory.getAbsolutePath());
        }
    }

    private File getDirectory(TextField textField) {
        File directory = new File(System.getProperty("user.home"));
        if(textField != null && textField.getText() != null && !textField.getText().equals("")) {
            directory = new File(textField.getText());
        }
        return directory;
    }

    @FXML
    private void saveButtonListener() {
        // update global settings with current settings
        Global.RAW_DATA_DIRECTORY = new File(txtRawDataDirectory.getText());
        Global.RAW_DATA_ARCHIVES = new File(txtArchiveDirectory.getText());
        if(cmbRawDataFormat.getSelectionModel().getSelectedItem().equals(rawDataTypes.get(true))) {
            Global.IS_FOLDER_LIKE = true;
            Global.FOLDER_LIKE_RAW_DATA_TEMPLATE = Arrays.stream(txtRawDataTemplate.getText().split(" ")).collect(Collectors.toList());
        } else {
            Global.IS_FOLDER_LIKE = false;
            Global.FILE_LIKE_RAW_DATA_TEMPLATE = Arrays.stream(txtRawDataTemplate.getText().split(" ")).collect(Collectors.toList());
        }
        Global.REPORTS_DIRECTORY = new File(txtReportDirectory.getText());
        // update settings file
        try {
            Global.updateSettingsFile();
        } catch (Throwable t) {
            logger.error("Settings could not be saved", t);
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("RawFinder");
            alert.setHeaderText("Error while saving settings, the new values will not be stored in the permanent file");
            alert.setContentText(t.getMessage());
            alert.showAndWait();
        }
        dialogStage.close();
    }

    @FXML
    private void cancelButtonListener() {
        dialogStage.close();
    }
}

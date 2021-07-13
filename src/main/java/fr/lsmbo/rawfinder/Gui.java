package fr.lsmbo.rawfinder;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Optional;

public class Gui {

    protected static final Logger logger = LoggerFactory.getLogger(Gui.class);
    protected static Stage dialogStage;
    private DataParser parser = null;
    private final ObservableList<RawData> data = FXCollections.observableArrayList();
    private final Alert exitPopup = new Alert(Alert.AlertType.CONFIRMATION);
    private final ButtonType btnYes = new ButtonType("Yes", ButtonBar.ButtonData.YES);
    private final ButtonType btnNo = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

    @FXML
    TableView<RawData> table;
    @FXML
    TableColumn<RawData, String> tcName;
    @FXML
    TableColumn<RawData, Double> tcSize;
    @FXML
    TableColumn<RawData, Date> tcDate;
    @FXML
    TableColumn<RawData, String> tcStatus;
    @FXML
    Button btnStart;
    @FXML
    Button btnClear;
    @FXML
    Button btnSettings;
    @FXML
    Button btnExport;
    @FXML
    Button btnCancel;
    @FXML
    Button btnQuit;
    @FXML
    ProgressIndicator progressIndicator;

    public void setDialogStage(Stage primaryStage) {
        dialogStage = primaryStage;
        dialogStage.setOnCloseRequest(e -> beforeClosing());
    }

    @FXML
    private void initialize() {
        table.setItems(data);
        tcName.setCellValueFactory(new PropertyValueFactory<>("name"));
        tcSize.setCellValueFactory(new PropertyValueFactory<>("size"));
        tcDate.setCellValueFactory(new PropertyValueFactory<>("date"));
        tcStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        //  format columns Size and Date
        tcSize.setCellFactory(column -> new TableCell<RawData, Double>() {
            @Override
            protected void updateItem(Double size, boolean empty) {
                super.updateItem(size, empty);
                setText(empty ? null : formatFileSize(size.longValue()));
            }
        });

        tcDate.setCellFactory(column -> new TableCell<RawData, Date>() {
            @Override
            protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : Global.simpleFormatDate2(item.getTime()));
            }
        });

        // Confirmation before exit
        exitPopup.setTitle("Quit application");
        exitPopup.setHeaderText("Do you want to quit this application ?");
        exitPopup.getButtonTypes().setAll(btnYes, btnNo);
        // this is to remove YES to be the default button (if so, it will be triggered when user pressed Enter, even if the focus is on CANCEL)
        DialogPane popupDialogPane = exitPopup.getDialogPane();
        ((Button)(popupDialogPane.lookupButton(btnYes))).setDefaultButton(false);
        // the following will trigger the focused button when user presses Enter
        EventHandler<KeyEvent> fireOnEnter = event -> {
            if (KeyCode.ENTER.equals(event.getCode()) && event.getTarget() instanceof Button)
                ((Button) event.getTarget()).fire();
        };
        popupDialogPane.getButtonTypes().stream().map(popupDialogPane::lookupButton).forEach(button -> button.addEventHandler(KeyEvent.KEY_PRESSED, fireOnEnter));

        // manage buttons
        btnClear.setDisable(true);
        btnExport.setDisable(true);
        btnCancel.setDisable(true);
    }

    private String formatFileSize(long size) {
        final String[] unitNames = {"octets", "Ko", "Mo", "Go", "To", "Po", "Eo", "Zo", "Yo"};
        int i ;
        for (i = 0 ; size > 1024 && i < unitNames.length - 1 ; i++) {
            size = size / 1024 ;
        }
        return String.format("%,d %s", size, unitNames[i]);
    }

    @FXML
    private void startListener() {
        data.clear();
        parser = new DataParser(Global.RAW_DATA_DIRECTORY);
        try {
            toggleButtons();
            final DataParserThread thread = new DataParserThread();
            thread.setParentDirectory(Global.RAW_DATA_DIRECTORY);
            thread.setOnSucceeded(event -> {
                DataParser parser = thread.getValue();
                parser.getAsRawData().keySet().forEach(key -> data.add(parser.getAsRawData().get(key)));
                toggleButtons();
            });
            thread.setOnFailed(event -> {
                showAlertDialog(Alert.AlertType.ERROR, "Data parsing error", event.getSource().getMessage());
                logger.error(event.getSource().getMessage(), event.getSource());
                toggleButtons();
            });
            thread.setOnCancelled(event -> {
                showAlertDialog(Alert.AlertType.INFORMATION, null, "Data parsing cancelled");
                toggleButtons();
            });
            new Thread(thread).start();
        } catch (Throwable t) {
            showAlertDialog(Alert.AlertType.ERROR, "Data parsing error", t.getMessage());
            logger.error(t.getMessage(), t);
        }
    }

    private void toggleButtons() {
        boolean isRunning = !progressIndicator.isVisible();
        btnStart.setDisable(isRunning);
        btnClear.setDisable(isRunning || data.size() == 0);
        btnSettings.setDisable(isRunning);
        btnExport.setDisable(isRunning || data.size() == 0);
//        btnCancel.setDisable(!isRunning);
        btnCancel.setDisable(true);
        btnQuit.setDisable(isRunning);
        table.setDisable(isRunning);
        progressIndicator.setVisible(isRunning);
    }

    @FXML
    private void exitListener() {
        logger.debug("Opening Exit window");
        Optional<ButtonType> result = exitPopup.showAndWait();
        if (result.orElse(ButtonType.NO) == btnYes) {
            beforeClosing();
            dialogStage.close();
        }
    }

    private void beforeClosing() {
        try {
            Platform.exit();
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
        }
    }

    @FXML
    private void exportListener() {
        try {
            // add a FileChooser to define where to generate the output file
            FileChooser fc = new FileChooser();
            fc.setTitle("Save as...");
            fc.setInitialDirectory(Global.REPORTS_DIRECTORY);
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel", "*.xlsx"));
            fc.setInitialFileName(Export.getDefaultFileName());

            File outputFile = fc.showSaveDialog(dialogStage);
            if(outputFile != null) {
                toggleButtons();
                final ExportThread thread = new ExportThread();
                thread.initialize(parser, outputFile);
                thread.setOnSucceeded(event -> {
                    thread.getValue();
                    showAlertDialog(Alert.AlertType.INFORMATION, null, "Export is finished");
                    toggleButtons();
                });
                thread.setOnFailed(event -> {
                    showAlertDialog(Alert.AlertType.ERROR, "Export error", event.getSource().getMessage());
                    logger.error(event.getSource().getMessage(), event.getSource());
                });
                new Thread(thread).start();



//                Export export = new Export(parser);
//
//                try {
//                    toggleProgressIndicator();
//                    new Thread(() -> Platform.runLater(() -> {
//                        try {
//                            export.start(outputFile);
//                            showAlertDialog(Alert.AlertType.INFORMATION, null, "Export is finished");
//                        } catch (Throwable throwable) {
//                            showAlertDialog(Alert.AlertType.ERROR, "Export error", throwable.getMessage());
//                            logger.error(throwable.getMessage(), throwable);
//                            throwable.printStackTrace();
//                        }
//                        toggleProgressIndicator();
//                    })).start();
//                } catch (Throwable t) {
//                    showAlertDialog(Alert.AlertType.ERROR, "Export error", t.getMessage());
//                    logger.error(t.getMessage(), t);
//                }
//                export.start(outputFile);
//                showAlertDialog(Alert.AlertType.INFORMATION, null, "Export is finished");
            }
        } catch (Throwable t) {
            // display the error message if any
            showAlertDialog(Alert.AlertType.ERROR, "Export error", t.getMessage());
            logger.error(t.getMessage(), t);
            t.printStackTrace();
        }
    }

    private Optional<ButtonType> showAlertDialog(Alert.AlertType type, String header, String text) {
        Alert alert = new Alert(type);
        alert.setTitle("RawFinder");
        alert.setHeaderText(header);
        alert.setContentText(text);
        return alert.showAndWait();
    }

    @FXML
    private void settingsListener() {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("GuiSettings.fxml"));
            AnchorPane pane = loader.load();

            Stage thisStage = new Stage();
            thisStage.setTitle("Settings");
            thisStage.initModality(Modality.WINDOW_MODAL);
            thisStage.initOwner(dialogStage);

            Scene scene = new Scene(pane);
            thisStage.setScene(scene);
            GuiSettings controller = loader.getController();
            controller.initialize(thisStage);
            thisStage.showAndWait();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @FXML
    private void clearListener() {
        data.clear();
        btnClear.setDisable(true);
        btnExport.setDisable(true);
    }
}

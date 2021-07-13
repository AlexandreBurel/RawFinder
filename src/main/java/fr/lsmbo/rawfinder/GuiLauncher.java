package fr.lsmbo.rawfinder;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class GuiLauncher extends Application {

    protected static final Logger logger = LoggerFactory.getLogger(GuiLauncher.class);

    public static void run() {
        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle(Global.getAppTitle());

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(this.getClass().getResource("Gui.fxml"));
            StackPane page = loader.load();
            Scene scene = new Scene(page);
            primaryStage.setScene(scene);
            Gui controller = loader.getController();
            controller.setDialogStage(primaryStage);

            // display frame
            primaryStage.show();
        } catch(IOException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
        }
    }
}

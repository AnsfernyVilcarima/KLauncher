package org.klauncher.launcher;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LauncherApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(LauncherApplication.class);

    @Override
    public void start(Stage stage) throws IOException {
        logger.info("Iniciando KarritoLauncher...");

        try {
            FXMLLoader fxmlLoader = new FXMLLoader(LauncherApplication.class.getResource("/fxml/main-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 800, 600);

            stage.setTitle("Karrito Launcher");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

            logger.info("Aplicación iniciada correctamente");
        } catch (Exception e) {
            logger.error("Error al iniciar la aplicación", e);
            throw e;
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
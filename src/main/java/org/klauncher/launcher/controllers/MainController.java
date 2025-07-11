package org.klauncher.launcher.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML
    private Label welcomeText;

    @FXML
    private TextField usernameField;

    @FXML
    private Button loginButton;

    @FXML
    private Button playButton;

    @FXML
    private ProgressBar downloadProgress;

    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {
        logger.info("Inicializando controlador principal");

        // Configuración inicial
        welcomeText.setText("Bienvenido a Karrito Launcher");
        statusLabel.setText("Listo para comenzar");
        downloadProgress.setVisible(false);
        playButton.setDisable(true);

        // Configurar eventos
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        // Habilitar botón de jugar cuando hay username
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> {
            playButton.setDisable(newValue.trim().isEmpty());
        });
    }

    @FXML
    private void onLoginButtonClick(ActionEvent event) {
        logger.info("Botón de login presionado");

        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            statusLabel.setText("Por favor ingrese un nombre de usuario");
            return;
        }

        // Simular proceso de login
        statusLabel.setText("Iniciando sesión como: " + username);
        loginButton.setDisable(true);

        // Aquí iría la lógica de autenticación
        // Por ahora solo simulamos un login exitoso
        statusLabel.setText("Sesión iniciada correctamente");
        loginButton.setDisable(false);
    }

    @FXML
    private void onPlayButtonClick(ActionEvent event) {
        logger.info("Botón de jugar presionado");

        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            statusLabel.setText("Por favor ingrese un nombre de usuario");
            return;
        }

        // Simular descarga/verificación
        statusLabel.setText("Verificando archivos del juego...");
        downloadProgress.setVisible(true);
        downloadProgress.setProgress(0.0);

        // Aquí iría la lógica para verificar y descargar archivos
        // Por ahora solo simulamos el proceso
        simulateDownload();
    }

    private void simulateDownload() {
        // Simulación simple de descarga
        new Thread(() -> {
            try {
                for (int i = 0; i <= 100; i += 10) {
                    Thread.sleep(200);
                    final int progress = i;

                    javafx.application.Platform.runLater(() -> {
                        downloadProgress.setProgress(progress / 100.0);
                        statusLabel.setText("Descargando... " + progress + "%");
                    });
                }

                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText("¡Listo para jugar!");
                    downloadProgress.setVisible(false);

                    // Aquí iría la lógica para lanzar Minecraft
                    logger.info("Lanzando Minecraft para usuario: " + usernameField.getText());
                });

            } catch (InterruptedException e) {
                logger.error("Error en simulación de descarga", e);
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}
package org.klauncher.launcher.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import org.klauncher.launcher.models.LauncherConfig;
import org.klauncher.launcher.services.LauncherService;
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

    private LauncherService launcherService;
    private LauncherConfig config;
    private boolean userAuthenticated = false;

    @FXML
    public void initialize() {
        logger.info("Inicializando controlador principal");

        // Inicializar servicios
        this.launcherService = new LauncherService();
        this.config = LauncherConfig.load();

        // Configuración inicial de la UI
        setupInitialUI();

        // Configurar eventos
        setupEventHandlers();

        // Cargar configuración guardada
        loadSavedConfiguration();
    }

    private void setupInitialUI() {
        welcomeText.setText("Bienvenido a Karrito Launcher");
        statusLabel.setText("Listo para comenzar");
        downloadProgress.setVisible(false);
        playButton.setDisable(true);
    }

    private void setupEventHandlers() {
        // Habilitar botón de jugar cuando hay username y usuario autenticado
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> {
            updatePlayButtonState();
        });
    }

    private void loadSavedConfiguration() {
        // Cargar último username si está configurado para recordar
        if (config.isRememberCredentials() && !config.getLastUsername().isEmpty()) {
            usernameField.setText(config.getLastUsername());
            statusLabel.setText("Usuario recordado: " + config.getLastUsername());
        }
    }

    private void updatePlayButtonState() {
        String username = usernameField.getText().trim();
        playButton.setDisable(username.isEmpty() || !userAuthenticated);
    }

    @FXML
    private void onLoginButtonClick(ActionEvent event) {
        logger.info("Botón de login presionado");

        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            statusLabel.setText("Por favor ingrese un nombre de usuario");
            return;
        }

        // Deshabilitar UI durante autenticación
        setUIEnabled(false);
        statusLabel.setText("Autenticando usuario...");

        // Realizar autenticación en hilo separado
        new Thread(() -> {
            try {
                boolean success = launcherService.authenticateUser(username, "");

                Platform.runLater(() -> {
                    if (success) {
                        userAuthenticated = true;
                        statusLabel.setText("Autenticación exitosa para: " + username);

                        // Guardar configuración si está habilitado
                        if (config.isRememberCredentials()) {
                            config.setLastUsername(username);
                            config.save();
                        }
                    } else {
                        userAuthenticated = false;
                        statusLabel.setText("Error de autenticación. Verifique sus credenciales.");
                    }

                    updatePlayButtonState();
                    setUIEnabled(true);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    userAuthenticated = false;
                    statusLabel.setText("Error durante la autenticación: " + e.getMessage());
                    updatePlayButtonState();
                    setUIEnabled(true);
                });
            }
        }).start();
    }

    @FXML
    private void onPlayButtonClick(ActionEvent event) {
        logger.info("Botón de jugar presionado");

        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            statusLabel.setText("Por favor ingrese un nombre de usuario");
            return;
        }

        if (!userAuthenticated) {
            statusLabel.setText("Debe autenticarse antes de jugar");
            return;
        }

        // Deshabilitar UI durante descarga/lanzamiento
        setUIEnabled(false);
        downloadProgress.setVisible(true);
        downloadProgress.setProgress(0.0);

        // Iniciar descarga y lanzamiento en hilo separado
        new Thread(() -> {
            try {
                launcherService.downloadGameFiles(new LauncherService.ProgressCallback() {
                    @Override
                    public void onProgress(double progress, String message) {
                        Platform.runLater(() -> {
                            downloadProgress.setProgress(progress);
                            statusLabel.setText(message);
                        });
                    }

                    @Override
                    public void onComplete() {
                        Platform.runLater(() -> {
                            statusLabel.setText("Descarga completada. Iniciando juego...");

                            try {
                                launcherService.launchGame(username, config.getGameVersion());
                                statusLabel.setText("¡Juego iniciado exitosamente!");
                                downloadProgress.setVisible(false);
                            } catch (Exception e) {
                                logger.error("Error al lanzar el juego", e);
                                statusLabel.setText("Error al iniciar el juego: " + e.getMessage());
                            } finally {
                                setUIEnabled(true);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Platform.runLater(() -> {
                            statusLabel.setText("Error: " + error);
                            downloadProgress.setVisible(false);
                            setUIEnabled(true);
                        });
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    logger.error("Error durante la descarga", e);
                    statusLabel.setText("Error durante la descarga: " + e.getMessage());
                    downloadProgress.setVisible(false);
                    setUIEnabled(true);
                });
            }
        }).start();
    }

    private void setUIEnabled(boolean enabled) {
        usernameField.setDisable(!enabled);
        loginButton.setDisable(!enabled);
        if (enabled) {
            updatePlayButtonState();
        } else {
            playButton.setDisable(true);
        }
    }

    /**
     * Método llamado cuando la aplicación se cierra
     */
    public void shutdown() {
        logger.info("Cerrando controlador principal");
        if (launcherService != null) {
            launcherService.shutdown();
        }
    }
}
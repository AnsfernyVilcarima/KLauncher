package org.klauncher.launcher.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.klauncher.launcher.database.DatabaseManager;
import org.klauncher.launcher.models.config.AdvancedLauncherConfig;
import org.klauncher.launcher.models.entities.UserProfile;
import org.klauncher.launcher.services.LauncherService;
import org.klauncher.launcher.services.ProfileManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML
    private Label welcomeText;

    @FXML
    private ComboBox<UserProfile> profileSelector;

    @FXML
    private TextField usernameField;

    @FXML
    private Button loginButton;

    @FXML
    private Button playButton;

    @FXML
    private Button createProfileButton;

    @FXML
    private Button settingsButton;

    @FXML
    private ProgressBar downloadProgress;

    @FXML
    private Label statusLabel;

    @FXML
    private Label profileInfoLabel;

    // Servicios
    private LauncherService launcherService;
    private ProfileManagerService profileManagerService;
    private DatabaseManager databaseManager;
    private AdvancedLauncherConfig config;

    // Estado
    private UserProfile currentProfile;
    private boolean userAuthenticated = false;
    private ObservableList<UserProfile> profiles;

    @FXML
    public void initialize() {
        logger.info("Inicializando controlador principal mejorado");

        try {
            // Inicializar servicios
            initializeServices();

            // Configurar UI inicial
            setupInitialUI();

            // Configurar eventos
            setupEventHandlers();

            // Cargar datos
            loadInitialData();

            logger.info("Controlador principal inicializado exitosamente");
        } catch (Exception e) {
            logger.error("Error al inicializar controlador principal", e);
            showError("Error de Inicialización", "No se pudo inicializar el launcher: " + e.getMessage());
        }
    }

    /**
     * Inicializa todos los servicios necesarios
     */
    private void initializeServices() throws SQLException {
        logger.debug("Inicializando servicios...");

        // Inicializar base de datos
        databaseManager = DatabaseManager.getInstance();
        databaseManager.initialize();

        // Inicializar servicios
        profileManagerService = new ProfileManagerService();
        profileManagerService.initialize();

        launcherService = new LauncherService();

        // Cargar configuración avanzada
        config = AdvancedLauncherConfig.load();

        logger.debug("Servicios inicializados correctamente");
    }

    /**
     * Configura la UI inicial
     */
    private void setupInitialUI() {
        welcomeText.setText("Karrito Launcher v" + config.getApplication().getLauncherVersion());
        statusLabel.setText("Listo para comenzar");
        downloadProgress.setVisible(false);
        playButton.setDisable(true);

        // Configurar selector de perfiles
        profiles = FXCollections.observableArrayList();
        profileSelector.setItems(profiles);
        profileSelector.setCellFactory(param -> new ProfileListCell());
        profileSelector.setButtonCell(new ProfileListCell());

        // Configurar tooltips
        profileSelector.setTooltip(new Tooltip("Selecciona un perfil para jugar"));
        createProfileButton.setTooltip(new Tooltip("Crear un nuevo perfil"));
        settingsButton.setTooltip(new Tooltip("Configuración del launcher"));
    }

    /**
     * Configura los event handlers
     */
    private void setupEventHandlers() {
        // Listener para cambio de perfil
        profileSelector.valueProperty().addListener((observable, oldProfile, newProfile) -> {
            if (newProfile != null && !newProfile.equals(oldProfile)) {
                onProfileChanged(newProfile);
            }
        });

        // Listener para campo de usuario (solo para perfiles offline)
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> {
            updatePlayButtonState();
        });
    }

    /**
     * Carga los datos iniciales
     */
    private void loadInitialData() {
        CompletableFuture.runAsync(() -> {
            try {
                // Cargar perfiles
                var allProfiles = profileManagerService.getAllProfiles();

                Platform.runLater(() -> {
                    profiles.clear();
                    profiles.addAll(allProfiles);

                    // Seleccionar perfil activo
                    Optional<UserProfile> activeProfile = profileManagerService.getActiveProfile();
                    if (activeProfile.isPresent()) {
                        profileSelector.setValue(activeProfile.get());
                        onProfileChanged(activeProfile.get());
                    } else if (!profiles.isEmpty()) {
                        profileSelector.setValue(profiles.get(0));
                        onProfileChanged(profiles.get(0));
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    logger.error("Error al cargar datos iniciales", e);
                    statusLabel.setText("Error al cargar perfiles");
                });
            }
        });
    }

    /**
     * Maneja el cambio de perfil
     */
    private void onProfileChanged(UserProfile profile) {
        logger.debug("Cambiando a perfil: {}", profile.getName());

        currentProfile = profile;
        userAuthenticated = false;

        // Actualizar UI según el tipo de perfil
        updateUIForProfile(profile);

        // Establecer como perfil activo
        if (profile.getId() != null) {
            profileManagerService.setActiveProfile(profile.getId())
                    .thenRun(() -> {
                        Platform.runLater(() -> {
                            statusLabel.setText("Perfil activo: " + profile.getDisplayName());
                        });
                    })
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            logger.error("Error al establecer perfil activo", throwable);
                            statusLabel.setText("Error al cambiar perfil");
                        });
                        return null;
                    });
        }
    }

    /**
     * Actualiza la UI según el perfil seleccionado
     */
    private void updateUIForProfile(UserProfile profile) {
        if (profile.getProfileType() == UserProfile.ProfileType.OFFLINE) {
            // Perfil offline - mostrar campo de usuario
            usernameField.setVisible(true);
            usernameField.setDisable(false);
            usernameField.setText(profile.getMinecraftUsername() != null ?
                    profile.getMinecraftUsername() : "");
            loginButton.setText("Usar Offline");
        } else {
            // Perfil online - ocultar campo de usuario
            usernameField.setVisible(false);
            usernameField.setDisable(true);
            loginButton.setText("Iniciar Sesión");
        }

        // Actualizar información del perfil
        updateProfileInfo(profile);
        updatePlayButtonState();
    }

    /**
     * Actualiza la información mostrada del perfil
     */
    private void updateProfileInfo(UserProfile profile) {
        StringBuilder info = new StringBuilder();
        info.append("Perfil: ").append(profile.getDisplayName());
        info.append(" | Tipo: ").append(profile.getProfileType().getValue());
        info.append(" | Memoria: ").append(profile.getMinMemoryMb()).append("-").append(profile.getMaxMemoryMb()).append("MB");

        profileInfoLabel.setText(info.toString());
    }

    /**
     * Actualiza el estado del botón de jugar
     */
    private void updatePlayButtonState() {
        boolean canPlay = false;

        if (currentProfile != null) {
            if (currentProfile.getProfileType() == UserProfile.ProfileType.OFFLINE) {
                // Para offline, necesita username
                String username = usernameField.getText().trim();
                canPlay = !username.isEmpty() && userAuthenticated;
            } else {
                // Para online, necesita autenticación
                canPlay = userAuthenticated;
            }
        }

        playButton.setDisable(!canPlay);
    }

    @FXML
    private void onLoginButtonClick(ActionEvent event) {
        logger.info("Botón de login presionado para perfil: {}",
                currentProfile != null ? currentProfile.getName() : "ninguno");

        if (currentProfile == null) {
            statusLabel.setText("Selecciona un perfil primero");
            return;
        }

        String username = usernameField.getText().trim();

        if (currentProfile.getProfileType() == UserProfile.ProfileType.OFFLINE) {
            if (username.isEmpty()) {
                statusLabel.setText("Ingresa un nombre de usuario para modo offline");
                return;
            }
        }

        // Deshabilitar UI durante autenticación
        setUIEnabled(false);
        statusLabel.setText("Autenticando...");

        // Realizar autenticación en hilo separado
        CompletableFuture.runAsync(() -> {
            try {
                boolean success;

                if (currentProfile.getProfileType() == UserProfile.ProfileType.OFFLINE) {
                    // Autenticación offline (siempre exitosa si hay username)
                    success = !username.isEmpty();
                    Thread.sleep(500); // Simular delay mínimo
                } else {
                    // Autenticación online (Microsoft/Mojang)
                    success = launcherService.authenticateUser(username, "");
                }

                Platform.runLater(() -> {
                    if (success) {
                        userAuthenticated = true;

                        if (currentProfile.getProfileType() == UserProfile.ProfileType.OFFLINE) {
                            statusLabel.setText("Modo offline listo: " + username);

                            // Actualizar username en el perfil si cambió
                            if (!username.equals(currentProfile.getMinecraftUsername())) {
                                currentProfile.setMinecraftUsername(username);
                                saveProfileAsync(currentProfile);
                            }
                        } else {
                            statusLabel.setText("Autenticación exitosa: " + currentProfile.getDisplayName());
                        }
                    } else {
                        userAuthenticated = false;
                        statusLabel.setText("Error de autenticación");
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
                    logger.error("Error en autenticación", e);
                });
            }
        });
    }

    @FXML
    private void onPlayButtonClick(ActionEvent event) {
        logger.info("Botón de jugar presionado para perfil: {}", currentProfile.getName());

        if (currentProfile == null || !userAuthenticated) {
            statusLabel.setText("Autentícate primero");
            return;
        }

        // Deshabilitar UI durante lanzamiento
        setUIEnabled(false);
        downloadProgress.setVisible(true);
        downloadProgress.setProgress(0.0);

        // Obtener username efectivo
        String effectiveUsername = currentProfile.getProfileType() == UserProfile.ProfileType.OFFLINE ?
                usernameField.getText().trim() : currentProfile.getMinecraftUsername();

        // Iniciar proceso de lanzamiento
        CompletableFuture.runAsync(() -> {
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
                            statusLabel.setText("Iniciando Minecraft...");

                            try {
                                launcherService.launchGame(effectiveUsername, config.getGame().getDefaultVersionType());

                                statusLabel.setText("¡Minecraft iniciado!");
                                downloadProgress.setVisible(false);

                                // Si está configurado, cerrar launcher
                                if (config.getApplication().isCloseLauncherOnGameStart()) {
                                    Platform.runLater(() -> {
                                        try {
                                            Thread.sleep(2000);
                                            Platform.exit();
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                    });
                                }

                            } catch (Exception e) {
                                logger.error("Error al lanzar el juego", e);
                                statusLabel.setText("Error al iniciar Minecraft: " + e.getMessage());
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
                    logger.error("Error durante el lanzamiento", e);
                    statusLabel.setText("Error: " + e.getMessage());
                    downloadProgress.setVisible(false);
                    setUIEnabled(true);
                });
            }
        });
    }

    @FXML
    private void onCreateProfileButtonClick(ActionEvent event) {
        logger.info("Creando nuevo perfil");

        // Por ahora, crear un perfil simple
        // TODO: Abrir diálogo de creación de perfil
        createSimpleProfile();
    }

    @FXML
    private void onSettingsButtonClick(ActionEvent event) {
        logger.info("Abriendo configuración");

        // TODO: Abrir ventana de configuración
        statusLabel.setText("Configuración próximamente...");
    }

    /**
     * Crea un perfil simple para demostración
     */
    private void createSimpleProfile() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nuevo Perfil");
        dialog.setHeaderText("Crear Nuevo Perfil");
        dialog.setContentText("Nombre del perfil:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                profileManagerService.createProfile(name.trim(), name.trim(), UserProfile.ProfileType.OFFLINE)
                        .thenAccept(profile -> {
                            Platform.runLater(() -> {
                                profiles.add(profile);
                                profileSelector.setValue(profile);
                                statusLabel.setText("Perfil '" + profile.getDisplayName() + "' creado");
                            });
                        })
                        .exceptionally(throwable -> {
                            Platform.runLater(() -> {
                                logger.error("Error al crear perfil", throwable);
                                showError("Error", "No se pudo crear el perfil: " + throwable.getMessage());
                            });
                            return null;
                        });
            }
        });
    }

    /**
     * Guarda un perfil de forma asíncrona
     */
    private void saveProfileAsync(UserProfile profile) {
        profileManagerService.updateProfile(profile)
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        logger.error("Error al guardar perfil", throwable);
                    });
                    return null;
                });
    }

    /**
     * Habilita/deshabilita la UI
     */
    private void setUIEnabled(boolean enabled) {
        profileSelector.setDisable(!enabled);
        usernameField.setDisable(!enabled || currentProfile == null ||
                currentProfile.getProfileType() != UserProfile.ProfileType.OFFLINE);
        loginButton.setDisable(!enabled);
        createProfileButton.setDisable(!enabled);
        settingsButton.setDisable(!enabled);

        if (enabled) {
            updatePlayButtonState();
        } else {
            playButton.setDisable(true);
        }
    }

    /**
     * Muestra un error en diálogo
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Método llamado cuando la aplicación se cierra
     */
    public void shutdown() {
        logger.info("Cerrando controlador principal");

        try {
            if (profileManagerService != null) {
                profileManagerService.shutdown();
            }
            if (launcherService != null) {
                launcherService.shutdown();
            }
            if (databaseManager != null) {
                databaseManager.close();
            }
        } catch (Exception e) {
            logger.error("Error durante el cierre", e);
        }
    }

    /**
     * Cell factory para mostrar perfiles en el ComboBox
     */
    private static class ProfileListCell extends ListCell<UserProfile> {
        @Override
        protected void updateItem(UserProfile profile, boolean empty) {
            super.updateItem(profile, empty);

            if (empty || profile == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(profile.getDisplayName() + " (" + profile.getProfileType().getValue() + ")");
            }
        }
    }
}
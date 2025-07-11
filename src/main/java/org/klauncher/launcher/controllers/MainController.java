package org.klauncher.launcher.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.klauncher.launcher.database.DatabaseManager;
import org.klauncher.launcher.models.config.AdvancedLauncherConfig;
import org.klauncher.launcher.models.entities.UserProfile;
import org.klauncher.launcher.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML private Label welcomeText;
    @FXML private ComboBox<UserProfile> profileSelector;
    @FXML private TextField usernameField;
    @FXML private Button loginButton;
    @FXML private Button playButton;
    @FXML private Button createProfileButton;
    @FXML private Button settingsButton;
    @FXML private ProgressBar downloadProgress;
    @FXML private Label statusLabel;
    @FXML private Label profileInfoLabel;

    // Nuevos controles de personalización
    @FXML private MenuButton themeMenuButton;
    @FXML private MenuButton backgroundMenuButton;

    // Servicios principales
    private LauncherService launcherService;
    private ProfileManagerService profileManagerService;
    private DatabaseManager databaseManager;
    private AdvancedLauncherConfig config;

    // Servicios de personalización
    private ThemeManagerService themeManager;
    private BackgroundManagerService backgroundManager;
    private CustomizationService customizationService;

    // Estado
    private UserProfile currentProfile;
    private boolean userAuthenticated = false;
    private ObservableList<UserProfile> profiles;

    @FXML
    public void initialize() {
        logger.info("Inicializando controlador principal con personalización completa");

        try {
            // Inicializar servicios core
            initializeCoreServices();

            // Inicializar servicios de personalización
            initializeCustomizationServices();

            // Configurar UI inicial
            setupInitialUI();

            // Configurar controles de personalización
            setupCustomizationControls();

            // Configurar eventos
            setupEventHandlers();

            // Cargar datos
            loadInitialData();

            logger.info("Controlador principal con personalización inicializado exitosamente");
        } catch (Exception e) {
            logger.error("Error al inicializar controlador principal", e);
            showError("Error de Inicialización", "No se pudo inicializar el launcher: " + e.getMessage());
        }
    }

    /**
     * Inicializa servicios principales
     */
    private void initializeCoreServices() throws SQLException {
        logger.debug("Inicializando servicios principales...");

        // Base de datos
        databaseManager = DatabaseManager.getInstance();
        databaseManager.initialize();

        // Servicios básicos
        profileManagerService = new ProfileManagerService();
        profileManagerService.initialize();

        launcherService = new LauncherService();

        // Configuración
        config = AdvancedLauncherConfig.load();

        logger.debug("Servicios principales inicializados");
    }

    /**
     * Inicializa servicios de personalización
     */
    private void initializeCustomizationServices() {
        logger.debug("Inicializando servicios de personalización...");

        // Gestión de temas mejorada
        themeManager = ThemeManagerService.getInstance();
        themeManager.initialize(config);

        // Gestión de fondos
        backgroundManager = BackgroundManagerService.getInstance();
        backgroundManager.initialize(config);

        // Servicio de personalización integrado
        customizationService = CustomizationService.getInstance();
        customizationService.initialize(config);

        logger.debug("Servicios de personalización inicializados");
    }

    /**
     * Configura la UI inicial
     */
    private void setupInitialUI() {
        welcomeText.setText("KARRITO LAUNCHER v" + config.getApplication().getLauncherVersion());
        statusLabel.setText("Listo para la aventura");
        downloadProgress.setVisible(false);
        playButton.setDisable(true);

        // Configurar selector de perfiles
        profiles = FXCollections.observableArrayList();
        profileSelector.setItems(profiles);
        profileSelector.setCellFactory(param -> new ProfileListCell());
        profileSelector.setButtonCell(new ProfileListCell());

        // Configurar tooltips épicos
        setupTooltips();

        // Aplicar estilos CSS iniciales
        applyInitialStyling();
    }

    /**
     * Configura controles de personalización
     */
    private void setupCustomizationControls() {
        // Si los controles existen en el FXML, configurarlos
        if (themeMenuButton != null) {
            setupThemeMenu();
        }

        if (backgroundMenuButton != null) {
            setupBackgroundMenu();
        }

        // Agregar funcionalidad avanzada al botón de configuración
        setupAdvancedSettingsButton();
    }

    /**
     * Configura menú de temas
     */
    private void setupThemeMenu() {
        themeMenuButton.setText("🎨 Temas");

        for (ThemeManagerService.Theme theme : ThemeManagerService.Theme.values()) {
            MenuItem themeItem = new MenuItem(theme.getDisplayName());
            themeItem.setOnAction(e -> {
                themeManager.setTheme(theme);
                showQuickNotification("Tema cambiado a: " + theme.getDisplayName());
            });
            themeMenuButton.getItems().add(themeItem);
        }

        // Separador
        themeMenuButton.getItems().add(new SeparatorMenuItem());

        // Tema aleatorio
        MenuItem randomTheme = new MenuItem("🎲 Aleatorio");
        randomTheme.setOnAction(e -> {
            themeManager.setRandomTheme();
            showQuickNotification("¡Tema aleatorio aplicado!");
        });
        themeMenuButton.getItems().add(randomTheme);
    }

    /**
     * Configura menú de fondos
     */
    private void setupBackgroundMenu() {
        backgroundMenuButton.setText("🌌 Fondos");

        for (BackgroundManagerService.BackgroundType bg : BackgroundManagerService.BackgroundType.values()) {
            MenuItem bgItem = new MenuItem(bg.getDisplayName());
            bgItem.setOnAction(e -> {
                backgroundManager.setBackground(bg);
                showQuickNotification("Fondo cambiado a: " + bg.getDisplayName());
            });
            backgroundMenuButton.getItems().add(bgItem);
        }

        // Separador
        backgroundMenuButton.getItems().add(new SeparatorMenuItem());

        // Fondo aleatorio
        MenuItem randomBg = new MenuItem("🔀 Aleatorio");
        randomBg.setOnAction(e -> {
            backgroundManager.setRandomBackground();
            showQuickNotification("¡Fondo aleatorio aplicado!");
        });
        backgroundMenuButton.getItems().add(randomBg);
    }

    /**
     * Configura botón de configuración avanzada
     */
    private void setupAdvancedSettingsButton() {
        // Crear menú contextual para el botón de configuración
        ContextMenu settingsMenu = new ContextMenu();

        // Personalización completa
        MenuItem customization = new MenuItem("🎨 Personalización Completa");
        customization.setOnAction(e -> openCustomizationPanel());

        // Cambio rápido de tema
        MenuItem quickTheme = new MenuItem("🌓 Cambiar Tema");
        quickTheme.setOnAction(e -> themeManager.cycleTheme());

        // Cambio rápido de fondo
        MenuItem quickBackground = new MenuItem("🌌 Cambiar Fondo");
        quickBackground.setOnAction(e -> backgroundManager.setRandomBackground());

        // Toggle animaciones
        MenuItem toggleAnimations = new MenuItem("✨ Toggle Animaciones");
        toggleAnimations.setOnAction(e -> {
            customizationService.toggleAnimations();
            showQuickNotification("Animaciones " + (config.getUi().isEnableAnimations() ? "activadas" : "desactivadas"));
        });

        // Toggle efecto cristal
        MenuItem toggleGlass = new MenuItem("💎 Toggle Efecto Cristal");
        toggleGlass.setOnAction(e -> {
            customizationService.toggleGlassEffect();
            showQuickNotification("Efecto cristal alternado");
        });

        // Separador
        SeparatorMenuItem separator = new SeparatorMenuItem();

        // Configuración tradicional
        MenuItem traditionalSettings = new MenuItem("⚙️ Configuración");
        traditionalSettings.setOnAction(e -> showTraditionalSettings());

        settingsMenu.getItems().addAll(
                customization, quickTheme, quickBackground,
                separator, toggleAnimations, toggleGlass,
                new SeparatorMenuItem(), traditionalSettings
        );

        // Configurar clic derecho
        settingsButton.setContextMenu(settingsMenu);

        // Clic izquierdo abre personalización
        settingsButton.setOnAction(e -> openCustomizationPanel());
    }

    /**
     * Configura tooltips épicos
     */
    private void setupTooltips() {
        profileSelector.setTooltip(new Tooltip("🎭 Selecciona tu perfil de aventurero"));
        createProfileButton.setTooltip(new Tooltip("➕ Crear nuevo perfil épico"));
        settingsButton.setTooltip(new Tooltip("🎨 Personalización total del launcher"));
        playButton.setTooltip(new Tooltip("🚀 ¡Iniciar la aventura!"));
        loginButton.setTooltip(new Tooltip("🔐 Autenticación de guerrero"));
    }

    /**
     * Aplica estilos CSS iniciales
     */
    private void applyInitialStyling() {
        // Aplicar clases CSS épicas a elementos importantes
        welcomeText.getStyleClass().add("epic-title");
        playButton.getStyleClass().add("epic-play-button");
        settingsButton.getStyleClass().add("settings-glow");

        // Registrar escena en servicios de personalización
        Platform.runLater(() -> {
            Scene scene = welcomeText.getScene();
            if (scene != null) {
                themeManager.registerScene(scene);
                customizationService.registerScene(scene);

                // Registrar región de fondo
                if (scene.getRoot() instanceof javafx.scene.layout.Region) {
                    backgroundManager.registerRegion((javafx.scene.layout.Region) scene.getRoot());
                }
            }
        });
    }

    /**
     * Configura event handlers
     */
    private void setupEventHandlers() {
        // Listener para cambio de perfil
        profileSelector.valueProperty().addListener((observable, oldProfile, newProfile) -> {
            if (newProfile != null && !newProfile.equals(oldProfile)) {
                onProfileChanged(newProfile);
            }
        });

        // Listener para campo de usuario
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> {
            updatePlayButtonState();
        });

        // Keyboard shortcuts épicos
        Platform.runLater(() -> {
            Scene scene = welcomeText.getScene();
            if (scene != null) {
                scene.setOnKeyPressed(event -> {
                    switch (event.getCode()) {
                        case F1 -> openCustomizationPanel();
                        case F2 -> themeManager.cycleTheme();
                        case F3 -> backgroundManager.setRandomBackground();
                        case F5 -> refreshLauncher();
                        case ESCAPE -> {
                            if (event.isControlDown()) {
                                Platform.exit();
                            }
                        }
                    }
                });
            }
        });
    }

    /**
     * Carga datos iniciales
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

                    // Mostrar mensaje de bienvenida épico
                    showQuickNotification("¡Bienvenido a Karrito Launcher! Presiona F1 para personalizar");
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
     * Abre panel de personalización completa
     */
    @FXML
    private void openCustomizationPanel() {
        try {
            logger.info("Abriendo panel de personalización épico");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/customization-panel.fxml"));
            Parent root = loader.load();

            Stage customizationStage = new Stage();
            customizationStage.setTitle("🎨 Personalización Épica - Karrito Launcher");
            customizationStage.initModality(Modality.APPLICATION_MODAL);
            customizationStage.initStyle(StageStyle.DECORATED);
            customizationStage.setResizable(true);

            Scene scene = new Scene(root, 600, 700);

            // Aplicar tema actual al panel
            themeManager.registerScene(scene);
            customizationService.registerScene(scene);

            customizationStage.setScene(scene);
            customizationStage.show();

            logger.info("Panel de personalización abierto exitosamente");

        } catch (IOException e) {
            logger.error("Error al abrir panel de personalización", e);
            showError("Error", "No se pudo abrir el panel de personalización: " + e.getMessage());
        }
    }

    /**
     * Muestra configuración tradicional
     */
    private void showTraditionalSettings() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información del Sistema");
        alert.setHeaderText("Estado del Launcher");

        StringBuilder info = new StringBuilder();
        info.append("=== KARRITO LAUNCHER ===\n");
        info.append("Versión: ").append(config.getApplication().getLauncherVersion()).append("\n\n");
        info.append("=== PERSONALIZACIÓN ===\n");
        info.append(themeManager.getThemeInfo()).append("\n");
        info.append(backgroundManager.getBackgroundInfo()).append("\n");
        info.append(customizationService.getCustomizationInfo()).append("\n");
        info.append("\n=== ATAJOS DE TECLADO ===\n");
        info.append("F1: Personalización completa\n");
        info.append("F2: Cambiar tema\n");
        info.append("F3: Cambiar fondo\n");
        info.append("F5: Refrescar\n");
        info.append("Ctrl+Esc: Salir\n");

        alert.setContentText(info.toString());
        alert.showAndWait();
    }

    /**
     * Refresca el launcher
     */
    private void refreshLauncher() {
        logger.info("Refrescando launcher...");

        // Refresar temas
        themeManager.refreshTheme();

        // Recargar perfiles
        loadInitialData();

        showQuickNotification("¡Launcher refrescado!");
    }

    /**
     * Muestra notificación rápida
     */
    private void showQuickNotification(String message) {
        String originalText = statusLabel.getText();
        statusLabel.setText("✨ " + message);
        statusLabel.getStyleClass().add("notification-glow");

        // Restaurar después de 3 segundos
        Platform.runLater(() -> {
            CompletableFuture.delayedExecutor(3, java.util.concurrent.TimeUnit.SECONDS)
                    .execute(() -> Platform.runLater(() -> {
                        statusLabel.setText(originalText);
                        statusLabel.getStyleClass().remove("notification-glow");
                    }));
        });
    }

    // ===============================================
    // MÉTODOS ORIGINALES MEJORADOS
    // ===============================================

    private void onProfileChanged(UserProfile profile) {
        logger.debug("Cambiando a perfil épico: {}", profile.getName());

        currentProfile = profile;
        userAuthenticated = false;

        updateUIForProfile(profile);

        if (profile.getId() != null) {
            profileManagerService.setActiveProfile(profile.getId())
                    .thenRun(() -> Platform.runLater(() -> {
                        showQuickNotification("Perfil activo: " + profile.getDisplayName() + " 🎭");
                    }))
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            logger.error("Error al establecer perfil activo", throwable);
                            statusLabel.setText("Error al cambiar perfil");
                        });
                        return null;
                    });
        }
    }

    private void updateUIForProfile(UserProfile profile) {
        if (profile.getProfileType() == UserProfile.ProfileType.OFFLINE) {
            usernameField.setVisible(true);
            usernameField.setDisable(false);
            usernameField.setText(profile.getMinecraftUsername() != null ?
                    profile.getMinecraftUsername() : "");
            loginButton.setText("🔓 Modo Offline");
        } else {
            usernameField.setVisible(false);
            usernameField.setDisable(true);
            loginButton.setText("🔐 Iniciar Sesión");
        }

        updateProfileInfo(profile);
        updatePlayButtonState();
    }

    private void updateProfileInfo(UserProfile profile) {
        StringBuilder info = new StringBuilder();
        info.append("🎭 ").append(profile.getDisplayName());
        info.append(" | 🔧 ").append(profile.getProfileType().getValue());
        info.append(" | 🧠 ").append(profile.getMinMemoryMb()).append("-").append(profile.getMaxMemoryMb()).append("MB");

        profileInfoLabel.setText(info.toString());
    }

    private void updatePlayButtonState() {
        boolean canPlay = false;

        if (currentProfile != null) {
            if (currentProfile.getProfileType() == UserProfile.ProfileType.OFFLINE) {
                String username = usernameField.getText().trim();
                canPlay = !username.isEmpty() && userAuthenticated;
            } else {
                canPlay = userAuthenticated;
            }
        }

        playButton.setDisable(!canPlay);

        // Efecto visual dinámico
        if (canPlay) {
            playButton.getStyleClass().add("ready-to-play");
        } else {
            playButton.getStyleClass().remove("ready-to-play");
        }
    }

    @FXML
    private void onLoginButtonClick(ActionEvent event) {
        logger.info("🔐 Autenticación iniciada para perfil: {}",
                currentProfile != null ? currentProfile.getName() : "ninguno");

        if (currentProfile == null) {
            showQuickNotification("⚠️ Selecciona un perfil primero");
            return;
        }

        String username = usernameField.getText().trim();

        if (currentProfile.getProfileType() == UserProfile.ProfileType.OFFLINE) {
            if (username.isEmpty()) {
                showQuickNotification("⚠️ Ingresa tu nombre de guerrero");
                return;
            }
        }

        setUIEnabled(false);
        statusLabel.setText("🔐 Autenticando guerrero...");

        CompletableFuture.runAsync(() -> {
            try {
                boolean success;

                if (currentProfile.getProfileType() == UserProfile.ProfileType.OFFLINE) {
                    success = !username.isEmpty();
                    Thread.sleep(800); // Efecto dramático
                } else {
                    success = launcherService.authenticateUser(username, "");
                }

                Platform.runLater(() -> {
                    if (success) {
                        userAuthenticated = true;

                        if (currentProfile.getProfileType() == UserProfile.ProfileType.OFFLINE) {
                            showQuickNotification("✅ Guerrero " + username + " listo para la batalla!");

                            if (!username.equals(currentProfile.getMinecraftUsername())) {
                                currentProfile.setMinecraftUsername(username);
                                saveProfileAsync(currentProfile);
                            }
                        } else {
                            showQuickNotification("✅ Autenticación épica completada!");
                        }
                    } else {
                        userAuthenticated = false;
                        showQuickNotification("❌ Error de autenticación");
                    }

                    updatePlayButtonState();
                    setUIEnabled(true);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    userAuthenticated = false;
                    showQuickNotification("💥 Error durante la autenticación");
                    updatePlayButtonState();
                    setUIEnabled(true);
                    logger.error("Error en autenticación", e);
                });
            }
        });
    }

    @FXML
    private void onPlayButtonClick(ActionEvent event) {
        logger.info("🚀 ¡INICIANDO AVENTURA ÉPICA!");

        if (currentProfile == null || !userAuthenticated) {
            showQuickNotification("⚠️ Autentícate primero, guerrero");
            return;
        }

        setUIEnabled(false);
        downloadProgress.setVisible(true);
        downloadProgress.setProgress(0.0);

        String effectiveUsername = currentProfile.getProfileType() == UserProfile.ProfileType.OFFLINE ?
                usernameField.getText().trim() : currentProfile.getMinecraftUsername();

        CompletableFuture.runAsync(() -> {
            try {
                launcherService.downloadGameFiles(new LauncherService.ProgressCallback() {
                    @Override
                    public void onProgress(double progress, String message) {
                        Platform.runLater(() -> {
                            downloadProgress.setProgress(progress);
                            statusLabel.setText("🔄 " + message);
                        });
                    }

                    @Override
                    public void onComplete() {
                        Platform.runLater(() -> {
                            statusLabel.setText("🚀 Iniciando aventura épica...");

                            try {
                                launcherService.launchGame(effectiveUsername, config.getGame().getDefaultVersionType());

                                showQuickNotification("🎮 ¡Aventura iniciada! ¡Que tengas épicas batallas!");
                                downloadProgress.setVisible(false);

                                if (config.getApplication().isCloseLauncherOnGameStart()) {
                                    Platform.runLater(() -> {
                                        try {
                                            Thread.sleep(3000);
                                            Platform.exit();
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                    });
                                }

                            } catch (Exception e) {
                                logger.error("Error al lanzar el juego", e);
                                statusLabel.setText("💥 Error al iniciar aventura");
                            } finally {
                                setUIEnabled(true);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Platform.runLater(() -> {
                            statusLabel.setText("💥 Error: " + error);
                            downloadProgress.setVisible(false);
                            setUIEnabled(true);
                        });
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    logger.error("Error durante el lanzamiento", e);
                    statusLabel.setText("💥 Error: " + e.getMessage());
                    downloadProgress.setVisible(false);
                    setUIEnabled(true);
                });
            }
        });
    }

    @FXML
    private void onCreateProfileButtonClick(ActionEvent event) {
        logger.info("➕ Creando nuevo perfil épico");
        createSimpleProfile();
    }

    @FXML
    private void onSettingsButtonClick(ActionEvent event) {
        openCustomizationPanel();
    }

    // ===============================================
    // MÉTODOS DE UTILIDAD
    // ===============================================

    private void createSimpleProfile() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("🎭 Nuevo Perfil Épico");
        dialog.setHeaderText("Crear Nuevo Guerrero");
        dialog.setContentText("Nombre del perfil:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                profileManagerService.createProfile(name.trim(), name.trim(), UserProfile.ProfileType.OFFLINE)
                        .thenAccept(profile -> {
                            Platform.runLater(() -> {
                                profiles.add(profile);
                                profileSelector.setValue(profile);
                                showQuickNotification("🎭 Guerrero '" + profile.getDisplayName() + "' creado");
                            });
                        })
                        .exceptionally(throwable -> {
                            Platform.runLater(() -> {
                                logger.error("Error al crear perfil", throwable);
                                showError("Error", "No se pudo crear el guerrero: " + throwable.getMessage());
                            });
                            return null;
                        });
            }
        });
    }

    private void saveProfileAsync(UserProfile profile) {
        profileManagerService.updateProfile(profile)
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        logger.error("Error al guardar perfil", throwable);
                    });
                    return null;
                });
    }

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
        logger.info("🔥 Cerrando launcher épico...");

        try {
            if (customizationService != null) {
                customizationService.shutdown();
            }
            if (themeManager != null) {
                themeManager.shutdown();
            }
            if (backgroundManager != null) {
                backgroundManager.shutdown();
            }
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
            logger.error("Error durante el cierre épico", e);
        }

        logger.info("🌟 ¡Hasta la próxima aventura!");
    }

    /**
     * Cell factory para mostrar perfiles épicos
     */
    private static class ProfileListCell extends ListCell<UserProfile> {
        @Override
        protected void updateItem(UserProfile profile, boolean empty) {
            super.updateItem(profile, empty);

            if (empty || profile == null) {
                setText(null);
                setGraphic(null);
            } else {
                String emoji = switch (profile.getProfileType()) {
                    case OFFLINE -> "🎮";
                    case MICROSOFT -> "🏢";
                    case MOJANG -> "🟢";
                };
                setText(emoji + " " + profile.getDisplayName() + " (" + profile.getProfileType().getValue() + ")");
            }
        }
    }
}
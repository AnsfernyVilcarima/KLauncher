package org.klauncher.launcher;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.klauncher.launcher.controllers.MainController;
import org.klauncher.launcher.database.DatabaseManager;
import org.klauncher.launcher.models.config.AdvancedLauncherConfig;
import org.klauncher.launcher.services.ThemeManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LauncherApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(LauncherApplication.class);

    private MainController mainController;
    private AdvancedLauncherConfig config;
    private Stage primaryStage;
    private ThemeManagerService themeManager;

    @Override
    public void init() throws Exception {
        logger.info("Inicializando KarritoLauncher...");

        try {
            // Cargar configuración
            config = AdvancedLauncherConfig.load();
            logger.info("Configuración cargada exitosamente");

            // Test de conexión a base de datos
            DatabaseManager dbManager = DatabaseManager.getInstance();
            if (!dbManager.testConnection()) {
                logger.warn("Conexión de base de datos no disponible, continuando con inicialización");
            }

        } catch (Exception e) {
            logger.error("Error durante la inicialización", e);
            throw e;
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        this.primaryStage = stage;

        logger.info("Iniciando interfaz gráfica...");

        try {
            // Cargar FXML
            FXMLLoader fxmlLoader = new FXMLLoader(LauncherApplication.class.getResource("/fxml/main-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());

            // Obtener referencia al controller
            mainController = fxmlLoader.getController();

            // Inicializar gestión de temas
            initializeThemeManager(scene);

            // Configurar ventana
            setupWindow(stage, scene);

            // Configurar cierre de aplicación
            setupShutdownHooks(stage);

            // Mostrar ventana
            stage.show();

            logger.info("Aplicación iniciada correctamente");

        } catch (Exception e) {
            logger.error("Error al iniciar la aplicación", e);
            showCriticalError("Error de Inicio",
                    "No se pudo iniciar KarritoLauncher:\n" + e.getMessage());
            Platform.exit();
            throw e;
        }
    }

    /**
     * Inicializa el gestor de temas
     */
    private void initializeThemeManager(Scene scene) {
        try {
            themeManager = ThemeManagerService.getInstance();
            themeManager.initialize(config);
            themeManager.registerScene(scene);

            logger.info("Gestor de temas inicializado: {}",
                    themeManager.getCurrentTheme().getDisplayName());
        } catch (Exception e) {
            logger.error("Error al inicializar gestor de temas", e);
            // Continuar sin temas si hay error
        }
    }

    /**
     * Configura la ventana principal
     */
    private void setupWindow(Stage stage, Scene scene) {
        stage.setTitle("Karrito Launcher v" + config.getApplication().getLauncherVersion());
        stage.setScene(scene);

        // Configurar tamaño de ventana
        if (config.getUi().isRememberWindowSize()) {
            stage.setWidth(config.getUi().getWindowWidth());
            stage.setHeight(config.getUi().getWindowHeight());
        } else {
            stage.setWidth(900);
            stage.setHeight(650);
        }

        // Configurar redimensionable
        stage.setResizable(true);

        // Tamaño mínimo
        stage.setMinWidth(600);
        stage.setMinHeight(400);

        // Listeners para guardar tamaño de ventana
        if (config.getUi().isRememberWindowSize()) {
            stage.widthProperty().addListener((obs, oldVal, newVal) -> {
                config.getUi().setWindowWidth(newVal.intValue());
            });

            stage.heightProperty().addListener((obs, oldVal, newVal) -> {
                config.getUi().setWindowHeight(newVal.intValue());
            });
        }

        logger.debug("Ventana configurada: {}x{}", stage.getWidth(), stage.getHeight());
    }

    /**
     * Configura los hooks de cierre
     */
    private void setupShutdownHooks(Stage stage) {
        // Handler para cerrar ventana
        stage.setOnCloseRequest(event -> {
            logger.info("Cerrando aplicación...");

            try {
                // Guardar configuración antes de cerrar
                saveConfiguration();

                // Ejecutar shutdown del controller
                if (mainController != null) {
                    mainController.shutdown();
                }

                // Cerrar gestión de temas
                if (themeManager != null) {
                    themeManager.shutdown();
                }

                // Cerrar base de datos
                DatabaseManager.getInstance().close();

                logger.info("Aplicación cerrada correctamente");

            } catch (Exception e) {
                logger.error("Error durante el cierre", e);
            }
        });

        // Shutdown hook de la JVM
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (mainController != null) {
                    mainController.shutdown();
                }
                DatabaseManager.getInstance().close();
            } catch (Exception e) {
                logger.error("Error en shutdown hook", e);
            }
        }));
    }

    /**
     * Guarda la configuración actual
     */
    private void saveConfiguration() {
        try {
            if (config != null) {
                // Actualizar timestamp
                config.getMetadata().setUpdatedAt(java.time.LocalDateTime.now());

                // Guardar
                config.save();
                logger.debug("Configuración guardada al cerrar");
            }
        } catch (Exception e) {
            logger.error("Error al guardar configuración", e);
        }
    }

    /**
     * Muestra un error crítico
     */
    private void showCriticalError(String title, String message) {
        try {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(title);
                alert.setHeaderText("Error Crítico");
                alert.setContentText(message);
                alert.showAndWait();
            });
        } catch (Exception e) {
            logger.error("No se pudo mostrar diálogo de error", e);
            System.err.println("Error crítico: " + message);
        }
    }

    @Override
    public void stop() throws Exception {
        logger.info("Ejecutando stop() de la aplicación");

        try {
            // Ejecutar limpieza final
            if (mainController != null) {
                mainController.shutdown();
            }

            // Cerrar conexiones
            DatabaseManager.getInstance().close();

            super.stop();

        } catch (Exception e) {
            logger.error("Error en stop()", e);
            throw e;
        }
    }

    /**
     * Punto de entrada principal
     */
    public static void main(String[] args) {
        // Configurar logging
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

        try {
            launch(args);
        } catch (Exception e) {
            Logger logger = LoggerFactory.getLogger(LauncherApplication.class);
            logger.error("Error fatal al iniciar la aplicación", e);
            System.exit(1);
        }
    }
}
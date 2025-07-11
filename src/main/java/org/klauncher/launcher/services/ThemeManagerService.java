package org.klauncher.launcher.services;

import javafx.scene.Scene;
import org.klauncher.launcher.models.config.AdvancedLauncherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para gestión de temas de la interfaz
 */
public class ThemeManagerService {
    private static final Logger logger = LoggerFactory.getLogger(ThemeManagerService.class);

    public enum Theme {
        DARK("dark", "/css/dark-theme.css", "Tema Oscuro"),
        LIGHT("light", "/css/light-theme.css", "Tema Claro");

        private final String id;
        private final String cssPath;
        private final String displayName;

        Theme(String id, String cssPath, String displayName) {
            this.id = id;
            this.cssPath = cssPath;
            this.displayName = displayName;
        }

        public String getId() { return id; }
        public String getCssPath() { return cssPath; }
        public String getDisplayName() { return displayName; }

        public static Theme fromId(String id) {
            for (Theme theme : values()) {
                if (theme.id.equals(id)) {
                    return theme;
                }
            }
            return DARK; // Default
        }
    }

    private static ThemeManagerService instance;
    private AdvancedLauncherConfig config;
    private Theme currentTheme;
    private final List<Scene> registeredScenes;

    private ThemeManagerService() {
        this.registeredScenes = new ArrayList<>();
    }

    public static synchronized ThemeManagerService getInstance() {
        if (instance == null) {
            instance = new ThemeManagerService();
        }
        return instance;
    }

    /**
     * Inicializa el servicio de temas
     */
    public void initialize(AdvancedLauncherConfig config) {
        this.config = config;

        // Cargar tema desde configuración
        String themeId = config.getUi().getTheme();
        currentTheme = Theme.fromId(themeId);

        logger.info("Servicio de temas inicializado con tema: {}", currentTheme.getDisplayName());
    }

    /**
     * Registra una escena para aplicar temas
     */
    public void registerScene(Scene scene) {
        if (scene == null) {
            logger.warn("Intentando registrar escena nula");
            return;
        }

        registeredScenes.add(scene);
        applyThemeToScene(scene, currentTheme);

        logger.debug("Escena registrada para gestión de temas");
    }

    /**
     * Desregistra una escena
     */
    public void unregisterScene(Scene scene) {
        registeredScenes.remove(scene);
        logger.debug("Escena desregistrada de gestión de temas");
    }

    /**
     * Cambia el tema actual
     */
    public void setTheme(Theme theme) {
        if (theme == null) {
            logger.warn("Intentando establecer tema nulo");
            return;
        }

        if (theme == currentTheme) {
            logger.debug("El tema {} ya está activo", theme.getDisplayName());
            return;
        }

        logger.info("Cambiando tema de {} a {}",
                currentTheme.getDisplayName(), theme.getDisplayName());

        Theme previousTheme = currentTheme;
        currentTheme = theme;

        try {
            // Aplicar tema a todas las escenas registradas
            for (Scene scene : registeredScenes) {
                applyThemeToScene(scene, theme);
            }

            // Guardar en configuración
            if (config != null) {
                config.getUi().setTheme(theme.getId());
                config.save();
            }

            logger.info("Tema cambiado exitosamente a: {}", theme.getDisplayName());

        } catch (Exception e) {
            logger.error("Error al cambiar tema, revirtiendo a anterior", e);
            currentTheme = previousTheme;

            // Revertir cambios
            for (Scene scene : registeredScenes) {
                applyThemeToScene(scene, previousTheme);
            }

            throw new RuntimeException("Error al cambiar tema: " + e.getMessage(), e);
        }
    }

    /**
     * Obtiene el tema actual
     */
    public Theme getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Obtiene todos los temas disponibles
     */
    public Theme[] getAvailableThemes() {
        return Theme.values();
    }

    /**
     * Alterna entre tema claro y oscuro
     */
    public void toggleTheme() {
        Theme newTheme = (currentTheme == Theme.DARK) ? Theme.LIGHT : Theme.DARK;
        setTheme(newTheme);
    }

    /**
     * Verifica si un tema está disponible
     */
    public boolean isThemeAvailable(Theme theme) {
        try {
            URL cssUrl = getClass().getResource(theme.getCssPath());
            return cssUrl != null;
        } catch (Exception e) {
            logger.error("Error al verificar disponibilidad del tema: {}", theme.getDisplayName(), e);
            return false;
        }
    }

    /**
     * Aplica un tema a una escena específica
     */
    private void applyThemeToScene(Scene scene, Theme theme) {
        try {
            // Limpiar hojas de estilo existentes
            scene.getStylesheets().clear();

            // Cargar nueva hoja de estilo
            URL cssUrl = getClass().getResource(theme.getCssPath());
            if (cssUrl == null) {
                logger.error("No se encontró el archivo CSS para el tema: {}", theme.getDisplayName());
                return;
            }

            String cssPath = cssUrl.toExternalForm();
            scene.getStylesheets().add(cssPath);

            // Aplicar color de acento personalizado si está configurado
            if (config != null && !config.getUi().getAccentColor().isEmpty()) {
                applyAccentColor(scene, config.getUi().getAccentColor());
            }

            logger.debug("Tema {} aplicado a escena", theme.getDisplayName());

        } catch (Exception e) {
            logger.error("Error al aplicar tema {} a escena", theme.getDisplayName(), e);
        }
    }

    /**
     * Aplica un color de acento personalizado
     */
    private void applyAccentColor(Scene scene, String accentColor) {
        try {
            // Crear CSS dinámico para color de acento
            String customCss = String.format("""
                .root {
                    -fx-accent-color: %s;
                    -fx-accent-hover: derive(%s, 20%%);
                    -fx-accent-pressed: derive(%s, -20%%);
                }
                """, accentColor, accentColor, accentColor);

            // Aplicar CSS inline
            scene.getRoot().setStyle(customCss);

            logger.debug("Color de acento personalizado aplicado: {}", accentColor);

        } catch (Exception e) {
            logger.error("Error al aplicar color de acento personalizado: {}", accentColor, e);
        }
    }

    /**
     * Aplica configuraciones de fuente personalizadas
     */
    public void applyFontSettings(Scene scene) {
        if (config == null) return;

        try {
            int fontSize = config.getUi().getFontSize();

            String fontCss = String.format("""
                .root {
                    -fx-font-size: %dpx;
                }
                """, fontSize);

            // Aplicar como estilo adicional
            String currentStyle = scene.getRoot().getStyle();
            scene.getRoot().setStyle(currentStyle + fontCss);

            logger.debug("Configuración de fuente aplicada: {}px", fontSize);

        } catch (Exception e) {
            logger.error("Error al aplicar configuración de fuente", e);
        }
    }

    /**
     * Aplica CSS personalizado del usuario
     */
    public void applyCustomCss(Scene scene) {
        if (config == null || config.getUi().getCustomCss().isEmpty()) {
            return;
        }

        try {
            String customCss = config.getUi().getCustomCss();

            // Aplicar CSS personalizado como estilo adicional
            String currentStyle = scene.getRoot().getStyle();
            scene.getRoot().setStyle(currentStyle + customCss);

            logger.debug("CSS personalizado aplicado");

        } catch (Exception e) {
            logger.error("Error al aplicar CSS personalizado", e);
        }
    }

    /**
     * Refresca el tema actual (útil después de cambios en configuración)
     */
    public void refreshTheme() {
        logger.info("Refrescando tema actual: {}", currentTheme.getDisplayName());

        for (Scene scene : registeredScenes) {
            applyThemeToScene(scene, currentTheme);
            applyFontSettings(scene);
            applyCustomCss(scene);
        }
    }

    /**
     * Obtiene información del tema actual
     */
    public String getThemeInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Tema Actual: ").append(currentTheme.getDisplayName()).append("\n");
        info.append("ID: ").append(currentTheme.getId()).append("\n");
        info.append("CSS: ").append(currentTheme.getCssPath()).append("\n");
        info.append("Escenas Registradas: ").append(registeredScenes.size()).append("\n");

        if (config != null) {
            info.append("Color de Acento: ").append(config.getUi().getAccentColor()).append("\n");
            info.append("Tamaño de Fuente: ").append(config.getUi().getFontSize()).append("px\n");
            info.append("Animaciones: ").append(config.getUi().isEnableAnimations() ? "Habilitadas" : "Deshabilitadas");
        }

        return info.toString();
    }

    /**
     * Limpia recursos del servicio
     */
    public void shutdown() {
        logger.info("Cerrando servicio de gestión de temas");
        registeredScenes.clear();
        instance = null;
    }
}
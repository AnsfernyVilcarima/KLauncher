package org.klauncher.launcher.services;

import javafx.scene.Scene;
import org.klauncher.launcher.models.config.AdvancedLauncherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio mejorado para gestión de múltiples temas
 */
public class ThemeManagerService {
    private static final Logger logger = LoggerFactory.getLogger(ThemeManagerService.class);

    public enum Theme {
        DARK("dark", "/css/dark-theme.css", "Tema Oscuro", "Diseño oscuro clásico"),
        LIGHT("light", "/css/light-theme.css", "Tema Claro", "Diseño claro y limpio"),
        COSMIC("cosmic", "/css/cosmic-theme.css", "Cosmic Explorer", "Aventura espacial épica"),
        CYBERPUNK("cyberpunk", "/css/cyberpunk-theme.css", "Cyberpunk 2077", "Futuro neon distópico"),
        MATRIX("matrix", "/css/matrix-theme.css", "Matrix Code", "Realidad virtual verde"),
        OCEAN("ocean", "/css/ocean-theme.css", "Deep Ocean", "Profundidades marinas"),
        FOREST("forest", "/css/forest-theme.css", "Mystic Forest", "Bosque encantado"),
        GAMING("gaming", "/css/gaming-theme.css", "Gaming Pro", "Diseño para gamers"),
        MINIMAL("minimal", "/css/minimal-theme.css", "Ultra Minimal", "Simplicidad extrema");

        private final String id;
        private final String cssPath;
        private final String displayName;
        private final String description;

        Theme(String id, String cssPath, String displayName, String description) {
            this.id = id;
            this.cssPath = cssPath;
            this.displayName = displayName;
            this.description = description;
        }

        public String getId() { return id; }
        public String getCssPath() { return cssPath; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }

        public static Theme fromId(String id) {
            for (Theme theme : values()) {
                if (theme.id.equals(id)) {
                    return theme;
                }
            }
            return COSMIC; // Default moderno
        }
    }

    private static ThemeManagerService instance;
    private AdvancedLauncherConfig config;
    private Theme currentTheme;
    private final List<Scene> registeredScenes;
    private CustomizationService customizationService;

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
     * Inicializa el servicio de temas con personalización
     */
    public void initialize(AdvancedLauncherConfig config) {
        this.config = config;

        // Inicializar personalización
        customizationService = CustomizationService.getInstance();
        customizationService.initialize(config);

        // Cargar tema desde configuración
        String themeId = config.getUi().getTheme();
        currentTheme = Theme.fromId(themeId);

        logger.info("Servicio de temas mejorado inicializado con: {}", currentTheme.getDisplayName());
    }

    /**
     * Registra una escena para aplicar temas y personalización
     */
    public void registerScene(Scene scene) {
        if (scene == null) {
            logger.warn("Intentando registrar escena nula");
            return;
        }

        registeredScenes.add(scene);

        // Registrar en servicios dependientes
        if (customizationService != null) {
            customizationService.registerScene(scene);
        }

        applyThemeToScene(scene, currentTheme);

        logger.debug("Escena registrada para gestión de temas mejorada");
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

            // Aplicar preset correspondiente en personalización
            if (customizationService != null) {
                applyThemePreset(theme);
            }

            // Guardar en configuración
            if (config != null) {
                config.getUi().setTheme(theme.getId());
                config.save();
            }

            logger.info("Tema cambiado exitosamente a: {}", theme.getDisplayName());

        } catch (Exception e) {
            logger.error("Error al cambiar tema, revirtiendo", e);
            currentTheme = previousTheme;

            // Revertir cambios
            for (Scene scene : registeredScenes) {
                applyThemeToScene(scene, previousTheme);
            }

            throw new RuntimeException("Error al cambiar tema: " + e.getMessage(), e);
        }
    }

    /**
     * Aplica preset de personalización según el tema
     */
    private void applyThemePreset(Theme theme) {
        CustomizationService.PresetStyle preset = switch (theme) {
            case COSMIC -> CustomizationService.PresetStyle.COSMIC;
            case CYBERPUNK -> CustomizationService.PresetStyle.CYBERPUNK;
            case MATRIX -> CustomizationService.PresetStyle.MATRIX;
            case OCEAN -> CustomizationService.PresetStyle.OCEAN;
            case FOREST -> CustomizationService.PresetStyle.FOREST;
            case GAMING -> CustomizationService.PresetStyle.GAMING;
            case MINIMAL -> CustomizationService.PresetStyle.MINIMAL;
            default -> CustomizationService.PresetStyle.COSMIC;
        };

        customizationService.applyPresetStyle(preset);
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
     * Alterna entre temas populares
     */
    public void cycleTheme() {
        Theme[] popularThemes = {Theme.COSMIC, Theme.CYBERPUNK, Theme.MATRIX, Theme.OCEAN};

        int currentIndex = -1;
        for (int i = 0; i < popularThemes.length; i++) {
            if (popularThemes[i] == currentTheme) {
                currentIndex = i;
                break;
            }
        }

        int nextIndex = (currentIndex + 1) % popularThemes.length;
        setTheme(popularThemes[nextIndex]);
    }

    /**
     * Establece tema aleatorio
     */
    public void setRandomTheme() {
        Theme[] themes = Theme.values();
        Theme randomTheme;

        do {
            randomTheme = themes[(int) (Math.random() * themes.length)];
        } while (randomTheme == currentTheme && themes.length > 1);

        setTheme(randomTheme);
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

            // Intentar cargar CSS del tema
            URL cssUrl = getClass().getResource(theme.getCssPath());
            if (cssUrl != null) {
                String cssPath = cssUrl.toExternalForm();
                scene.getStylesheets().add(cssPath);
                logger.debug("CSS del tema {} aplicado desde: {}", theme.getDisplayName(), cssPath);
            } else {
                // Fallback al tema cósmico
                logger.warn("CSS no encontrado para tema {}, usando fallback", theme.getDisplayName());
                applyFallbackTheme(scene);
            }

            // Aplicar color de acento personalizado
            if (config != null && !config.getUi().getAccentColor().isEmpty()) {
                applyAccentColor(scene, config.getUi().getAccentColor());
            }

            // Aplicar configuraciones de fuente
            applyFontSettings(scene);

            logger.debug("Tema {} aplicado a escena", theme.getDisplayName());

        } catch (Exception e) {
            logger.error("Error al aplicar tema {} a escena", theme.getDisplayName(), e);
            applyFallbackTheme(scene);
        }
    }

    /**
     * Aplica tema de fallback cuando hay errores
     */
    private void applyFallbackTheme(Scene scene) {
        try {
            URL fallbackUrl = getClass().getResource("/css/dark-theme.css");
            if (fallbackUrl != null) {
                scene.getStylesheets().add(fallbackUrl.toExternalForm());
            } else {
                // CSS inline como último recurso
                scene.getRoot().setStyle("""
                    -fx-background-color: #1A1A1A;
                    -fx-text-fill: #FFFFFF;
                    """);
            }
        } catch (Exception e) {
            logger.error("Error incluso en tema de fallback", e);
        }
    }

    /**
     * Aplica un color de acento personalizado
     */
    private void applyAccentColor(Scene scene, String accentColor) {
        try {
            String customCss = String.format("""
                .root {
                    -fx-accent-color: %s;
                    -fx-accent-hover: derive(%s, 20%%);
                    -fx-accent-pressed: derive(%s, -20%%);
                }
                
                #playButton {
                    -fx-background-color: %s;
                    -fx-border-color: %s;
                }
                
                .text-field:focused {
                    -fx-border-color: %s;
                }
                
                .combo-box:focused {
                    -fx-border-color: %s;
                }
                """, accentColor, accentColor, accentColor,
                    accentColor, accentColor, accentColor, accentColor);

            // Aplicar CSS inline adicional
            String currentStyle = scene.getRoot().getStyle();
            scene.getRoot().setStyle(currentStyle + customCss);

            logger.debug("Color de acento personalizado aplicado: {}", accentColor);

        } catch (Exception e) {
            logger.error("Error al aplicar color de acento: {}", accentColor, e);
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
            String currentStyle = scene.getRoot().getStyle();
            scene.getRoot().setStyle(currentStyle + customCss);

            logger.debug("CSS personalizado aplicado");

        } catch (Exception e) {
            logger.error("Error al aplicar CSS personalizado", e);
        }
    }

    /**
     * Refresca el tema actual
     */
    public void refreshTheme() {
        logger.info("Refrescando tema actual: {}", currentTheme.getDisplayName());

        for (Scene scene : registeredScenes) {
            applyThemeToScene(scene, currentTheme);
        }
    }

    /**
     * Abre panel de personalización
     */
    public void openCustomizationPanel() {
        // Este método será implementado para abrir la ventana de personalización
        logger.info("Abriendo panel de personalización");
        // TODO: Implementar apertura de ventana
    }

    /**
     * Obtiene información completa del tema
     */
    public String getThemeInfo() {
        StringBuilder info = new StringBuilder();
        info.append("=== INFORMACIÓN DEL TEMA ===\n");
        info.append("Tema Actual: ").append(currentTheme.getDisplayName()).append("\n");
        info.append("Descripción: ").append(currentTheme.getDescription()).append("\n");
        info.append("ID: ").append(currentTheme.getId()).append("\n");
        info.append("CSS: ").append(currentTheme.getCssPath()).append("\n");
        info.append("Disponible: ").append(isThemeAvailable(currentTheme) ? "Sí" : "No").append("\n");
        info.append("Escenas Registradas: ").append(registeredScenes.size()).append("\n");

        if (config != null) {
            info.append("\n=== CONFIGURACIÓN ===\n");
            info.append("Color de Acento: ").append(config.getUi().getAccentColor()).append("\n");
            info.append("Tamaño de Fuente: ").append(config.getUi().getFontSize()).append("px\n");
            info.append("Animaciones: ").append(config.getUi().isEnableAnimations() ? "Sí" : "No").append("\n");
        }

        info.append("\n=== TEMAS DISPONIBLES ===\n");
        for (Theme theme : Theme.values()) {
            info.append("- ").append(theme.getDisplayName())
                    .append(" (").append(theme.getId()).append(") ")
                    .append(isThemeAvailable(theme) ? "✓" : "✗").append("\n");
        }

        return info.toString();
    }

    /**
     * Limpia recursos del servicio
     */
    public void shutdown() {
        logger.info("Cerrando servicio de gestión de temas mejorado");

        if (customizationService != null) {
            customizationService.shutdown();
        }

        registeredScenes.clear();
        instance = null;
    }
}
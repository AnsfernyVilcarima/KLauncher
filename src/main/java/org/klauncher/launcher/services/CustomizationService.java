package org.klauncher.launcher.services;

import javafx.scene.Scene;
import javafx.scene.layout.Region;
import org.klauncher.launcher.models.config.AdvancedLauncherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio principal de personalización que integra temas, fondos y efectos
 */
public class CustomizationService {
    private static final Logger logger = LoggerFactory.getLogger(CustomizationService.class);

    public enum PresetStyle {
        COSMIC("cosmic", "Cosmic Explorer", "Tema espacial con nebulosas"),
        CYBERPUNK("cyberpunk", "Cyberpunk 2077", "Estilo futurista neon"),
        MATRIX("matrix", "Matrix Reloaded", "Lluvia de código verde"),
        OCEAN("ocean", "Deep Ocean", "Profundidades marinas"),
        FOREST("forest", "Mystic Forest", "Bosque encantado"),
        MINIMAL("minimal", "Clean Minimal", "Diseño limpio y simple"),
        GAMING("gaming", "Gaming Pro", "Optimizado para gamers"),
        CUSTOM("custom", "Custom Style", "Estilo personalizado");

        private final String id;
        private final String displayName;
        private final String description;

        PresetStyle(String id, String displayName, String description) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }

        public static PresetStyle fromId(String id) {
            for (PresetStyle style : values()) {
                if (style.id.equals(id)) {
                    return style;
                }
            }
            return COSMIC;
        }
    }

    public static class CustomizationSettings {
        private PresetStyle presetStyle = PresetStyle.COSMIC;
        private String primaryColor = "#2196F3";
        private String secondaryColor = "#64B5F6";
        private String accentColor = "#00BCD4";
        private BackgroundManagerService.BackgroundType backgroundType = BackgroundManagerService.BackgroundType.COSMIC;
        private boolean enableAnimations = true;
        private boolean enableGlassEffect = true;
        private boolean enableParticles = false;
        private double opacityLevel = 0.9;
        private double blurLevel = 5.0;
        private String customFont = "Segoe UI";
        private int fontSize = 13;

        // Getters y Setters
        public PresetStyle getPresetStyle() { return presetStyle; }
        public void setPresetStyle(PresetStyle presetStyle) { this.presetStyle = presetStyle; }

        public String getPrimaryColor() { return primaryColor; }
        public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }

        public String getSecondaryColor() { return secondaryColor; }
        public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }

        public String getAccentColor() { return accentColor; }
        public void setAccentColor(String accentColor) { this.accentColor = accentColor; }

        public BackgroundManagerService.BackgroundType getBackgroundType() { return backgroundType; }
        public void setBackgroundType(BackgroundManagerService.BackgroundType backgroundType) { this.backgroundType = backgroundType; }

        public boolean isEnableAnimations() { return enableAnimations; }
        public void setEnableAnimations(boolean enableAnimations) { this.enableAnimations = enableAnimations; }

        public boolean isEnableGlassEffect() { return enableGlassEffect; }
        public void setEnableGlassEffect(boolean enableGlassEffect) { this.enableGlassEffect = enableGlassEffect; }

        public boolean isEnableParticles() { return enableParticles; }
        public void setEnableParticles(boolean enableParticles) { this.enableParticles = enableParticles; }

        public double getOpacityLevel() { return opacityLevel; }
        public void setOpacityLevel(double opacityLevel) { this.opacityLevel = opacityLevel; }

        public double getBlurLevel() { return blurLevel; }
        public void setBlurLevel(double blurLevel) { this.blurLevel = blurLevel; }

        public String getCustomFont() { return customFont; }
        public void setCustomFont(String customFont) { this.customFont = customFont; }

        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }
    }

    private static CustomizationService instance;
    private AdvancedLauncherConfig config;
    private CustomizationSettings currentSettings;
    private ThemeManagerService themeManager;
    private BackgroundManagerService backgroundManager;
    private final List<Scene> registeredScenes;

    private CustomizationService() {
        this.registeredScenes = new ArrayList<>();
        this.currentSettings = new CustomizationSettings();
    }

    public static synchronized CustomizationService getInstance() {
        if (instance == null) {
            instance = new CustomizationService();
        }
        return instance;
    }

    /**
     * Inicializa el servicio de personalización
     */
    public void initialize(AdvancedLauncherConfig config) {
        this.config = config;

        // Inicializar servicios dependientes
        themeManager = ThemeManagerService.getInstance();
        themeManager.initialize(config);

        backgroundManager = BackgroundManagerService.getInstance();
        backgroundManager.initialize(config);

        // Cargar configuraciones personalizadas
        loadCustomizationSettings();

        logger.info("Servicio de personalización inicializado");
    }

    /**
     * Registra una escena para personalización
     */
    public void registerScene(Scene scene) {
        if (scene == null) return;

        registeredScenes.add(scene);
        themeManager.registerScene(scene);

        // Registrar región de fondo si es posible
        if (scene.getRoot() instanceof Region) {
            backgroundManager.registerRegion((Region) scene.getRoot());
        }

        applyCurrentCustomization(scene);

        logger.debug("Escena registrada para personalización");
    }

    /**
     * Aplica un preset de estilo completo
     */
    public void applyPresetStyle(PresetStyle preset) {
        logger.info("Aplicando preset: {}", preset.getDisplayName());

        currentSettings.setPresetStyle(preset);

        switch (preset) {
            case COSMIC -> applyCosmicPreset();
            case CYBERPUNK -> applyCyberpunkPreset();
            case MATRIX -> applyMatrixPreset();
            case OCEAN -> applyOceanPreset();
            case FOREST -> applyForestPreset();
            case MINIMAL -> applyMinimalPreset();
            case GAMING -> applyGamingPreset();
            default -> applyCosmicPreset();
        }

        refreshAllScenes();
        saveCustomizationSettings();

        logger.info("Preset {} aplicado exitosamente", preset.getDisplayName());
    }

    /**
     * Aplica configuraciones personalizadas
     */
    public void applyCustomSettings(CustomizationSettings settings) {
        logger.info("Aplicando configuraciones personalizadas");

        this.currentSettings = settings;

        // Aplicar fondo
        backgroundManager.setBackground(settings.getBackgroundType());

        // Aplicar colores y efectos personalizados
        applyCustomColorsAndEffects();

        refreshAllScenes();
        saveCustomizationSettings();

        logger.info("Configuraciones personalizadas aplicadas");
    }

    /**
     * Cambio rápido de color de acento
     */
    public void setAccentColor(String color) {
        currentSettings.setAccentColor(color);
        applyCustomColorsAndEffects();
        refreshAllScenes();
        saveCustomizationSettings();

        logger.info("Color de acento cambiado a: {}", color);
    }

    /**
     * Cambio de fondo independiente
     */
    public void setBackground(BackgroundManagerService.BackgroundType background) {
        currentSettings.setBackgroundType(background);
        backgroundManager.setBackground(background);
        saveCustomizationSettings();

        logger.info("Fondo cambiado a: {}", background.getDisplayName());
    }

    /**
     * Toggle de animaciones
     */
    public void toggleAnimations() {
        currentSettings.setEnableAnimations(!currentSettings.isEnableAnimations());
        refreshAllScenes();
        saveCustomizationSettings();

        logger.info("Animaciones: {}", currentSettings.isEnableAnimations() ? "Habilitadas" : "Deshabilitadas");
    }

    /**
     * Toggle de efectos de cristal
     */
    public void toggleGlassEffect() {
        currentSettings.setEnableGlassEffect(!currentSettings.isEnableGlassEffect());
        refreshAllScenes();
        saveCustomizationSettings();

        logger.info("Efecto cristal: {}", currentSettings.isEnableGlassEffect() ? "Habilitado" : "Deshabilitado");
    }

    // Métodos de presets específicos
    private void applyCosmicPreset() {
        currentSettings.setPrimaryColor("#2196F3");
        currentSettings.setSecondaryColor("#64B5F6");
        currentSettings.setAccentColor("#00BCD4");
        currentSettings.setBackgroundType(BackgroundManagerService.BackgroundType.COSMIC);
        currentSettings.setEnableGlassEffect(true);
        currentSettings.setEnableAnimations(true);
        currentSettings.setOpacityLevel(0.85);

        backgroundManager.setBackground(BackgroundManagerService.BackgroundType.COSMIC);
    }

    private void applyCyberpunkPreset() {
        currentSettings.setPrimaryColor("#FF0080");
        currentSettings.setSecondaryColor("#00FF88");
        currentSettings.setAccentColor("#FFFF00");
        currentSettings.setBackgroundType(BackgroundManagerService.BackgroundType.CYBERPUNK);
        currentSettings.setEnableGlassEffect(false);
        currentSettings.setEnableAnimations(true);
        currentSettings.setOpacityLevel(0.9);

        backgroundManager.setBackground(BackgroundManagerService.BackgroundType.CYBERPUNK);
    }

    private void applyMatrixPreset() {
        currentSettings.setPrimaryColor("#00FF00");
        currentSettings.setSecondaryColor("#80FF80");
        currentSettings.setAccentColor("#00CC00");
        currentSettings.setBackgroundType(BackgroundManagerService.BackgroundType.MATRIX);
        currentSettings.setEnableGlassEffect(false);
        currentSettings.setEnableAnimations(true);
        currentSettings.setOpacityLevel(0.8);

        backgroundManager.setBackground(BackgroundManagerService.BackgroundType.MATRIX);
    }

    private void applyOceanPreset() {
        currentSettings.setPrimaryColor("#0077BE");
        currentSettings.setSecondaryColor("#4FC3F7");
        currentSettings.setAccentColor("#00ACC1");
        currentSettings.setBackgroundType(BackgroundManagerService.BackgroundType.OCEAN);
        currentSettings.setEnableGlassEffect(true);
        currentSettings.setEnableAnimations(true);
        currentSettings.setOpacityLevel(0.85);

        backgroundManager.setBackground(BackgroundManagerService.BackgroundType.OCEAN);
    }

    private void applyForestPreset() {
        currentSettings.setPrimaryColor("#4CAF50");
        currentSettings.setSecondaryColor("#81C784");
        currentSettings.setAccentColor("#66BB6A");
        currentSettings.setBackgroundType(BackgroundManagerService.BackgroundType.FOREST);
        currentSettings.setEnableGlassEffect(true);
        currentSettings.setEnableAnimations(true);
        currentSettings.setOpacityLevel(0.8);

        backgroundManager.setBackground(BackgroundManagerService.BackgroundType.FOREST);
    }

    private void applyMinimalPreset() {
        currentSettings.setPrimaryColor("#424242");
        currentSettings.setSecondaryColor("#757575");
        currentSettings.setAccentColor("#2196F3");
        currentSettings.setBackgroundType(BackgroundManagerService.BackgroundType.GRADIENT);
        currentSettings.setEnableGlassEffect(true);
        currentSettings.setEnableAnimations(false);
        currentSettings.setOpacityLevel(0.95);

        backgroundManager.setBackground(BackgroundManagerService.BackgroundType.GRADIENT);
    }

    private void applyGamingPreset() {
        currentSettings.setPrimaryColor("#E91E63");
        currentSettings.setSecondaryColor("#FF4081");
        currentSettings.setAccentColor("#00E676");
        currentSettings.setBackgroundType(BackgroundManagerService.BackgroundType.PARTICLES);
        currentSettings.setEnableGlassEffect(true);
        currentSettings.setEnableAnimations(true);
        currentSettings.setOpacityLevel(0.9);

        backgroundManager.setBackground(BackgroundManagerService.BackgroundType.PARTICLES);
    }

    /**
     * Aplica colores y efectos personalizados
     */
    private void applyCustomColorsAndEffects() {
        for (Scene scene : registeredScenes) {
            // Aplicar CSS dinámico con colores personalizados
            String customCSS = generateCustomCSS();

            // Limpiar estilos anteriores
            scene.getRoot().setStyle("");

            // Aplicar nuevo estilo
            scene.getRoot().setStyle(customCSS);
        }
    }

    /**
     * Genera CSS personalizado basado en configuraciones
     */
    private String generateCustomCSS() {
        return String.format("""
            .root {
                -fx-primary-color: %s;
                -fx-secondary-color: %s;
                -fx-accent-color: %s;
                -fx-font-family: "%s";
                -fx-font-size: %dpx;
            }
            
            .button {
                -fx-opacity: %.2f;
            }
            
            .main-panel {
                -fx-opacity: %.2f;
                %s
            }
            
            %s
            """,
                currentSettings.getPrimaryColor(),
                currentSettings.getSecondaryColor(),
                currentSettings.getAccentColor(),
                currentSettings.getCustomFont(),
                currentSettings.getFontSize(),
                currentSettings.getOpacityLevel(),
                currentSettings.getOpacityLevel(),
                currentSettings.isEnableGlassEffect() ?
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0.3, 0, 8);" : "",
                currentSettings.isEnableAnimations() ?
                        generateAnimationCSS() : ""
        );
    }

    /**
     * Genera CSS de animaciones
     */
    private String generateAnimationCSS() {
        return """
            .button:hover {
                -fx-scale-x: 1.05;
                -fx-scale-y: 1.05;
            }
            
            .button:pressed {
                -fx-scale-x: 0.95;
                -fx-scale-y: 0.95;
            }
            """;
    }

    /**
     * Refresca todas las escenas registradas
     */
    private void refreshAllScenes() {
        for (Scene scene : registeredScenes) {
            applyCurrentCustomization(scene);
        }
    }

    /**
     * Aplica la personalización actual a una escena
     */
    private void applyCurrentCustomization(Scene scene) {
        // Aplicar tema base
        themeManager.refreshTheme();

        // Aplicar personalizaciones
        applyCustomColorsAndEffects();
    }

    /**
     * Carga configuraciones desde archivo
     */
    private void loadCustomizationSettings() {
        // Por simplicidad, usar valores por defecto
        // En una implementación completa, cargarías desde JSON/YAML
        applyCosmicPreset();
    }

    /**
     * Guarda configuraciones en archivo
     */
    private void saveCustomizationSettings() {
        if (config != null) {
            // Guardar en la configuración existente
            config.getUi().setAccentColor(currentSettings.getAccentColor());
            config.getUi().setEnableAnimations(currentSettings.isEnableAnimations());
            config.getUi().setFontSize(currentSettings.getFontSize());
            config.save();
        }
    }

    /**
     * Obtiene las configuraciones actuales
     */
    public CustomizationSettings getCurrentSettings() {
        return currentSettings;
    }

    /**
     * Obtiene información de personalización
     */
    public String getCustomizationInfo() {
        return String.format("""
            Preset Actual: %s
            Color Primario: %s
            Color de Acento: %s
            Fondo: %s
            Animaciones: %s
            Efectos de Cristal: %s
            Opacidad: %.0f%%
            """,
                currentSettings.getPresetStyle().getDisplayName(),
                currentSettings.getPrimaryColor(),
                currentSettings.getAccentColor(),
                currentSettings.getBackgroundType().getDisplayName(),
                currentSettings.isEnableAnimations() ? "Sí" : "No",
                currentSettings.isEnableGlassEffect() ? "Sí" : "No",
                currentSettings.getOpacityLevel() * 100
        );
    }

    /**
     * Limpia recursos del servicio
     */
    public void shutdown() {
        logger.info("Cerrando servicio de personalización");

        if (themeManager != null) {
            themeManager.shutdown();
        }

        if (backgroundManager != null) {
            backgroundManager.shutdown();
        }

        registeredScenes.clear();
        instance = null;
    }
}
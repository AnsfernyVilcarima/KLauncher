package org.klauncher.launcher.services;

import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.image.Image;
import org.klauncher.launcher.models.config.AdvancedLauncherConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Servicio para gestión de fondos de pantalla dinámicos
 */
public class BackgroundManagerService {
    private static final Logger logger = LoggerFactory.getLogger(BackgroundManagerService.class);

    public enum BackgroundType {
        COSMIC("cosmic", "Cosmic Space", "Nebulosas y galaxias"),
        MATRIX("matrix", "Matrix Code", "Lluvia de código verde"),
        CYBERPUNK("cyberpunk", "Cyberpunk City", "Ciudad futurista"),
        FOREST("forest", "Mystic Forest", "Bosque místico"),
        OCEAN("ocean", "Deep Ocean", "Océano profundo"),
        GRADIENT("gradient", "Dynamic Gradient", "Gradientes dinámicos"),
        PARTICLES("particles", "Floating Particles", "Partículas flotantes"),
        CUSTOM("custom", "Custom Image", "Imagen personalizada");

        private final String id;
        private final String displayName;
        private final String description;

        BackgroundType(String id, String displayName, String description) {
            this.id = id;
            this.displayName = displayName;
            this.description = description;
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }

        public static BackgroundType fromId(String id) {
            for (BackgroundType type : values()) {
                if (type.id.equals(id)) {
                    return type;
                }
            }
            return COSMIC; // Default
        }
    }

    private static BackgroundManagerService instance;
    private AdvancedLauncherConfig config;
    private BackgroundType currentBackground;
    private final List<Region> registeredRegions;
    private final Random random;

    private BackgroundManagerService() {
        this.registeredRegions = new ArrayList<>();
        this.random = new Random();
    }

    public static synchronized BackgroundManagerService getInstance() {
        if (instance == null) {
            instance = new BackgroundManagerService();
        }
        return instance;
    }

    /**
     * Inicializa el servicio de fondos
     */
    public void initialize(AdvancedLauncherConfig config) {
        this.config = config;

        // Cargar fondo desde configuración o usar default
        String backgroundId = config.getUi().getCustomCss(); // Reutilizamos este campo
        currentBackground = BackgroundType.fromId(backgroundId);

        logger.info("Servicio de fondos inicializado con: {}", currentBackground.getDisplayName());
    }

    /**
     * Registra una región para aplicar fondos
     */
    public void registerRegion(Region region) {
        if (region == null) {
            logger.warn("Intentando registrar región nula");
            return;
        }

        registeredRegions.add(region);
        applyBackgroundToRegion(region, currentBackground);

        logger.debug("Región registrada para gestión de fondos");
    }

    /**
     * Cambia el fondo actual
     */
    public void setBackground(BackgroundType backgroundType) {
        if (backgroundType == null) {
            logger.warn("Intentando establecer fondo nulo");
            return;
        }

        if (backgroundType == currentBackground) {
            logger.debug("El fondo {} ya está activo", backgroundType.getDisplayName());
            return;
        }

        logger.info("Cambiando fondo de {} a {}",
                currentBackground.getDisplayName(), backgroundType.getDisplayName());

        currentBackground = backgroundType;

        // Aplicar a todas las regiones registradas
        for (Region region : registeredRegions) {
            applyBackgroundToRegion(region, backgroundType);
        }

        // Guardar en configuración
        if (config != null) {
            config.getUi().setCustomCss(backgroundType.getId());
            config.save();
        }

        logger.info("Fondo cambiado exitosamente a: {}", backgroundType.getDisplayName());
    }

    /**
     * Obtiene el fondo actual
     */
    public BackgroundType getCurrentBackground() {
        return currentBackground;
    }

    /**
     * Obtiene todos los fondos disponibles
     */
    public BackgroundType[] getAvailableBackgrounds() {
        return BackgroundType.values();
    }

    /**
     * Cambia a un fondo aleatorio
     */
    public void setRandomBackground() {
        BackgroundType[] backgrounds = BackgroundType.values();
        BackgroundType randomBackground;

        do {
            randomBackground = backgrounds[random.nextInt(backgrounds.length)];
        } while (randomBackground == currentBackground && backgrounds.length > 1);

        setBackground(randomBackground);
    }

    /**
     * Aplica un fondo a una región específica
     */
    private void applyBackgroundToRegion(Region region, BackgroundType backgroundType) {
        try {
            switch (backgroundType) {
                case COSMIC -> applyCosmicBackground(region);
                case MATRIX -> applyMatrixBackground(region);
                case CYBERPUNK -> applyCyberpunkBackground(region);
                case FOREST -> applyForestBackground(region);
                case OCEAN -> applyOceanBackground(region);
                case GRADIENT -> applyGradientBackground(region);
                case PARTICLES -> applyParticlesBackground(region);
                case CUSTOM -> applyCustomBackground(region);
                default -> applyCosmicBackground(region);
            }

            logger.debug("Fondo {} aplicado a región", backgroundType.getDisplayName());

        } catch (Exception e) {
            logger.error("Error al aplicar fondo {} a región", backgroundType.getDisplayName(), e);
            // Fallback al fondo cósmico
            applyCosmicBackground(region);
        }
    }

    /**
     * Aplica fondo cósmico (gradientes)
     */
    private void applyCosmicBackground(Region region) {
        String style = """
            -fx-background-color: 
                radial-gradient(circle at 20%% 50%%, rgba(120, 119, 198, 0.3), transparent 50%%),
                radial-gradient(circle at 80%% 20%%, rgba(255, 119, 198, 0.3), transparent 50%%),
                radial-gradient(circle at 40%% 80%%, rgba(120, 198, 255, 0.4), transparent 50%%),
                linear-gradient(135deg, #0c0c0c 0%%, #1a1a2e 50%%, #16213e 100%%);
        """;
        region.setStyle(style);
    }

    /**
     * Aplica fondo Matrix
     */
    private void applyMatrixBackground(Region region) {
        String style = """
            -fx-background-color: 
                linear-gradient(90deg, transparent 0%%, rgba(0, 255, 0, 0.03) 50%%, transparent 100%%),
                linear-gradient(0deg, transparent 0%%, rgba(0, 255, 0, 0.02) 50%%, transparent 100%%),
                linear-gradient(135deg, #000000 0%%, #001100 50%%, #000000 100%%);
        """;
        region.setStyle(style);
    }

    /**
     * Aplica fondo Cyberpunk
     */
    private void applyCyberpunkBackground(Region region) {
        String style = """
            -fx-background-color: 
                radial-gradient(circle at 30%% 40%%, rgba(255, 0, 128, 0.3), transparent 60%%),
                radial-gradient(circle at 70%% 60%%, rgba(0, 255, 255, 0.2), transparent 60%%),
                linear-gradient(45deg, #0a0a0a 0%%, #1a0a2e 30%%, #240046 70%%, #0a0a0a 100%%);
        """;
        region.setStyle(style);
    }

    /**
     * Aplica fondo de bosque
     */
    private void applyForestBackground(Region region) {
        String style = """
            -fx-background-color: 
                radial-gradient(circle at 60%% 20%%, rgba(0, 128, 0, 0.2), transparent 50%%),
                radial-gradient(circle at 20%% 80%%, rgba(34, 139, 34, 0.3), transparent 50%%),
                linear-gradient(180deg, #0a0a0a 0%%, #1a2e1a 50%%, #213e21 100%%);
        """;
        region.setStyle(style);
    }

    /**
     * Aplica fondo oceánico
     */
    private void applyOceanBackground(Region region) {
        String style = """
            -fx-background-color: 
                radial-gradient(circle at 40%% 30%%, rgba(0, 150, 255, 0.3), transparent 60%%),
                radial-gradient(circle at 80%% 70%%, rgba(0, 100, 200, 0.2), transparent 50%%),
                linear-gradient(180deg, #001122 0%%, #0a1a2e 50%%, #16213e 100%%);
        """;
        region.setStyle(style);
    }

    /**
     * Aplica fondo de gradiente dinámico
     */
    private void applyGradientBackground(Region region) {
        // Generar colores aleatorios para un gradiente único
        int r1 = random.nextInt(100) + 50;
        int g1 = random.nextInt(100) + 50;
        int b1 = random.nextInt(100) + 50;

        int r2 = random.nextInt(100) + 50;
        int g2 = random.nextInt(100) + 50;
        int b2 = random.nextInt(100) + 50;

        String style = String.format("""
            -fx-background-color: 
                linear-gradient(45deg, 
                    rgba(%d, %d, %d, 0.8) 0%%, 
                    rgba(%d, %d, %d, 0.6) 50%%, 
                    rgba(%d, %d, %d, 0.8) 100%%);
        """, r1, g1, b1, (r1+r2)/2, (g1+g2)/2, (b1+b2)/2, r2, g2, b2);

        region.setStyle(style);
    }

    /**
     * Aplica fondo de partículas
     */
    private void applyParticlesBackground(Region region) {
        String style = """
            -fx-background-color: 
                radial-gradient(circle at 10%% 20%%, rgba(255, 255, 255, 0.1) 1px, transparent 1px),
                radial-gradient(circle at 80%% 80%%, rgba(100, 181, 246, 0.1) 1px, transparent 1px),
                radial-gradient(circle at 40%% 40%%, rgba(255, 255, 255, 0.05) 1px, transparent 1px),
                radial-gradient(circle at 90%% 10%%, rgba(144, 202, 249, 0.1) 1px, transparent 1px),
                linear-gradient(135deg, #0c0c0c 0%%, #1a1a2e 100%%);
            -fx-background-size: 100px 100px, 150px 150px, 200px 200px, 80px 80px, 100%% 100%%;
        """;
        region.setStyle(style);
    }

    /**
     * Aplica fondo personalizado (imagen)
     */
    private void applyCustomBackground(Region region) {
        try {
            // Intentar cargar imagen personalizada
            InputStream imageStream = getClass().getResourceAsStream("/images/backgrounds/custom.jpg");
            if (imageStream != null) {
                Image image = new Image(imageStream);
                BackgroundImage backgroundImage = new BackgroundImage(
                        image,
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundRepeat.NO_REPEAT,
                        BackgroundPosition.CENTER,
                        new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true)
                );
                region.setBackground(new Background(backgroundImage));
            } else {
                // Fallback a gradiente si no hay imagen
                applyCosmicBackground(region);
            }
        } catch (Exception e) {
            logger.warn("No se pudo cargar imagen personalizada, usando fondo cósmico", e);
            applyCosmicBackground(region);
        }
    }

    /**
     * Crea un CSS dinámico para animaciones de fondo
     */
    public String generateAnimatedBackgroundCSS() {
        return switch (currentBackground) {
            case MATRIX -> """
                @keyframes matrix-rain {
                    0% { -fx-translate-y: -100px; }
                    100% { -fx-translate-y: 100vh; }
                }
                .matrix-particle {
                    -fx-animation: matrix-rain 3s linear infinite;
                }
                """;
            case PARTICLES -> """
                @keyframes float-particles {
                    0%, 100% { -fx-translate-y: 0px; }
                    50% { -fx-translate-y: -10px; }
                }
                .floating-particle {
                    -fx-animation: float-particles 4s ease-in-out infinite;
                }
                """;
            case COSMIC -> """
                @keyframes cosmic-pulse {
                    0%, 100% { -fx-opacity: 0.3; }
                    50% { -fx-opacity: 0.6; }
                }
                .cosmic-glow {
                    -fx-animation: cosmic-pulse 6s ease-in-out infinite;
                }
                """;
            default -> "";
        };
    }

    /**
     * Obtiene información del fondo actual
     */
    public String getBackgroundInfo() {
        return String.format("""
            Fondo Actual: %s
            Descripción: %s
            ID: %s
            Regiones Registradas: %d
            """,
                currentBackground.getDisplayName(),
                currentBackground.getDescription(),
                currentBackground.getId(),
                registeredRegions.size()
        );
    }

    /**
     * Limpia recursos del servicio
     */
    public void shutdown() {
        logger.info("Cerrando servicio de gestión de fondos");
        registeredRegions.clear();
        instance = null;
    }
}
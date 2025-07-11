package org.klauncher.launcher.models.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.constraints.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuración avanzada del launcher con soporte para temas, perfiles y más
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdvancedLauncherConfig {
    private static final Logger logger = LoggerFactory.getLogger(AdvancedLauncherConfig.class);
    private static final String CONFIG_FILE_NAME = "launcher-config.yml";

    // Configuración de aplicación
    @JsonProperty("application")
    private ApplicationConfig application = new ApplicationConfig();

    // Configuración de interfaz
    @JsonProperty("ui")
    private UIConfig ui = new UIConfig();

    // Configuración de Java
    @JsonProperty("java")
    private JavaConfig java = new JavaConfig();

    // Configuración de red
    @JsonProperty("network")
    private NetworkConfig network = new NetworkConfig();

    // Configuración de juego
    @JsonProperty("game")
    private GameConfig game = new GameConfig();

    // Configuración de desarrollador
    @JsonProperty("developer")
    private DeveloperConfig developer = new DeveloperConfig();

    // Metadatos
    @JsonProperty("metadata")
    private ConfigMetadata metadata = new ConfigMetadata();

    /**
     * Configuración de la aplicación
     */
    public static class ApplicationConfig {
        @JsonProperty("launcherVersion")
        private String launcherVersion = "1.0.0";

        @JsonProperty("autoUpdate")
        private boolean autoUpdate = true;

        @JsonProperty("checkUpdatesOnStartup")
        private boolean checkUpdatesOnStartup = true;

        @JsonProperty("closeLauncherOnGameStart")
        private boolean closeLauncherOnGameStart = false;

        @JsonProperty("minimizeToTray")
        private boolean minimizeToTray = false;

        @JsonProperty("language")
        @NotBlank
        private String language = "es";

        @JsonProperty("dataDirectory")
        private String dataDirectory = Paths.get(System.getProperty("user.home"), ".karrito").toString();

        // Getters y Setters
        public String getLauncherVersion() { return launcherVersion; }
        public void setLauncherVersion(String launcherVersion) { this.launcherVersion = launcherVersion; }

        public boolean isAutoUpdate() { return autoUpdate; }
        public void setAutoUpdate(boolean autoUpdate) { this.autoUpdate = autoUpdate; }

        public boolean isCheckUpdatesOnStartup() { return checkUpdatesOnStartup; }
        public void setCheckUpdatesOnStartup(boolean checkUpdatesOnStartup) { this.checkUpdatesOnStartup = checkUpdatesOnStartup; }

        public boolean isCloseLauncherOnGameStart() { return closeLauncherOnGameStart; }
        public void setCloseLauncherOnGameStart(boolean closeLauncherOnGameStart) { this.closeLauncherOnGameStart = closeLauncherOnGameStart; }

        public boolean isMinimizeToTray() { return minimizeToTray; }
        public void setMinimizeToTray(boolean minimizeToTray) { this.minimizeToTray = minimizeToTray; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }

        public String getDataDirectory() { return dataDirectory; }
        public void setDataDirectory(String dataDirectory) { this.dataDirectory = dataDirectory; }
    }

    /**
     * Configuración de interfaz de usuario
     */
    public static class UIConfig {
        @JsonProperty("theme")
        @NotBlank
        private String theme = "dark";

        @JsonProperty("accentColor")
        private String accentColor = "#00BCD4";

        @JsonProperty("windowWidth")
        @Min(400)
        private int windowWidth = 900;

        @JsonProperty("windowHeight")
        @Min(300)
        private int windowHeight = 600;

        @JsonProperty("rememberWindowSize")
        private boolean rememberWindowSize = true;

        @JsonProperty("enableAnimations")
        private boolean enableAnimations = true;

        @JsonProperty("showAdvancedOptions")
        private boolean showAdvancedOptions = false;

        @JsonProperty("fontSize")
        @Min(8)
        @Max(24)
        private int fontSize = 12;

        @JsonProperty("customCss")
        private String customCss = "";

        // Getters y Setters
        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }

        public String getAccentColor() { return accentColor; }
        public void setAccentColor(String accentColor) { this.accentColor = accentColor; }

        public int getWindowWidth() { return windowWidth; }
        public void setWindowWidth(int windowWidth) { this.windowWidth = windowWidth; }

        public int getWindowHeight() { return windowHeight; }
        public void setWindowHeight(int windowHeight) { this.windowHeight = windowHeight; }

        public boolean isRememberWindowSize() { return rememberWindowSize; }
        public void setRememberWindowSize(boolean rememberWindowSize) { this.rememberWindowSize = rememberWindowSize; }

        public boolean isEnableAnimations() { return enableAnimations; }
        public void setEnableAnimations(boolean enableAnimations) { this.enableAnimations = enableAnimations; }

        public boolean isShowAdvancedOptions() { return showAdvancedOptions; }
        public void setShowAdvancedOptions(boolean showAdvancedOptions) { this.showAdvancedOptions = showAdvancedOptions; }

        public int getFontSize() { return fontSize; }
        public void setFontSize(int fontSize) { this.fontSize = fontSize; }

        public String getCustomCss() { return customCss; }
        public void setCustomCss(String customCss) { this.customCss = customCss; }
    }

    /**
     * Configuración de Java
     */
    public static class JavaConfig {
        @JsonProperty("autoDetectJava")
        private boolean autoDetectJava = true;

        @JsonProperty("javaExecutablePath")
        private String javaExecutablePath = "";

        @JsonProperty("defaultMinMemoryMb")
        @Min(256)
        private int defaultMinMemoryMb = 512;

        @JsonProperty("defaultMaxMemoryMb")
        @Min(512)
        private int defaultMaxMemoryMb = 2048;

        @JsonProperty("additionalJvmArgs")
        private String additionalJvmArgs = "";

        @JsonProperty("enableJvmOptimizations")
        private boolean enableJvmOptimizations = true;

        @JsonProperty("javaVersions")
        private Map<String, String> javaVersions = new HashMap<>();

        // Getters y Setters
        public boolean isAutoDetectJava() { return autoDetectJava; }
        public void setAutoDetectJava(boolean autoDetectJava) { this.autoDetectJava = autoDetectJava; }

        public String getJavaExecutablePath() { return javaExecutablePath; }
        public void setJavaExecutablePath(String javaExecutablePath) { this.javaExecutablePath = javaExecutablePath; }

        public int getDefaultMinMemoryMb() { return defaultMinMemoryMb; }
        public void setDefaultMinMemoryMb(int defaultMinMemoryMb) { this.defaultMinMemoryMb = defaultMinMemoryMb; }

        public int getDefaultMaxMemoryMb() { return defaultMaxMemoryMb; }
        public void setDefaultMaxMemoryMb(int defaultMaxMemoryMb) { this.defaultMaxMemoryMb = defaultMaxMemoryMb; }

        public String getAdditionalJvmArgs() { return additionalJvmArgs; }
        public void setAdditionalJvmArgs(String additionalJvmArgs) { this.additionalJvmArgs = additionalJvmArgs; }

        public boolean isEnableJvmOptimizations() { return enableJvmOptimizations; }
        public void setEnableJvmOptimizations(boolean enableJvmOptimizations) { this.enableJvmOptimizations = enableJvmOptimizations; }

        public Map<String, String> getJavaVersions() { return javaVersions; }
        public void setJavaVersions(Map<String, String> javaVersions) { this.javaVersions = javaVersions; }
    }

    /**
     * Configuración de red
     */
    public static class NetworkConfig {
        @JsonProperty("useProxy")
        private boolean useProxy = false;

        @JsonProperty("proxyHost")
        private String proxyHost = "";

        @JsonProperty("proxyPort")
        @Min(1)
        @Max(65535)
        private int proxyPort = 8080;

        @JsonProperty("proxyUsername")
        private String proxyUsername = "";

        @JsonProperty("proxyPassword")
        private String proxyPassword = "";

        @JsonProperty("connectionTimeoutSeconds")
        @Min(5)
        @Max(300)
        private int connectionTimeoutSeconds = 30;

        @JsonProperty("readTimeoutSeconds")
        @Min(10)
        @Max(600)
        private int readTimeoutSeconds = 60;

        @JsonProperty("maxConcurrentDownloads")
        @Min(1)
        @Max(16)
        private int maxConcurrentDownloads = 4;

        // Getters y Setters
        public boolean isUseProxy() { return useProxy; }
        public void setUseProxy(boolean useProxy) { this.useProxy = useProxy; }

        public String getProxyHost() { return proxyHost; }
        public void setProxyHost(String proxyHost) { this.proxyHost = proxyHost; }

        public int getProxyPort() { return proxyPort; }
        public void setProxyPort(int proxyPort) { this.proxyPort = proxyPort; }

        public String getProxyUsername() { return proxyUsername; }
        public void setProxyUsername(String proxyUsername) { this.proxyUsername = proxyUsername; }

        public String getProxyPassword() { return proxyPassword; }
        public void setProxyPassword(String proxyPassword) { this.proxyPassword = proxyPassword; }

        public int getConnectionTimeoutSeconds() { return connectionTimeoutSeconds; }
        public void setConnectionTimeoutSeconds(int connectionTimeoutSeconds) { this.connectionTimeoutSeconds = connectionTimeoutSeconds; }

        public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
        public void setReadTimeoutSeconds(int readTimeoutSeconds) { this.readTimeoutSeconds = readTimeoutSeconds; }

        public int getMaxConcurrentDownloads() { return maxConcurrentDownloads; }
        public void setMaxConcurrentDownloads(int maxConcurrentDownloads) { this.maxConcurrentDownloads = maxConcurrentDownloads; }
    }

    /**
     * Configuración de juego
     */
    public static class GameConfig {
        @JsonProperty("defaultVersionType")
        private String defaultVersionType = "release";

        @JsonProperty("enableSnapshots")
        private boolean enableSnapshots = false;

        @JsonProperty("enableOldVersions")
        private boolean enableOldVersions = false;

        @JsonProperty("enableMods")
        private boolean enableMods = true;

        @JsonProperty("autoInstallDependencies")
        private boolean autoInstallDependencies = true;

        @JsonProperty("verifyFileIntegrity")
        private boolean verifyFileIntegrity = true;

        @JsonProperty("keepLogFiles")
        private boolean keepLogFiles = true;

        @JsonProperty("maxLogFiles")
        @Min(1)
        @Max(100)
        private int maxLogFiles = 10;

        // Getters y Setters
        public String getDefaultVersionType() { return defaultVersionType; }
        public void setDefaultVersionType(String defaultVersionType) { this.defaultVersionType = defaultVersionType; }

        public boolean isEnableSnapshots() { return enableSnapshots; }
        public void setEnableSnapshots(boolean enableSnapshots) { this.enableSnapshots = enableSnapshots; }

        public boolean isEnableOldVersions() { return enableOldVersions; }
        public void setEnableOldVersions(boolean enableOldVersions) { this.enableOldVersions = enableOldVersions; }

        public boolean isEnableMods() { return enableMods; }
        public void setEnableMods(boolean enableMods) { this.enableMods = enableMods; }

        public boolean isAutoInstallDependencies() { return autoInstallDependencies; }
        public void setAutoInstallDependencies(boolean autoInstallDependencies) { this.autoInstallDependencies = autoInstallDependencies; }

        public boolean isVerifyFileIntegrity() { return verifyFileIntegrity; }
        public void setVerifyFileIntegrity(boolean verifyFileIntegrity) { this.verifyFileIntegrity = verifyFileIntegrity; }

        public boolean isKeepLogFiles() { return keepLogFiles; }
        public void setKeepLogFiles(boolean keepLogFiles) { this.keepLogFiles = keepLogFiles; }

        public int getMaxLogFiles() { return maxLogFiles; }
        public void setMaxLogFiles(int maxLogFiles) { this.maxLogFiles = maxLogFiles; }
    }

    /**
     * Configuración de desarrollador
     */
    public static class DeveloperConfig {
        @JsonProperty("enableDebugMode")
        private boolean enableDebugMode = false;

        @JsonProperty("enableVerboseLogging")
        private boolean enableVerboseLogging = false;

        @JsonProperty("showPerformanceMetrics")
        private boolean showPerformanceMetrics = false;

        @JsonProperty("enableExperimentalFeatures")
        private boolean enableExperimentalFeatures = false;

        // Getters y Setters
        public boolean isEnableDebugMode() { return enableDebugMode; }
        public void setEnableDebugMode(boolean enableDebugMode) { this.enableDebugMode = enableDebugMode; }

        public boolean isEnableVerboseLogging() { return enableVerboseLogging; }
        public void setEnableVerboseLogging(boolean enableVerboseLogging) { this.enableVerboseLogging = enableVerboseLogging; }

        public boolean isShowPerformanceMetrics() { return showPerformanceMetrics; }
        public void setShowPerformanceMetrics(boolean showPerformanceMetrics) { this.showPerformanceMetrics = showPerformanceMetrics; }

        public boolean isEnableExperimentalFeatures() { return enableExperimentalFeatures; }
        public void setEnableExperimentalFeatures(boolean enableExperimentalFeatures) { this.enableExperimentalFeatures = enableExperimentalFeatures; }
    }

    /**
     * Metadatos de configuración
     */
    public static class ConfigMetadata {
        @JsonProperty("configVersion")
        private int configVersion = 1;

        @JsonProperty("createdAt")
        private LocalDateTime createdAt = LocalDateTime.now();

        @JsonProperty("updatedAt")
        private LocalDateTime updatedAt = LocalDateTime.now();

        @JsonProperty("lastBackup")
        private LocalDateTime lastBackup;

        // Getters y Setters
        public int getConfigVersion() { return configVersion; }
        public void setConfigVersion(int configVersion) { this.configVersion = configVersion; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

        public LocalDateTime getLastBackup() { return lastBackup; }
        public void setLastBackup(LocalDateTime lastBackup) { this.lastBackup = lastBackup; }
    }

    // Getters principales
    public ApplicationConfig getApplication() { return application; }
    public void setApplication(ApplicationConfig application) { this.application = application; }

    public UIConfig getUi() { return ui; }
    public void setUi(UIConfig ui) { this.ui = ui; }

    public JavaConfig getJava() { return java; }
    public void setJava(JavaConfig java) { this.java = java; }

    public NetworkConfig getNetwork() { return network; }
    public void setNetwork(NetworkConfig network) { this.network = network; }

    public GameConfig getGame() { return game; }
    public void setGame(GameConfig game) { this.game = game; }

    public DeveloperConfig getDeveloper() { return developer; }
    public void setDeveloper(DeveloperConfig developer) { this.developer = developer; }

    public ConfigMetadata getMetadata() { return metadata; }
    public void setMetadata(ConfigMetadata metadata) { this.metadata = metadata; }

    /**
     * Carga la configuración desde archivo
     */
    public static AdvancedLauncherConfig load() {
        Path configPath = getConfigPath();

        if (!Files.exists(configPath)) {
            logger.info("Archivo de configuración no existe, creando configuración por defecto");
            AdvancedLauncherConfig defaultConfig = new AdvancedLauncherConfig();
            defaultConfig.save();
            return defaultConfig;
        }

        try {
            ObjectMapper mapper = createObjectMapper();
            AdvancedLauncherConfig config = mapper.readValue(configPath.toFile(), AdvancedLauncherConfig.class);
            logger.info("Configuración cargada desde: {}", configPath);
            return config;
        } catch (IOException e) {
            logger.error("Error al cargar configuración, usando valores por defecto", e);
            return new AdvancedLauncherConfig();
        }
    }

    /**
     * Guarda la configuración en archivo
     */
    public void save() {
        Path configPath = getConfigPath();

        try {
            Files.createDirectories(configPath.getParent());

            // Actualizar timestamp
            metadata.setUpdatedAt(LocalDateTime.now());

            ObjectMapper mapper = createObjectMapper();
            mapper.writeValue(configPath.toFile(), this);
            logger.info("Configuración guardada en: {}", configPath);
        } catch (IOException e) {
            logger.error("Error al guardar configuración", e);
        }
    }

    /**
     * Crea una copia de respaldo de la configuración
     */
    public void backup() {
        Path configPath = getConfigPath();
        Path backupPath = configPath.resolveSibling("launcher-config.backup.yml");

        try {
            if (Files.exists(configPath)) {
                Files.copy(configPath, backupPath);
                metadata.setLastBackup(LocalDateTime.now());
                logger.info("Configuración respaldada en: {}", backupPath);
            }
        } catch (IOException e) {
            logger.error("Error al crear respaldo de configuración", e);
        }
    }

    /**
     * Obtiene la ruta del archivo de configuración
     */
    private static Path getConfigPath() {
        return Paths.get(System.getProperty("user.home"), ".karrito", CONFIG_FILE_NAME);
    }

    /**
     * Crea el ObjectMapper configurado
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.registerModule(new JavaTimeModule());
        mapper.findAndRegisterModules();
        return mapper;
    }

    /**
     * Valida la configuración
     */
    public boolean isValid() {
        return application != null &&
                ui != null &&
                java != null &&
                network != null &&
                game != null &&
                developer != null &&
                metadata != null;
    }
}
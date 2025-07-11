package org.klauncher.launcher.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuración del launcher que se guarda en archivo JSON
 */
public class LauncherConfig {
    private static final Logger logger = LoggerFactory.getLogger(LauncherConfig.class);
    private static final String CONFIG_FILE_NAME = "launcher-config.json";

    @JsonProperty("gameDirectory")
    private String gameDirectory;

    @JsonProperty("javaPath")
    private String javaPath;

    @JsonProperty("maxMemoryMb")
    private int maxMemoryMb;

    @JsonProperty("minMemoryMb")
    private int minMemoryMb;

    @JsonProperty("lastUsername")
    private String lastUsername;

    @JsonProperty("autoLogin")
    private boolean autoLogin;

    @JsonProperty("rememberCredentials")
    private boolean rememberCredentials;

    @JsonProperty("launcherVersion")
    private String launcherVersion;

    @JsonProperty("gameVersion")
    private String gameVersion;

    @JsonProperty("serverUrl")
    private String serverUrl;

    // Constructor por defecto
    public LauncherConfig() {
        // Valores por defecto
        this.gameDirectory = Paths.get(System.getProperty("user.home"), ".karrito").toString();
        this.javaPath = System.getProperty("java.home");
        this.maxMemoryMb = 2048;
        this.minMemoryMb = 512;
        this.lastUsername = "";
        this.autoLogin = false;
        this.rememberCredentials = false;
        this.launcherVersion = "1.0.0";
        this.gameVersion = "1.20.1";
        this.serverUrl = "https://api.karrito.com";
    }

    // Getters y Setters
    public String getGameDirectory() {
        return gameDirectory;
    }

    public void setGameDirectory(String gameDirectory) {
        this.gameDirectory = gameDirectory;
    }

    public String getJavaPath() {
        return javaPath;
    }

    public void setJavaPath(String javaPath) {
        this.javaPath = javaPath;
    }

    public int getMaxMemoryMb() {
        return maxMemoryMb;
    }

    public void setMaxMemoryMb(int maxMemoryMb) {
        this.maxMemoryMb = maxMemoryMb;
    }

    public int getMinMemoryMb() {
        return minMemoryMb;
    }

    public void setMinMemoryMb(int minMemoryMb) {
        this.minMemoryMb = minMemoryMb;
    }

    public String getLastUsername() {
        return lastUsername;
    }

    public void setLastUsername(String lastUsername) {
        this.lastUsername = lastUsername;
    }

    public boolean isAutoLogin() {
        return autoLogin;
    }

    public void setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
    }

    public boolean isRememberCredentials() {
        return rememberCredentials;
    }

    public void setRememberCredentials(boolean rememberCredentials) {
        this.rememberCredentials = rememberCredentials;
    }

    public String getLauncherVersion() {
        return launcherVersion;
    }

    public void setLauncherVersion(String launcherVersion) {
        this.launcherVersion = launcherVersion;
    }

    public String getGameVersion() {
        return gameVersion;
    }

    public void setGameVersion(String gameVersion) {
        this.gameVersion = gameVersion;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    /**
     * Carga la configuración desde archivo
     */
    public static LauncherConfig load() {
        Path configPath = getConfigPath();

        if (!Files.exists(configPath)) {
            logger.info("Archivo de configuración no existe, creando configuración por defecto");
            LauncherConfig defaultConfig = new LauncherConfig();
            defaultConfig.save();
            return defaultConfig;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            LauncherConfig config = mapper.readValue(configPath.toFile(), LauncherConfig.class);
            logger.info("Configuración cargada desde: {}", configPath);
            return config;
        } catch (IOException e) {
            logger.error("Error al cargar configuración, usando valores por defecto", e);
            return new LauncherConfig();
        }
    }

    /**
     * Guarda la configuración en archivo
     */
    public void save() {
        Path configPath = getConfigPath();

        try {
            Files.createDirectories(configPath.getParent());
            ObjectMapper mapper = new ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), this);
            logger.info("Configuración guardada en: {}", configPath);
        } catch (IOException e) {
            logger.error("Error al guardar configuración", e);
        }
    }

    /**
     * Obtiene la ruta del archivo de configuración
     */
    private static Path getConfigPath() {
        return Paths.get(System.getProperty("user.home"), ".karrito", CONFIG_FILE_NAME);
    }

    /**
     * Valida que la configuración sea correcta
     */
    public boolean isValid() {
        if (gameDirectory == null || gameDirectory.isEmpty()) {
            return false;
        }

        if (maxMemoryMb < minMemoryMb) {
            return false;
        }

        if (minMemoryMb < 256) {
            return false;
        }

        return true;
    }

    /**
     * Obtiene la ruta del directorio de juego como Path
     */
    public Path getGameDirectoryPath() {
        return Paths.get(gameDirectory);
    }

    @Override
    public String toString() {
        return "LauncherConfig{" +
                "gameDirectory='" + gameDirectory + '\'' +
                ", javaPath='" + javaPath + '\'' +
                ", maxMemoryMb=" + maxMemoryMb +
                ", minMemoryMb=" + minMemoryMb +
                ", lastUsername='" + lastUsername + '\'' +
                ", autoLogin=" + autoLogin +
                ", rememberCredentials=" + rememberCredentials +
                ", launcherVersion='" + launcherVersion + '\'' +
                ", gameVersion='" + gameVersion + '\'' +
                ", serverUrl='" + serverUrl + '\'' +
                '}';
    }
}
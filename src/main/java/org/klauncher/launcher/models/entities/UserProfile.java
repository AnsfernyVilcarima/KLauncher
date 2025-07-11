package org.klauncher.launcher.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad que representa un perfil de usuario del launcher
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserProfile {

    public enum ProfileType {
        OFFLINE("offline"),
        MICROSOFT("microsoft"),
        MOJANG("mojang");

        private final String value;

        ProfileType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static ProfileType fromString(String value) {
            for (ProfileType type : values()) {
                if (type.value.equals(value)) {
                    return type;
                }
            }
            return OFFLINE;
        }
    }

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("minecraftUsername")
    private String minecraftUsername;

    @JsonProperty("microsoftAccountId")
    private String microsoftAccountId;

    @JsonProperty("profileType")
    private ProfileType profileType;

    @JsonProperty("javaPath")
    private String javaPath;

    @JsonProperty("javaArgs")
    private String javaArgs;

    @JsonProperty("minMemoryMb")
    private int minMemoryMb;

    @JsonProperty("maxMemoryMb")
    private int maxMemoryMb;

    @JsonProperty("gameDirectory")
    private String gameDirectory;

    @JsonProperty("isActive")
    private boolean isActive;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    // Constructor por defecto
    public UserProfile() {
        this.profileType = ProfileType.OFFLINE;
        this.minMemoryMb = 512;
        this.maxMemoryMb = 2048;
        this.isActive = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor con parámetros básicos
    public UserProfile(String name, String displayName, ProfileType profileType) {
        this();
        this.name = name;
        this.displayName = displayName;
        this.profileType = profileType;
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getMinecraftUsername() {
        return minecraftUsername;
    }

    public void setMinecraftUsername(String minecraftUsername) {
        this.minecraftUsername = minecraftUsername;
    }

    public String getMicrosoftAccountId() {
        return microsoftAccountId;
    }

    public void setMicrosoftAccountId(String microsoftAccountId) {
        this.microsoftAccountId = microsoftAccountId;
    }

    public ProfileType getProfileType() {
        return profileType;
    }

    public void setProfileType(ProfileType profileType) {
        this.profileType = profileType;
    }

    public String getJavaPath() {
        return javaPath;
    }

    public void setJavaPath(String javaPath) {
        this.javaPath = javaPath;
    }

    public String getJavaArgs() {
        return javaArgs;
    }

    public void setJavaArgs(String javaArgs) {
        this.javaArgs = javaArgs;
    }

    public int getMinMemoryMb() {
        return minMemoryMb;
    }

    public void setMinMemoryMb(int minMemoryMb) {
        this.minMemoryMb = minMemoryMb;
    }

    public int getMaxMemoryMb() {
        return maxMemoryMb;
    }

    public void setMaxMemoryMb(int maxMemoryMb) {
        this.maxMemoryMb = maxMemoryMb;
    }

    public String getGameDirectory() {
        return gameDirectory;
    }

    public void setGameDirectory(String gameDirectory) {
        this.gameDirectory = gameDirectory;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Métodos de utilidad
    public boolean isOnlineProfile() {
        return profileType == ProfileType.MICROSOFT || profileType == ProfileType.MOJANG;
    }

    public boolean hasMinecraftAccount() {
        return minecraftUsername != null && !minecraftUsername.trim().isEmpty();
    }

    public String getEffectiveGameDirectory() {
        if (gameDirectory != null && !gameDirectory.trim().isEmpty()) {
            return gameDirectory;
        }
        return System.getProperty("user.home") + "/.karrito/profiles/" + name;
    }

    public void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Valida que el perfil tenga los datos mínimos requeridos
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() &&
                displayName != null && !displayName.trim().isEmpty() &&
                profileType != null &&
                minMemoryMb > 0 && maxMemoryMb > minMemoryMb;
    }

    /**
     * Crea una copia del perfil
     */
    public UserProfile copy() {
        UserProfile copy = new UserProfile();
        copy.id = this.id;
        copy.name = this.name;
        copy.displayName = this.displayName;
        copy.minecraftUsername = this.minecraftUsername;
        copy.microsoftAccountId = this.microsoftAccountId;
        copy.profileType = this.profileType;
        copy.javaPath = this.javaPath;
        copy.javaArgs = this.javaArgs;
        copy.minMemoryMb = this.minMemoryMb;
        copy.maxMemoryMb = this.maxMemoryMb;
        copy.gameDirectory = this.gameDirectory;
        copy.isActive = this.isActive;
        copy.createdAt = this.createdAt;
        copy.updatedAt = this.updatedAt;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserProfile that = (UserProfile) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", displayName='" + displayName + '\'' +
                ", profileType=" + profileType +
                ", isActive=" + isActive +
                '}';
    }
}
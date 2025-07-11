package org.klauncher.launcher.services;

import org.klauncher.launcher.database.DatabaseManager;
import org.klauncher.launcher.database.dao.UserProfileDAO;
import org.klauncher.launcher.models.entities.UserProfile;
import org.klauncher.launcher.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Servicio para gestión completa de perfiles de usuario
 */
public class ProfileManagerService {
    private static final Logger logger = LoggerFactory.getLogger(ProfileManagerService.class);

    private final UserProfileDAO profileDAO;
    private final DatabaseManager databaseManager;
    private UserProfile currentActiveProfile;

    public ProfileManagerService() {
        this.databaseManager = DatabaseManager.getInstance();
        this.profileDAO = new UserProfileDAO(databaseManager);
    }

    /**
     * Inicializa el servicio de perfiles
     */
    public void initialize() throws SQLException {
        logger.info("Inicializando servicio de gestión de perfiles");

        // Cargar perfil activo actual
        loadActiveProfile();

        // Si no hay perfiles, crear uno por defecto
        if (getAllProfiles().isEmpty()) {
            createDefaultProfile();
        }

        logger.info("Servicio de gestión de perfiles inicializado");
    }

    /**
     * Crea un nuevo perfil de usuario
     */
    public CompletableFuture<UserProfile> createProfile(String name, String displayName,
                                                        UserProfile.ProfileType type) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Creando nuevo perfil: {} ({})", displayName, type);

                // Validar que el nombre no exista
                if (profileDAO.existsByName(name)) {
                    throw new IllegalArgumentException("Ya existe un perfil con el nombre: " + name);
                }

                // Crear el perfil
                UserProfile profile = new UserProfile(name, displayName, type);

                // Configurar directorio de juego
                String gameDir = Paths.get(System.getProperty("user.home"),
                        ".karrito", "profiles", name).toString();
                profile.setGameDirectory(gameDir);

                // Guardar en base de datos
                UserProfile savedProfile = profileDAO.create(profile);

                // Crear directorio de juego
                createProfileDirectories(savedProfile);

                logger.info("Perfil creado exitosamente: {}", savedProfile);
                return savedProfile;

            } catch (Exception e) {
                logger.error("Error al crear perfil: {}", e.getMessage(), e);
                throw new RuntimeException("Error al crear perfil: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Obtiene todos los perfiles
     */
    public List<UserProfile> getAllProfiles() {
        try {
            return profileDAO.findAll();
        } catch (SQLException e) {
            logger.error("Error al obtener perfiles", e);
            throw new RuntimeException("Error al obtener perfiles", e);
        }
    }

    /**
     * Busca un perfil por nombre
     */
    public Optional<UserProfile> getProfileByName(String name) {
        try {
            return profileDAO.findByName(name);
        } catch (SQLException e) {
            logger.error("Error al buscar perfil por nombre: {}", name, e);
            return Optional.empty();
        }
    }

    /**
     * Busca un perfil por ID
     */
    public Optional<UserProfile> getProfileById(Long id) {
        try {
            return profileDAO.findById(id);
        } catch (SQLException e) {
            logger.error("Error al buscar perfil por ID: {}", id, e);
            return Optional.empty();
        }
    }

    /**
     * Obtiene el perfil activo actual
     */
    public Optional<UserProfile> getActiveProfile() {
        return Optional.ofNullable(currentActiveProfile);
    }

    /**
     * Establece un perfil como activo
     */
    public CompletableFuture<Void> setActiveProfile(Long profileId) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Estableciendo perfil activo: {}", profileId);

                // Verificar que el perfil existe
                Optional<UserProfile> profileOpt = profileDAO.findById(profileId);
                if (profileOpt.isEmpty()) {
                    throw new IllegalArgumentException("No existe perfil con ID: " + profileId);
                }

                // Establecer como activo en base de datos
                profileDAO.setActiveProfile(profileId);

                // Actualizar perfil activo en memoria
                currentActiveProfile = profileOpt.get();
                currentActiveProfile.setActive(true);

                logger.info("Perfil {} establecido como activo", currentActiveProfile.getName());

            } catch (Exception e) {
                logger.error("Error al establecer perfil activo: {}", e.getMessage(), e);
                throw new RuntimeException("Error al establecer perfil activo: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Actualiza un perfil existente
     */
    public CompletableFuture<UserProfile> updateProfile(UserProfile profile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Actualizando perfil: {}", profile.getName());

                // Validar perfil
                if (!profile.isValid()) {
                    throw new IllegalArgumentException("Los datos del perfil no son válidos");
                }

                // Actualizar timestamp
                profile.updateTimestamp();

                // Guardar en base de datos
                UserProfile updatedProfile = profileDAO.update(profile);

                // Si es el perfil activo, actualizar en memoria
                if (currentActiveProfile != null &&
                        currentActiveProfile.getId().equals(updatedProfile.getId())) {
                    currentActiveProfile = updatedProfile;
                }

                logger.info("Perfil actualizado exitosamente: {}", updatedProfile.getName());
                return updatedProfile;

            } catch (Exception e) {
                logger.error("Error al actualizar perfil: {}", e.getMessage(), e);
                throw new RuntimeException("Error al actualizar perfil: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Elimina un perfil
     */
    public CompletableFuture<Boolean> deleteProfile(Long profileId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Eliminando perfil: {}", profileId);

                // Verificar que no sea el único perfil
                if (profileDAO.count() <= 1) {
                    throw new IllegalStateException("No se puede eliminar el único perfil existente");
                }

                // Obtener perfil antes de eliminar
                Optional<UserProfile> profileOpt = profileDAO.findById(profileId);
                if (profileOpt.isEmpty()) {
                    return false;
                }

                UserProfile profile = profileOpt.get();

                // Si es el perfil activo, cambiar a otro
                if (currentActiveProfile != null &&
                        currentActiveProfile.getId().equals(profileId)) {

                    // Buscar otro perfil para activar
                    List<UserProfile> allProfiles = profileDAO.findAll();
                    Optional<UserProfile> newActiveProfile = allProfiles.stream()
                            .filter(p -> !p.getId().equals(profileId))
                            .findFirst();

                    if (newActiveProfile.isPresent()) {
                        profileDAO.setActiveProfile(newActiveProfile.get().getId());
                        currentActiveProfile = newActiveProfile.get();
                        currentActiveProfile.setActive(true);
                    }
                }

                // Eliminar directorio del perfil (opcional)
                deleteProfileDirectories(profile);

                // Eliminar de base de datos
                boolean deleted = profileDAO.delete(profileId);

                if (deleted) {
                    logger.info("Perfil {} eliminado exitosamente", profile.getName());
                }

                return deleted;

            } catch (Exception e) {
                logger.error("Error al eliminar perfil: {}", e.getMessage(), e);
                throw new RuntimeException("Error al eliminar perfil: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Duplica un perfil existente
     */
    public CompletableFuture<UserProfile> duplicateProfile(Long profileId, String newName, String newDisplayName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Duplicando perfil {} como {}", profileId, newName);

                // Obtener perfil original
                Optional<UserProfile> originalOpt = profileDAO.findById(profileId);
                if (originalOpt.isEmpty()) {
                    throw new IllegalArgumentException("No existe perfil con ID: " + profileId);
                }

                // Crear copia
                UserProfile original = originalOpt.get();
                UserProfile duplicate = original.copy();
                duplicate.setId(null);
                duplicate.setName(newName);
                duplicate.setDisplayName(newDisplayName);
                duplicate.setActive(false);

                // Configurar nuevo directorio
                String gameDir = Paths.get(System.getProperty("user.home"),
                        ".karrito", "profiles", newName).toString();
                duplicate.setGameDirectory(gameDir);

                // Guardar
                UserProfile savedDuplicate = profileDAO.create(duplicate);

                // Crear directorios
                createProfileDirectories(savedDuplicate);

                // Copiar archivos de configuración si existen
                copyProfileFiles(original, savedDuplicate);

                logger.info("Perfil duplicado exitosamente: {}", savedDuplicate.getName());
                return savedDuplicate;

            } catch (Exception e) {
                logger.error("Error al duplicar perfil: {}", e.getMessage(), e);
                throw new RuntimeException("Error al duplicar perfil: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Carga el perfil activo desde la base de datos
     */
    private void loadActiveProfile() {
        try {
            Optional<UserProfile> activeProfile = profileDAO.findActiveProfile();
            currentActiveProfile = activeProfile.orElse(null);

            if (currentActiveProfile != null) {
                logger.info("Perfil activo cargado: {}", currentActiveProfile.getName());
            } else {
                logger.info("No hay perfil activo");
            }
        } catch (SQLException e) {
            logger.error("Error al cargar perfil activo", e);
        }
    }

    /**
     * Crea un perfil por defecto si no existe ninguno
     */
    private void createDefaultProfile() {
        try {
            logger.info("Creando perfil por defecto");

            UserProfile defaultProfile = new UserProfile();
            defaultProfile.setName("default");
            defaultProfile.setDisplayName("Default Profile");
            defaultProfile.setProfileType(UserProfile.ProfileType.OFFLINE);
            defaultProfile.setActive(true);

            String gameDir = Paths.get(System.getProperty("user.home"),
                    ".karrito", "profiles", "default").toString();
            defaultProfile.setGameDirectory(gameDir);

            UserProfile savedProfile = profileDAO.create(defaultProfile);
            createProfileDirectories(savedProfile);

            currentActiveProfile = savedProfile;

            logger.info("Perfil por defecto creado: {}", savedProfile.getName());
        } catch (SQLException e) {
            logger.error("Error al crear perfil por defecto", e);
        }
    }

    /**
     * Crea los directorios necesarios para un perfil
     */
    private void createProfileDirectories(UserProfile profile) {
        try {
            Path gameDir = Paths.get(profile.getEffectiveGameDirectory());
            Files.createDirectories(gameDir);
            Files.createDirectories(gameDir.resolve("saves"));
            Files.createDirectories(gameDir.resolve("screenshots"));
            Files.createDirectories(gameDir.resolve("resourcepacks"));
            Files.createDirectories(gameDir.resolve("config"));

            logger.debug("Directorios creados para perfil: {}", profile.getName());
        } catch (IOException e) {
            logger.warn("Error al crear directorios para perfil {}: {}",
                    profile.getName(), e.getMessage());
        }
    }

    /**
     * Elimina los directorios de un perfil
     */
    private void deleteProfileDirectories(UserProfile profile) {
        try {
            Path gameDir = Paths.get(profile.getEffectiveGameDirectory());
            if (Files.exists(gameDir)) {
                FileUtils.deleteDirectory(gameDir);
                logger.debug("Directorios eliminados para perfil: {}", profile.getName());
            }
        } catch (IOException e) {
            logger.warn("Error al eliminar directorios para perfil {}: {}",
                    profile.getName(), e.getMessage());
        }
    }

    /**
     * Copia archivos de configuración entre perfiles
     */
    private void copyProfileFiles(UserProfile source, UserProfile target) {
        try {
            Path sourceDir = Paths.get(source.getEffectiveGameDirectory());
            Path targetDir = Paths.get(target.getEffectiveGameDirectory());

            if (Files.exists(sourceDir)) {
                // Copiar archivos de configuración específicos
                copyIfExists(sourceDir.resolve("options.txt"), targetDir.resolve("options.txt"));
                copyIfExists(sourceDir.resolve("servers.dat"), targetDir.resolve("servers.dat"));

                logger.debug("Archivos copiados de {} a {}", source.getName(), target.getName());
            }
        } catch (Exception e) {
            logger.warn("Error al copiar archivos entre perfiles: {}", e.getMessage());
        }
    }

    /**
     * Copia un archivo si existe
     */
    private void copyIfExists(Path source, Path target) throws IOException {
        if (Files.exists(source)) {
            Files.createDirectories(target.getParent());
            Files.copy(source, target);
        }
    }

    /**
     * Cierra el servicio
     */
    public void shutdown() {
        logger.info("Cerrando servicio de gestión de perfiles");
        // Aquí se pueden agregar tareas de limpieza si es necesario
    }
}
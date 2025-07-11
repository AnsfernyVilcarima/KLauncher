package org.klauncher.launcher.database.dao;

import org.klauncher.launcher.database.DatabaseManager;
import org.klauncher.launcher.models.entities.UserProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object para gestión de perfiles de usuario
 */
public class UserProfileDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserProfileDAO.class);

    private final DatabaseManager databaseManager;

    public UserProfileDAO(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Crea un nuevo perfil de usuario
     */
    public UserProfile create(UserProfile profile) throws SQLException {
        logger.debug("Creando nuevo perfil: {}", profile.getName());

        String sql = """
            INSERT INTO user_profiles (
                name, display_name, minecraft_username, microsoft_account_id,
                profile_type, java_path, java_args, min_memory_mb, max_memory_mb,
                game_directory, is_active
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setProfileParameters(stmt, profile);

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Error al crear perfil, no se insertaron filas");
            }

            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    profile.setId(generatedKeys.getLong(1));
                    profile.setCreatedAt(LocalDateTime.now());
                    profile.setUpdatedAt(LocalDateTime.now());
                }
            }

            logger.info("Perfil creado exitosamente: {} (ID: {})", profile.getName(), profile.getId());
            return profile;
        }
    }

    /**
     * Busca un perfil por ID
     */
    public Optional<UserProfile> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM user_profiles WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProfile(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Busca un perfil por nombre
     */
    public Optional<UserProfile> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM user_profiles WHERE name = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToProfile(rs));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Obtiene todos los perfiles
     */
    public List<UserProfile> findAll() throws SQLException {
        String sql = "SELECT * FROM user_profiles ORDER BY created_at DESC";
        List<UserProfile> profiles = new ArrayList<>();

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                profiles.add(mapResultSetToProfile(rs));
            }
        }

        logger.debug("Encontrados {} perfiles", profiles.size());
        return profiles;
    }

    /**
     * Obtiene el perfil activo actual
     */
    public Optional<UserProfile> findActiveProfile() throws SQLException {
        String sql = "SELECT * FROM user_profiles WHERE is_active = 1 LIMIT 1";

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return Optional.of(mapResultSetToProfile(rs));
            }
        }

        return Optional.empty();
    }

    /**
     * Actualiza un perfil existente
     */
    public UserProfile update(UserProfile profile) throws SQLException {
        logger.debug("Actualizando perfil: {} (ID: {})", profile.getName(), profile.getId());

        if (profile.getId() == null) {
            throw new IllegalArgumentException("No se puede actualizar un perfil sin ID");
        }

        String sql = """
            UPDATE user_profiles SET
                name = ?, display_name = ?, minecraft_username = ?, microsoft_account_id = ?,
                profile_type = ?, java_path = ?, java_args = ?, min_memory_mb = ?, max_memory_mb = ?,
                game_directory = ?, is_active = ?, updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
        """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            setProfileParameters(stmt, profile);
            stmt.setLong(12, profile.getId());

            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("No se encontró el perfil con ID: " + profile.getId());
            }

            profile.setUpdatedAt(LocalDateTime.now());
            logger.info("Perfil actualizado exitosamente: {}", profile.getName());
            return profile;
        }
    }

    /**
     * Establece un perfil como activo (desactiva todos los demás)
     */
    public void setActiveProfile(Long profileId) throws SQLException {
        logger.debug("Estableciendo perfil activo: {}", profileId);

        try (Connection conn = databaseManager.getConnection()) {
            conn.setAutoCommit(false);

            try {
                // Desactivar todos los perfiles
                String deactivateAllSql = "UPDATE user_profiles SET is_active = 0";
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate(deactivateAllSql);
                }

                // Activar el perfil específico
                String activateSql = "UPDATE user_profiles SET is_active = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(activateSql)) {
                    stmt.setLong(1, profileId);
                    int affectedRows = stmt.executeUpdate();

                    if (affectedRows == 0) {
                        throw new SQLException("No se encontró el perfil con ID: " + profileId);
                    }
                }

                conn.commit();
                logger.info("Perfil {} establecido como activo", profileId);

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Elimina un perfil
     */
    public boolean delete(Long profileId) throws SQLException {
        logger.debug("Eliminando perfil: {}", profileId);

        String sql = "DELETE FROM user_profiles WHERE id = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, profileId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                logger.info("Perfil {} eliminado exitosamente", profileId);
                return true;
            }

            return false;
        }
    }

    /**
     * Verifica si existe un perfil con el nombre dado
     */
    public boolean existsByName(String name) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user_profiles WHERE name = ?";

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Obtiene el conteo total de perfiles
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM user_profiles";

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /**
     * Establece los parámetros del PreparedStatement para operaciones de inserción/actualización
     */
    private void setProfileParameters(PreparedStatement stmt, UserProfile profile) throws SQLException {
        stmt.setString(1, profile.getName());
        stmt.setString(2, profile.getDisplayName());
        stmt.setString(3, profile.getMinecraftUsername());
        stmt.setString(4, profile.getMicrosoftAccountId());
        stmt.setString(5, profile.getProfileType().getValue());
        stmt.setString(6, profile.getJavaPath());
        stmt.setString(7, profile.getJavaArgs());
        stmt.setInt(8, profile.getMinMemoryMb());
        stmt.setInt(9, profile.getMaxMemoryMb());
        stmt.setString(10, profile.getGameDirectory());
        stmt.setBoolean(11, profile.isActive());
    }

    /**
     * Mapea un ResultSet a un objeto UserProfile
     */
    private UserProfile mapResultSetToProfile(ResultSet rs) throws SQLException {
        UserProfile profile = new UserProfile();

        profile.setId(rs.getLong("id"));
        profile.setName(rs.getString("name"));
        profile.setDisplayName(rs.getString("display_name"));
        profile.setMinecraftUsername(rs.getString("minecraft_username"));
        profile.setMicrosoftAccountId(rs.getString("microsoft_account_id"));
        profile.setProfileType(UserProfile.ProfileType.fromString(rs.getString("profile_type")));
        profile.setJavaPath(rs.getString("java_path"));
        profile.setJavaArgs(rs.getString("java_args"));
        profile.setMinMemoryMb(rs.getInt("min_memory_mb"));
        profile.setMaxMemoryMb(rs.getInt("max_memory_mb"));
        profile.setGameDirectory(rs.getString("game_directory"));
        profile.setActive(rs.getBoolean("is_active"));

        // Mapear timestamps
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            profile.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            profile.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        return profile;
    }
}
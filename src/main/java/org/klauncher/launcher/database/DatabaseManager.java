package org.klauncher.launcher.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Properties;

/**
 * Gestor de base de datos SQLite para el launcher
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_NAME = "karrito_launcher.db";
    private static final String DB_VERSION_KEY = "schema_version";
    private static final int CURRENT_SCHEMA_VERSION = 1;

    private static DatabaseManager instance;
    private final Path databasePath;
    private Connection connection;

    private DatabaseManager() {
        this.databasePath = getDataDirectory().resolve(DB_NAME);
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Inicializa la base de datos
     */
    public void initialize() throws SQLException {
        logger.info("Inicializando base de datos en: {}", databasePath);

        try {
            // Crear directorio si no existe
            Files.createDirectories(databasePath.getParent());

            // Establecer conexión
            connect();

            // Verificar y actualizar esquema
            checkAndUpdateSchema();

            logger.info("Base de datos inicializada correctamente");
        } catch (IOException e) {
            throw new SQLException("Error al crear directorio de base de datos", e);
        }
    }

    /**
     * Establece conexión con la base de datos
     */
    private void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return;
        }

        String url = "jdbc:sqlite:" + databasePath.toString();
        Properties props = new Properties();
        props.setProperty("foreign_keys", "true");
        props.setProperty("journal_mode", "WAL");
        props.setProperty("synchronous", "NORMAL");

        connection = DriverManager.getConnection(url, props);
        connection.setAutoCommit(true);

        logger.debug("Conexión establecida con la base de datos");
    }

    /**
     * Verifica y actualiza el esquema de la base de datos
     */
    private void checkAndUpdateSchema() throws SQLException {
        // Crear tabla de metadatos si no existe
        createMetadataTable();

        int currentVersion = getDatabaseVersion();
        logger.info("Versión actual del esquema: {}, Versión requerida: {}",
                currentVersion, CURRENT_SCHEMA_VERSION);

        if (currentVersion < CURRENT_SCHEMA_VERSION) {
            logger.info("Actualizando esquema de base de datos...");
            runMigrations(currentVersion);
            setDatabaseVersion(CURRENT_SCHEMA_VERSION);
            logger.info("Esquema actualizado exitosamente");
        }
    }

    /**
     * Crea la tabla de metadatos
     */
    private void createMetadataTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS metadata (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Ejecuta las migraciones necesarias
     */
    private void runMigrations(int fromVersion) throws SQLException {
        if (fromVersion < 1) {
            createInitialSchema();
        }
        // Aquí se agregarían futuras migraciones
    }

    /**
     * Crea el esquema inicial de la base de datos
     */
    private void createInitialSchema() throws SQLException {
        logger.info("Creando esquema inicial de la base de datos");

        String[] createTables = {
                // Tabla de perfiles de usuario
                """
            CREATE TABLE IF NOT EXISTS user_profiles (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL UNIQUE,
                display_name TEXT NOT NULL,
                minecraft_username TEXT,
                microsoft_account_id TEXT,
                profile_type TEXT NOT NULL DEFAULT 'offline',
                java_path TEXT,
                java_args TEXT,
                min_memory_mb INTEGER DEFAULT 512,
                max_memory_mb INTEGER DEFAULT 2048,
                game_directory TEXT,
                is_active BOOLEAN DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """,

                // Tabla de configuraciones globales
                """
            CREATE TABLE IF NOT EXISTS launcher_settings (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                key TEXT NOT NULL UNIQUE,
                value TEXT,
                value_type TEXT NOT NULL DEFAULT 'string',
                description TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """,

                // Tabla de versiones de Minecraft instaladas
                """
            CREATE TABLE IF NOT EXISTS minecraft_versions (
                id TEXT PRIMARY KEY,
                version_type TEXT NOT NULL,
                release_time DATETIME,
                is_installed BOOLEAN DEFAULT 0,
                installation_path TEXT,
                loader_type TEXT,
                loader_version TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """,

                // Tabla de cuentas de Microsoft
                """
            CREATE TABLE IF NOT EXISTS microsoft_accounts (
                id TEXT PRIMARY KEY,
                username TEXT NOT NULL,
                access_token TEXT,
                refresh_token TEXT,
                token_expires_at DATETIME,
                minecraft_profile TEXT,
                is_active BOOLEAN DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """,

                // Tabla de logs del launcher
                """
            CREATE TABLE IF NOT EXISTS launcher_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                level TEXT NOT NULL,
                logger_name TEXT NOT NULL,
                message TEXT NOT NULL,
                exception_trace TEXT,
                user_profile_id INTEGER,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_profile_id) REFERENCES user_profiles(id)
            )
            """
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : createTables) {
                stmt.execute(sql);
            }
        }

        // Crear índices
        createIndexes();

        // Insertar configuraciones por defecto
        insertDefaultSettings();

        logger.info("Esquema inicial creado exitosamente");
    }

    /**
     * Crea los índices necesarios
     */
    private void createIndexes() throws SQLException {
        String[] indexes = {
                "CREATE INDEX IF NOT EXISTS idx_user_profiles_active ON user_profiles(is_active)",
                "CREATE INDEX IF NOT EXISTS idx_launcher_settings_key ON launcher_settings(key)",
                "CREATE INDEX IF NOT EXISTS idx_minecraft_versions_installed ON minecraft_versions(is_installed)",
                "CREATE INDEX IF NOT EXISTS idx_microsoft_accounts_active ON microsoft_accounts(is_active)",
                "CREATE INDEX IF NOT EXISTS idx_launcher_logs_created_at ON launcher_logs(created_at)",
                "CREATE INDEX IF NOT EXISTS idx_launcher_logs_level ON launcher_logs(level)"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : indexes) {
                stmt.execute(sql);
            }
        }
    }

    /**
     * Inserta configuraciones por defecto
     */
    private void insertDefaultSettings() throws SQLException {
        String sql = """
            INSERT OR IGNORE INTO launcher_settings (key, value, value_type, description) VALUES 
            ('theme', 'dark', 'string', 'Tema de la interfaz (dark/light)'),
            ('check_updates', 'true', 'boolean', 'Verificar actualizaciones automáticamente'),
            ('close_launcher_on_game_start', 'false', 'boolean', 'Cerrar launcher al iniciar el juego'),
            ('default_java_path', '', 'string', 'Ruta por defecto de Java'),
            ('download_timeout_seconds', '300', 'integer', 'Timeout para descargas en segundos'),
            ('max_concurrent_downloads', '4', 'integer', 'Máximo de descargas concurrentes'),
            ('launcher_version', '1.0.0', 'string', 'Versión del launcher')
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Obtiene la versión actual de la base de datos
     */
    private int getDatabaseVersion() throws SQLException {
        String sql = "SELECT value FROM metadata WHERE key = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, DB_VERSION_KEY);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Integer.parseInt(rs.getString("value"));
            }
            return 0;
        } catch (NumberFormatException e) {
            logger.warn("Error al parsear versión de base de datos, asumiendo versión 0");
            return 0;
        }
    }

    /**
     * Establece la versión de la base de datos
     */
    private void setDatabaseVersion(int version) throws SQLException {
        String sql = """
            INSERT OR REPLACE INTO metadata (key, value, updated_at) 
            VALUES (?, ?, CURRENT_TIMESTAMP)
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, DB_VERSION_KEY);
            stmt.setString(2, String.valueOf(version));
            stmt.executeUpdate();
        }
    }

    /**
     * Obtiene la conexión actual
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        return connection;
    }

    /**
     * Cierra la conexión con la base de datos
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Conexión con base de datos cerrada");
            }
        } catch (SQLException e) {
            logger.error("Error al cerrar conexión con base de datos", e);
        }
    }

    /**
     * Obtiene el directorio de datos del launcher
     */
    private Path getDataDirectory() {
        return Paths.get(System.getProperty("user.home"), ".karrito");
    }

    /**
     * Ejecuta una consulta de prueba para verificar la conexión
     */
    public boolean testConnection() {
        try {
            String sql = "SELECT 1";
            try (Statement stmt = getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error al probar conexión con base de datos", e);
            return false;
        }
    }
}
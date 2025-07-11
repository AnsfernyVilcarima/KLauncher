package org.klauncher.launcher.utils;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Consumer;

public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    /**
     * Calcula el hash SHA-256 de un archivo
     */
    public static String calculateSHA256(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(filePath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Verifica si un archivo existe y tiene el hash correcto
     */
    public static boolean verifyFileIntegrity(Path filePath, String expectedHash) {
        if (!Files.exists(filePath)) {
            logger.debug("Archivo no existe: {}", filePath);
            return false;
        }

        try {
            String actualHash = calculateSHA256(filePath);
            boolean isValid = actualHash.equals(expectedHash);

            if (!isValid) {
                logger.warn("Hash incorrecto para archivo: {}. Esperado: {}, Actual: {}",
                        filePath, expectedHash, actualHash);
            }

            return isValid;
        } catch (IOException e) {
            logger.error("Error al verificar integridad del archivo: {}", filePath, e);
            return false;
        }
    }

    /**
     * Copia un archivo con callback de progreso
     */
    public static void copyFileWithProgress(Path source, Path destination, Consumer<Long> progressCallback) throws IOException {
        logger.debug("Copiando archivo: {} -> {}", source, destination);

        Files.createDirectories(destination.getParent());

        long totalSize = Files.size(source);
        long copiedBytes = 0;

        try (InputStream in = Files.newInputStream(source);
             OutputStream out = Files.newOutputStream(destination)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                copiedBytes += bytesRead;

                if (progressCallback != null) {
                    progressCallback.accept(copiedBytes);
                }
            }
        }

        logger.debug("Archivo copiado exitosamente: {}", destination);
    }

    /**
     * Extrae un archivo ZIP con callback de progreso
     */
    public static void extractZipWithProgress(Path zipFile, Path destinationDir,
                                              Consumer<String> progressCallback) throws IOException {
        logger.info("Extrayendo archivo ZIP: {} -> {}", zipFile, destinationDir);

        Files.createDirectories(destinationDir);

        try (InputStream fileIn = Files.newInputStream(zipFile);
             BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
             ZipArchiveInputStream zipIn = new ZipArchiveInputStream(bufferedIn)) {

            ZipArchiveEntry entry;
            while ((entry = zipIn.getNextZipEntry()) != null) {
                Path entryPath = destinationDir.resolve(entry.getName());

                // Verificar que el archivo se extraiga dentro del directorio destino
                if (!entryPath.normalize().startsWith(destinationDir.normalize())) {
                    throw new IOException("Entrada ZIP fuera del directorio destino: " + entry.getName());
                }

                if (progressCallback != null) {
                    progressCallback.accept(entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());

                    try (OutputStream out = Files.newOutputStream(entryPath)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = zipIn.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        }

        logger.info("Archivo ZIP extraído exitosamente");
    }

    /**
     * Elimina un directorio y todo su contenido
     */
    public static void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        logger.debug("Eliminando directorio: {}", directory);

        Files.walk(directory)
                .sorted((path1, path2) -> path2.compareTo(path1)) // Orden reverso para eliminar archivos antes que directorios
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        logger.warn("No se pudo eliminar: {}", path, e);
                    }
                });

        logger.debug("Directorio eliminado: {}", directory);
    }

    /**
     * Obtiene el tamaño total de un directorio
     */
    public static long getDirectorySize(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return 0;
        }

        return Files.walk(directory)
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        logger.warn("Error al obtener tamaño del archivo: {}", path, e);
                        return 0;
                    }
                })
                .sum();
    }

    /**
     * Crea un directorio si no existe
     */
    public static void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            logger.debug("Directorio creado: {}", directory);
        }
    }

    /**
     * Mueve un archivo de forma segura
     */
    public static void moveFile(Path source, Path destination) throws IOException {
        logger.debug("Moviendo archivo: {} -> {}", source, destination);

        Files.createDirectories(destination.getParent());
        Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);

        logger.debug("Archivo movido exitosamente");
    }

    /**
     * Verifica si hay suficiente espacio en disco
     */
    public static boolean hasEnoughSpace(Path directory, long requiredBytes) {
        try {
            FileStore fileStore = Files.getFileStore(directory);
            long availableBytes = fileStore.getUsableSpace();

            logger.debug("Espacio disponible: {} bytes, Requerido: {} bytes",
                    availableBytes, requiredBytes);

            return availableBytes >= requiredBytes;
        } catch (IOException e) {
            logger.warn("Error al verificar espacio en disco", e);
            return false;
        }
    }

    /**
     * Formatea el tamaño en bytes a una cadena legible
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
module karrito.launcher {
    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    // HTTP client
    requires okhttp3;

    // JSON processing
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;

    // Compression
    requires org.apache.commons.compress;

    // Logging
    requires org.slf4j;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;

    // Required for JavaFX FXML
    opens org.klauncher.launcher.controllers to javafx.fxml;
    opens org.klauncher.launcher to javafx.fxml;

    // Required for JSON serialization

    // Export main packages
    exports org.klauncher.launcher;
    exports org.klauncher.launcher.controllers;
    exports org.klauncher.launcher.services;
    exports org.klauncher.launcher.utils;
}
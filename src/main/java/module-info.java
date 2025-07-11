module karrito.launcher {
    // JavaFX modules
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    // HTTP client
    requires okhttp3;
    requires retrofit2;

    // JSON processing
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.fasterxml.jackson.dataformat.yaml;

    // Database
    requires java.sql;
    requires org.xerial.sqlitejdbc;

    // Compression
    requires org.apache.commons.compress;

    // Utilities
    requires org.apache.commons.lang3;
    requires com.google.common;

    // Validation
    requires org.hibernate.validator;
    requires jakarta.el;
    requires jakarta.validation;

    // Logging
    requires org.slf4j;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;

    // Required for JavaFX FXML
    opens org.klauncher.launcher.controllers to javafx.fxml;
    opens org.klauncher.launcher to javafx.fxml;

    // Required for JSON serialization
    opens org.klauncher.launcher.models to com.fasterxml.jackson.databind;
    opens org.klauncher.launcher.models.entities to com.fasterxml.jackson.databind, org.hibernate.validator;
    opens org.klauncher.launcher.models.config to com.fasterxml.jackson.databind;

    // Export main packages
    exports org.klauncher.launcher;
    exports org.klauncher.launcher.controllers;
    exports org.klauncher.launcher.models;
    exports org.klauncher.launcher.models.entities;
    exports org.klauncher.launcher.models.config;
    exports org.klauncher.launcher.services;
    exports org.klauncher.launcher.utils;
    exports org.klauncher.launcher.database;
    exports org.klauncher.launcher.database.dao;
}
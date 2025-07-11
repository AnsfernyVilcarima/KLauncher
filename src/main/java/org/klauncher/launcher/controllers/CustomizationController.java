package org.klauncher.launcher.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import org.klauncher.launcher.services.CustomizationService;
import org.klauncher.launcher.services.BackgroundManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador para el panel de personalización avanzada
 */
public class CustomizationController {
    private static final Logger logger = LoggerFactory.getLogger(CustomizationController.class);

    // Presets
    @FXML private ComboBox<CustomizationService.PresetStyle> presetComboBox;
    @FXML private Button applyPresetButton;
    @FXML private Button randomPresetButton;

    // Colores
    @FXML private ColorPicker primaryColorPicker;
    @FXML private ColorPicker secondaryColorPicker;
    @FXML private ColorPicker accentColorPicker;

    // Fondos
    @FXML private ComboBox<BackgroundManagerService.BackgroundType> backgroundComboBox;
    @FXML private Button randomBackgroundButton;

    // Efectos
    @FXML private CheckBox enableAnimationsCheck;
    @FXML private CheckBox enableGlassEffectCheck;
    @FXML private CheckBox enableParticlesCheck;
    @FXML private Slider opacitySlider;
    @FXML private Slider blurSlider;

    // Fuentes
    @FXML private ComboBox<String> fontComboBox;
    @FXML private Slider fontSizeSlider;
    @FXML private Label fontSizeLabel;

    // Controles
    @FXML private Button applyButton;
    @FXML private Button resetButton;
    @FXML private Button saveButton;
    @FXML private Button closeButton;

    // Preview
    @FXML private VBox previewPanel;
    @FXML private Label previewTitle;
    @FXML private Button previewButton;

    // Servicios
    private CustomizationService customizationService;
    private CustomizationService.CustomizationSettings currentSettings;

    @FXML
    public void initialize() {
        logger.info("Inicializando panel de personalización");

        // Inicializar servicio
        customizationService = CustomizationService.getInstance();
        currentSettings = customizationService.getCurrentSettings();

        // Configurar controles
        setupPresetControls();
        setupColorControls();
        setupBackgroundControls();
        setupEffectControls();
        setupFontControls();
        setupPreview();

        // Cargar valores actuales
        loadCurrentSettings();

        logger.info("Panel de personalización inicializado");
    }

    /**
     * Configura controles de presets
     */
    private void setupPresetControls() {
        // Llenar combo de presets
        ObservableList<CustomizationService.PresetStyle> presets =
                FXCollections.observableArrayList(CustomizationService.PresetStyle.values());
        presetComboBox.setItems(presets);
        presetComboBox.setCellFactory(param -> new PresetListCell());
        presetComboBox.setButtonCell(new PresetListCell());

        // Evento de cambio de preset
        presetComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                previewPreset(newVal);
            }
        });
    }

    /**
     * Configura controles de color
     */
    private void setupColorControls() {
        // Eventos de cambio de color
        primaryColorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentSettings.setPrimaryColor(colorToHex(newVal));
                updatePreview();
            }
        });

        secondaryColorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentSettings.setSecondaryColor(colorToHex(newVal));
                updatePreview();
            }
        });

        accentColorPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentSettings.setAccentColor(colorToHex(newVal));
                updatePreview();
            }
        });
    }

    /**
     * Configura controles de fondo
     */
    private void setupBackgroundControls() {
        // Llenar combo de fondos
        ObservableList<BackgroundManagerService.BackgroundType> backgrounds =
                FXCollections.observableArrayList(BackgroundManagerService.BackgroundType.values());
        backgroundComboBox.setItems(backgrounds);
        backgroundComboBox.setCellFactory(param -> new BackgroundListCell());
        backgroundComboBox.setButtonCell(new BackgroundListCell());

        // Evento de cambio de fondo
        backgroundComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentSettings.setBackgroundType(newVal);
                updatePreview();
            }
        });
    }

    /**
     * Configura controles de efectos
     */
    private void setupEffectControls() {
        // Checkboxes
        enableAnimationsCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            currentSettings.setEnableAnimations(newVal);
            updatePreview();
        });

        enableGlassEffectCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            currentSettings.setEnableGlassEffect(newVal);
            updatePreview();
        });

        enableParticlesCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            currentSettings.setEnableParticles(newVal);
            updatePreview();
        });

        // Sliders
        opacitySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentSettings.setOpacityLevel(newVal.doubleValue() / 100.0);
            updatePreview();
        });

        blurSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentSettings.setBlurLevel(newVal.doubleValue());
            updatePreview();
        });
    }

    /**
     * Configura controles de fuente
     */
    private void setupFontControls() {
        // Llenar combo de fuentes
        ObservableList<String> fonts = FXCollections.observableArrayList(
                "Segoe UI", "Arial", "Helvetica", "Times New Roman",
                "Calibri", "Verdana", "Tahoma", "Georgia",
                "Consolas", "Courier New", "Impact", "Comic Sans MS"
        );
        fontComboBox.setItems(fonts);

        // Evento de cambio de fuente
        fontComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentSettings.setCustomFont(newVal);
                updatePreview();
            }
        });

        // Slider de tamaño de fuente
        fontSizeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int size = newVal.intValue();
            currentSettings.setFontSize(size);
            fontSizeLabel.setText(size + "px");
            updatePreview();
        });
    }

    /**
     * Configura panel de vista previa
     */
    private void setupPreview() {
        // Configurar estilos del preview
        previewPanel.getStyleClass().add("glass-panel");
        previewTitle.getStyleClass().add("preview-title");
        previewButton.getStyleClass().add("preview-button");
    }

    /**
     * Carga configuraciones actuales
     */
    private void loadCurrentSettings() {
        // Presets
        presetComboBox.setValue(currentSettings.getPresetStyle());

        // Colores
        primaryColorPicker.setValue(javafx.scene.paint.Color.web(currentSettings.getPrimaryColor()));
        secondaryColorPicker.setValue(javafx.scene.paint.Color.web(currentSettings.getSecondaryColor()));
        accentColorPicker.setValue(javafx.scene.paint.Color.web(currentSettings.getAccentColor()));

        // Fondos
        backgroundComboBox.setValue(currentSettings.getBackgroundType());

        // Efectos
        enableAnimationsCheck.setSelected(currentSettings.isEnableAnimations());
        enableGlassEffectCheck.setSelected(currentSettings.isEnableGlassEffect());
        enableParticlesCheck.setSelected(currentSettings.isEnableParticles());
        opacitySlider.setValue(currentSettings.getOpacityLevel() * 100);
        blurSlider.setValue(currentSettings.getBlurLevel());

        // Fuentes
        fontComboBox.setValue(currentSettings.getCustomFont());
        fontSizeSlider.setValue(currentSettings.getFontSize());
        fontSizeLabel.setText(currentSettings.getFontSize() + "px");

        updatePreview();
    }

    /**
     * Previsualiza un preset sin aplicarlo
     */
    private void previewPreset(CustomizationService.PresetStyle preset) {
        // Crear configuraciones temporales para preview
        CustomizationService.CustomizationSettings tempSettings =
                new CustomizationService.CustomizationSettings();
        tempSettings.setPresetStyle(preset);

        // Aplicar valores del preset (sin guardarlo)
        switch (preset) {
            case COSMIC -> {
                tempSettings.setPrimaryColor("#2196F3");
                tempSettings.setSecondaryColor("#64B5F6");
                tempSettings.setAccentColor("#00BCD4");
                tempSettings.setBackgroundType(BackgroundManagerService.BackgroundType.COSMIC);
            }
            case CYBERPUNK -> {
                tempSettings.setPrimaryColor("#FF0080");
                tempSettings.setSecondaryColor("#00FF88");
                tempSettings.setAccentColor("#FFFF00");
                tempSettings.setBackgroundType(BackgroundManagerService.BackgroundType.CYBERPUNK);
            }
            case MATRIX -> {
                tempSettings.setPrimaryColor("#00FF00");
                tempSettings.setSecondaryColor("#80FF80");
                tempSettings.setAccentColor("#00CC00");
                tempSettings.setBackgroundType(BackgroundManagerService.BackgroundType.MATRIX);
            }
            // ... más presets
        }

        // Actualizar controles sin disparar eventos
        updateControlsFromSettings(tempSettings);
    }

    /**
     * Actualiza controles desde configuraciones
     */
    private void updateControlsFromSettings(CustomizationService.CustomizationSettings settings) {
        primaryColorPicker.setValue(javafx.scene.paint.Color.web(settings.getPrimaryColor()));
        secondaryColorPicker.setValue(javafx.scene.paint.Color.web(settings.getSecondaryColor()));
        accentColorPicker.setValue(javafx.scene.paint.Color.web(settings.getAccentColor()));
        backgroundComboBox.setValue(settings.getBackgroundType());
        enableAnimationsCheck.setSelected(settings.isEnableAnimations());
        enableGlassEffectCheck.setSelected(settings.isEnableGlassEffect());
        enableParticlesCheck.setSelected(settings.isEnableParticles());
        opacitySlider.setValue(settings.getOpacityLevel() * 100);
        blurSlider.setValue(settings.getBlurLevel());
        fontComboBox.setValue(settings.getCustomFont());
        fontSizeSlider.setValue(settings.getFontSize());
    }

    /**
     * Actualiza vista previa
     */
    private void updatePreview() {
        String previewStyle = String.format("""
            -fx-background-color: %s;
            -fx-border-color: %s;
            -fx-border-width: 2px;
            -fx-border-radius: 10px;
            -fx-background-radius: 10px;
            -fx-opacity: %.2f;
            %s
            """,
                currentSettings.getPrimaryColor(),
                currentSettings.getAccentColor(),
                currentSettings.getOpacityLevel(),
                currentSettings.isEnableGlassEffect() ?
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0.5, 0, 5);" : ""
        );

        previewPanel.setStyle(previewStyle);
        previewTitle.setStyle("-fx-text-fill: " + currentSettings.getAccentColor() + ";");
        previewButton.setStyle(String.format("""
            -fx-background-color: %s;
            -fx-text-fill: white;
            -fx-border-color: %s;
            """,
                currentSettings.getAccentColor(),
                currentSettings.getSecondaryColor()
        ));
    }

    // Event Handlers
    @FXML
    private void onApplyPreset(ActionEvent event) {
        CustomizationService.PresetStyle selected = presetComboBox.getValue();
        if (selected != null) {
            customizationService.applyPresetStyle(selected);
            showInfo("Preset Aplicado", "Preset '" + selected.getDisplayName() + "' aplicado exitosamente.");
        }
    }

    @FXML
    private void onRandomPreset(ActionEvent event) {
        CustomizationService.PresetStyle[] presets = CustomizationService.PresetStyle.values();
        CustomizationService.PresetStyle random = presets[(int) (Math.random() * presets.length)];
        presetComboBox.setValue(random);
        customizationService.applyPresetStyle(random);
        showInfo("Preset Aleatorio", "Preset '" + random.getDisplayName() + "' aplicado.");
    }

    @FXML
    private void onRandomBackground(ActionEvent event) {
        BackgroundManagerService.BackgroundType[] backgrounds =
                BackgroundManagerService.BackgroundType.values();
        BackgroundManagerService.BackgroundType random =
                backgrounds[(int) (Math.random() * backgrounds.length)];
        backgroundComboBox.setValue(random);
        customizationService.setBackground(random);
        showInfo("Fondo Aleatorio", "Fondo '" + random.getDisplayName() + "' aplicado.");
    }

    @FXML
    private void onApply(ActionEvent event) {
        customizationService.applyCustomSettings(currentSettings);
        showInfo("Aplicado", "Personalización aplicada exitosamente.");
    }

    @FXML
    private void onReset(ActionEvent event) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Reset");
        confirm.setHeaderText("¿Resetear personalización?");
        confirm.setContentText("Esto restaurará la configuración por defecto.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            customizationService.applyPresetStyle(CustomizationService.PresetStyle.COSMIC);
            loadCurrentSettings();
            showInfo("Reset", "Personalización restaurada a valores por defecto.");
        }
    }

    @FXML
    private void onSave(ActionEvent event) {
        customizationService.applyCustomSettings(currentSettings);
        showInfo("Guardado", "Personalización guardada exitosamente.");
    }

    @FXML
    private void onClose(ActionEvent event) {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    // Utility methods
    private String colorToHex(javafx.scene.paint.Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255)
        );
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Cell factories
    private static class PresetListCell extends ListCell<CustomizationService.PresetStyle> {
        @Override
        protected void updateItem(CustomizationService.PresetStyle preset, boolean empty) {
            super.updateItem(preset, empty);
            if (empty || preset == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(preset.getDisplayName() + " - " + preset.getDescription());
            }
        }
    }

    private static class BackgroundListCell extends ListCell<BackgroundManagerService.BackgroundType> {
        @Override
        protected void updateItem(BackgroundManagerService.BackgroundType background, boolean empty) {
            super.updateItem(background, empty);
            if (empty || background == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(background.getDisplayName() + " - " + background.name());
            }
        }
    }
}
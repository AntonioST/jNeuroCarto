package io.ast.jneurocarto.javafx.utils;

import javafx.scene.control.Dialog;
import javafx.stage.Modality;
import javafx.stage.Stage;

public final class DialogUtils {
    private DialogUtils() {
        throw new RuntimeException();
    }

    public static void setAlwaysOnTop(Dialog<?> dialog) {
        dialog.initModality(Modality.NONE); // do not steal events from main window.
        dialog.setOnShown(_ -> {
            var stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.setAlwaysOnTop(true);
        });
    }
}

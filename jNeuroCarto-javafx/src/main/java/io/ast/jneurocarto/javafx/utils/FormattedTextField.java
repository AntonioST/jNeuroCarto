package io.ast.jneurocarto.javafx.utils;

import java.util.function.Function;

import javafx.beans.property.*;
import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.util.StringConverter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class FormattedTextField<T> extends TextField {

    private final StringConverter<T> converter;
    private final Tooltip tooltip = new Tooltip("");

    public FormattedTextField(@Nullable T value, StringConverter<T> converter) {
        this.converter = converter;
        super(converter.toString(value));

        getValueProperty().addListener((_, _, v) -> setText(converter.toString(v)));
        textProperty().addListener((_, _, t) -> onKeyTyped(t));
        addEventHandler(ActionEvent.ACTION, _ -> onAction());
        focusedProperty().addListener((_, _, focus) -> {
            if (!focus) onAction();
        });
    }

    public abstract Property<T> getValueProperty();

    public abstract @Nullable T getValue();

    public abstract void setValue(@Nullable T value);

    private void onKeyTyped(String text) {
        try {
            converter.fromString(text);
            Tooltip.uninstall(this, tooltip);
            setStyle("-fx-control-inner-background: white;");
        } catch (Exception ex) {
            tooltip.setText(ex.getMessage());
            Tooltip.install(this, tooltip);
            setStyle("-fx-control-inner-background: pink;");
        }
    }

    private void onAction() {
        try {
            setValue(converter.fromString(getText()));
            Tooltip.uninstall(this, tooltip);
            setStyle("-fx-control-inner-background: white;");
        } catch (Exception ex) {
            tooltip.setText(ex.getMessage());
            Tooltip.install(this, tooltip);
            setStyle("-fx-control-inner-background: pink;");
        }
    }

    public static void install(TextField field, Function<String, @Nullable String> validator) {
        var tooltip = new Tooltip();
        field.textProperty().addListener((_, _, t) -> {
            var message = validator.apply(t);
            if (message == null) {
                Tooltip.uninstall(field, tooltip);
                field.setStyle("-fx-control-inner-background: white;");
            } else {
                if (!message.isEmpty()) {
                    tooltip.setText(message);
                    Tooltip.install(field, tooltip);
                } else {
                    Tooltip.uninstall(field, tooltip);
                }
                field.setStyle("-fx-control-inner-background: pink;");
            }
        });
    }

    public static class OfIntField extends FormattedTextField<Number> {
        private static final StringConverter<Number> INT = new StringConverter<>() {
            @Override
            public String toString(Number object) {
                return Integer.toString(object.intValue());
            }

            @Override
            public Number fromString(String string) {
                return Integer.parseInt(string);
            }
        };

        private final IntegerProperty intProperty;

        public OfIntField(int value) {
            this(value, INT);
        }

        public OfIntField(int value, StringConverter<Number> converter) {
            intProperty = new SimpleIntegerProperty(value);
            super(value, converter);
        }

        public OfIntField(IntegerProperty value, StringConverter<Number> converter) {
            intProperty = new SimpleIntegerProperty();
            super(value.intValue(), converter);
            intProperty.bindBidirectional(value);
        }

        @Override
        public Property<Number> getValueProperty() {
            return intProperty;
        }

        @Override
        public Integer getValue() {
            return intProperty.getValue();
        }

        public int getIntValue() {
            return intProperty.getValue();
        }

        @Override
        public void setValue(@Nullable Number value) {
            intProperty.set(value == null ? 0 : value.intValue());
        }

        public void setIntValue(int value) {
            intProperty.set(value);
        }
    }

    public static class OfDoubleField extends FormattedTextField<Number> {
        private static final StringConverter<Number> DOUBLE = new StringConverter<>() {
            @Override
            public String toString(Number object) {
                return Double.toString(object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return Double.parseDouble(string);
            }
        };

        private final SimpleDoubleProperty doubleProperty;

        public OfDoubleField(double value) {
            this(value, DOUBLE);

        }

        public OfDoubleField(double value, StringConverter<Number> converter) {
            doubleProperty = new SimpleDoubleProperty(value);
            super(value, converter);
        }

        public OfDoubleField(DoubleProperty value, StringConverter<Number> converter) {
            doubleProperty = new SimpleDoubleProperty();
            super(value.get(), converter);
            doubleProperty.bindBidirectional(value);
        }

        @Override
        public Property<Number> getValueProperty() {
            return doubleProperty;
        }

        @Override
        public Double getValue() {
            return doubleProperty.getValue();
        }

        public double getDoubleValue() {
            return doubleProperty.getValue();
        }

        @Override
        public void setValue(@Nullable Number value) {
            doubleProperty.set(value == null ? 0 : value.doubleValue());
        }

        public void setDoubleValue(double value) {
            doubleProperty.set(value);
        }
    }

    public static class OfObjectField<T> extends FormattedTextField<T> {
        private final ObjectProperty<T> valueProperty;

        public OfObjectField(@Nullable T value, StringConverter<T> converter) {
            valueProperty = new SimpleObjectProperty<>(value);
            super(value, converter);
        }

        public OfObjectField(ObjectProperty<T> value, StringConverter<T> converter) {
            valueProperty = new SimpleObjectProperty<>();
            super(value.get(), converter);
            valueProperty.bindBidirectional(value);
        }

        @Override
        public Property<T> getValueProperty() {
            return valueProperty;
        }

        public @Nullable T getValue() {
            return valueProperty.get();
        }

        public final void setValue(@Nullable T value) {
            valueProperty.set(value);
        }
    }
}

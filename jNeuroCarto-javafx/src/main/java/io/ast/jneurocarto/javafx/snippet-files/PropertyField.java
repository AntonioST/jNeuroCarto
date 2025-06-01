package io.ast.jneurocarto.snippets;


import javafx.beans.property.*;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PropertyField {
    private static class Example1<TYPE> {
        // @start region="public ObjectProperty<T>"
        // @highlight region regex="[Tt]ype|TYPE"
        public final ObjectProperty<@Nullable TYPE> typeProperty = new SimpleObjectProperty<>();

        public final @Nullable TYPE getType() {
            return typeProperty.get();
        }

        public final void setType(@Nullable TYPE value) {
            typeProperty.set(value);
        }
        // @end
        // @end

        // @start region="public IntegerProperty"
        // @highlight region regex="[Bb]ool"
        public final BooleanProperty boolProperty = new SimpleBooleanProperty();

        public final boolean isBool() {
            return boolProperty.get();
        }

        public final void setBool(boolean value) {
            boolProperty.set(value);
        }
        // @end
        // @end

        // @start region="public IntegerProperty"
        // @highlight region regex="[Ii]nt"
        public final IntegerProperty intProperty = new SimpleIntegerProperty();

        public final int getInt() {
            return intProperty.get();
        }

        public final void setInt(int value) {
            intProperty.set(value);
        }
        // @end
        // @end

        // @start region="public DoubleProperty"
        // @highlight region regex="[Dd]ouble"
        public final DoubleProperty doubleProperty = new SimpleDoubleProperty();

        public final double getDouble() {
            return doubleProperty.get();
        }

        public final void setDouble(double value) {
            doubleProperty.set(value);
        }
        // @end
        // @end

        // @start region="public StringProperty"
        // @highlight region regex="[Ss]tring"
        public final StringProperty stringProperty = new SimpleStringProperty();

        public final @Nullable String getString() {
            return stringProperty.get();
        }

        public final void setString(@Nullable String value) {
            stringProperty.set(value);
        }
        // @end
        // @end
    }

    private static class Example2<TYPE> {
        // @start region="private ObjectProperty<T>"
        // @highlight region regex="[Tt]ype|TYPE"
        private final ObjectProperty<@Nullable TYPE> type = new SimpleObjectProperty<>();

        public final ObjectProperty<@Nullable TYPE> typeProperty() {
            return type;
        }

        public final @Nullable TYPE getType() {
            return type.get();
        }

        public final void setType(@Nullable TYPE value) {
            type.set(value);
        }
        // @end
        // @end
    }
}
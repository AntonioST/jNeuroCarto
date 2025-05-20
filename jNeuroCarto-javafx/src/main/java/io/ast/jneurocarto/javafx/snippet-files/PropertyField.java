package io.ast.jneurocarto.snippets;


import javafx.beans.property.*;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Parent;

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

    private static class Example3 extends Parent {
        // @start region="custom event"
        // @highlight region regex="TypeEvent|TYPE"

        public static class TypeEvent extends Event {
            public static final EventType<TypeEvent> ANY = new EventType<>(Event.ANY, "TYPE_EVENT");
            public static final EventType<TypeEvent> OTHER = new EventType<>(ANY, "OTHER_TYPE_EVENT");

            TypeEvent(@Nullable Object source, EventTarget target, EventType<TypeEvent> type) {
                super(source, target, type);
            }
        }

        private final ObjectProperty<@Nullable EventHandler<TypeEvent>> typeEventHandler = new SimpleObjectProperty<>();

        public final ObjectProperty<@Nullable EventHandler<TypeEvent>> onTypeEventProperty() {
            return typeEventHandler;
        }

        public final @Nullable EventHandler<TypeEvent> getOnTypeEvent() {
            return typeEventHandler.get();
        }

        public final void setOnTypeEvent(EventHandler<TypeEvent> value) {
            typeEventHandler.set(value);
        }

        private void fireTypeEvent() {
            if (!isDisabled()) {
                fireEvent(new TypeEvent(null, null, TypeEvent.ANY));
            }
        }
        // @end
        // @end
    }
}
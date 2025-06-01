package io.ast.jneurocarto.javafx.chart.snippets;


import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.Parent;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class CustomEvent {

    class Example2 extends Parent {
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
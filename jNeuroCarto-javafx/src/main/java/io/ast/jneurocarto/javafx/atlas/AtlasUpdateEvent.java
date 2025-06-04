package io.ast.jneurocarto.javafx.atlas;

import javafx.event.Event;
import javafx.event.EventType;

import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.atlas.BrainAtlas;
import io.ast.jneurocarto.atlas.ImageSlice;
import io.ast.jneurocarto.atlas.ImageSliceStack;

public class AtlasUpdateEvent extends Event {
    public static final EventType<AtlasUpdateEvent> ANY = new EventType<>(Event.ANY, "ATLAS_ANY");
    public static final EventType<AtlasUpdateEvent> LOADED = new EventType<>(ANY, "ATLAS_LOADED");
    public static final EventType<AtlasUpdateEvent> PROJECTION = new EventType<>(ANY, "ATLAS_PROJECTION");
    public static final EventType<AtlasUpdateEvent> SLICING = new EventType<>(ANY, "ATLAS_SLICING");
    public static final EventType<AtlasUpdateEvent> POSITION = new EventType<>(ANY, "ATLAS_POSITION");
    public static final EventType<AtlasUpdateEvent> REFERENCE = new EventType<>(ANY, "ATLAS_REFERENCE");
    private final AtlasPlugin atlas;


    public AtlasUpdateEvent(EventType<AtlasUpdateEvent> type, AtlasPlugin atlas) {
        super(type);
        this.atlas = atlas;
    }

    public @Nullable BrainAtlas brain() {
        return atlas.getBrainAtlas();
    }

    public String name() {
        return atlas.atlasName();
    }

    public ImageSliceStack.Projection getProjection() {
        return atlas.getProjection();
    }

    public @Nullable AtlasReference getAtlasReference() {
        return atlas.getAtlasReference();
    }

    public @Nullable String getAtlasReferenceName() {
        var reference = getAtlasReference();
        if (reference == null) return null;
        return reference.name();
    }

    @Nullable
    public ImageSliceStack getImageSliceStack() {
        return atlas.getImageSliceStack();
    }

    @Nullable
    public ImageSlice getImageSlice() {
        return atlas.getImageSlice();
    }

    public SlicePainter painter() {
        return atlas.painter();
    }
}

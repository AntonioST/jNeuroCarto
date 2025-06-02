package io.ast.jneurocarto.javafx.atlas;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import io.ast.jneurocarto.core.Coordinate;

@JsonRootName("AtlasReferences")
public class AtlasReferenceState {

    @JsonProperty()
    public Map<String, AtlasReferenceData> brains = new HashMap<>();

    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({
      @JsonSubTypes.Type(value = AtlasReferenceIndirect.class),
      @JsonSubTypes.Type(value = AtlasReferenceCoordinate.class)
    })
    public sealed interface AtlasReferenceData {
    }

    public record AtlasReferenceIndirect(
        @JsonProperty() String use
    ) implements AtlasReferenceData {
    }

    public static final class AtlasReferenceCoordinate implements AtlasReferenceData {
        @JsonProperty()
        public Map<String, int[]> references = new HashMap<>();
    }

    public @Nullable AtlasReferenceCoordinate get(String atlas) {
        var a = brains.get(atlas);
        while (a instanceof AtlasReferenceIndirect(var use)) {
            a = brains.get(use);
        }
        return (AtlasReferenceCoordinate) a;
    }

    public @Nullable AtlasReference get(String atlas, String name) {
        var a = get(atlas);
        if (a == null) return null;
        var p = a.references.get(name);
        if (p == null) return null;
        var c = new Coordinate(p[0], p[1], p[2]);
        var f = p[3] != 0;
        return new AtlasReference(atlas, name, c, f);
    }

    public void add(AtlasReference reference) {
        var atlas = reference.atlasNams();
        var a = brains.get(atlas);

        AtlasReferenceCoordinate c;
        if (a == null) {
            c = new AtlasReferenceCoordinate();
            brains.put(atlas, c);
        } else {
            if ((c = get(atlas)) == null) throw new RuntimeException("broken structure");
        }

        var p = reference.coordinate();
        c.references.put(reference.name(), new int[]{
          (int) p.ap(), (int) p.dv(), (int) p.ml(), reference.flipAP() ? 1 : 0
        });
    }

}

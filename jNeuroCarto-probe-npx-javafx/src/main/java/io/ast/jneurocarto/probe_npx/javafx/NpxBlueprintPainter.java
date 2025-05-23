package io.ast.jneurocarto.probe_npx.javafx;

import java.util.Set;

import javafx.scene.paint.Color;

import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;
import io.ast.jneurocarto.javafx.blueprint.BlueprintPainter;
import io.ast.jneurocarto.javafx.blueprint.BlueprintPaintingService;
import io.ast.jneurocarto.probe_npx.ChannelMap;

import static io.ast.jneurocarto.probe_npx.NpxProbeDescription.*;

public class NpxBlueprintPainter implements BlueprintPainter<ChannelMap> {
    @Override
    public Set<Feature> supportedFeatures() {
        return Set.of(Feature.conflict);
    }

    @Override
    public void plotBlueprint(BlueprintPaintingService<ChannelMap> service) {
        var type = service.getChannelmap().type();
        var sc = (double) type.spacePerColumn();
        var sr = (double) type.spacePerRow();
        var offset = sc + sc * type.nColumnPerShank();

        service.setOffset(offset, 0);
        service.setCorner(sc / 2, sr / 2);

        if (service.hasFeature(Feature.conflict)) {
            plotConflictBlueprint(service);
        } else {
            plotDefaultBlueprint(service);
        }
    }

    private void plotConflictBlueprint(BlueprintPaintingService<ChannelMap> service) {
        setConflictBlueprint(new BlueprintToolkit<>(service.blueprint));
        service.addCategory(1, "conflict", Color.RED);
    }

    private void plotDefaultBlueprint(BlueprintPaintingService<ChannelMap> service) {
        service.blueprint.set(CATE_SET, CATE_FULL);
        service.addCategory(CATE_FULL, "full-", Color.GREEN);
        service.addCategory(CATE_HALF, "half-", Color.ORANGE);
        service.addCategory(CATE_QUARTER, "quarter-", Color.BLUE);
        service.addCategory(CATE_EXCLUDED, "excluded", Color.PINK);
    }

    private void setConflictBlueprint(BlueprintToolkit<ChannelMap> blueprint) {
        //XXX Unsupported Operation NpxBlueprintPainter.conflictBlueprint
        throw new UnsupportedOperationException();
    }
}

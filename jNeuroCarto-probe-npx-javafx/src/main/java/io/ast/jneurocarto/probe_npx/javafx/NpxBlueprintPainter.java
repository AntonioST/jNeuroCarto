package io.ast.jneurocarto.probe_npx.javafx;

import java.util.Set;

import javafx.scene.paint.Color;

import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;
import io.ast.jneurocarto.javafx.blueprint.BlueprintPainter;
import io.ast.jneurocarto.javafx.blueprint.BlueprintPaintingHandle;
import io.ast.jneurocarto.probe_npx.ChannelMap;

import static io.ast.jneurocarto.probe_npx.NpxProbeDescription.*;

public class NpxBlueprintPainter implements BlueprintPainter<ChannelMap> {

    @Override
    public Set<Feature> supportedFeatures() {
        return Set.of(Feature.conflict);
    }

    @Override
    public void changeFeature(BlueprintPaintingHandle<ChannelMap> handle) {
        if (handle.hasFeature(Feature.conflict)) {
            handle.addCategory(1, "conflict", Color.RED);
        } else {
            handle.addCategory(CATE_FULL, "full-", Color.GREEN);
            handle.addCategory(CATE_HALF, "half-", Color.ORANGE);
            handle.addCategory(CATE_QUARTER, "quarter-", Color.BLUE);
            handle.addCategory(CATE_EXCLUDED, "excluded", Color.PINK);
        }
    }

    @Override
    public void changeChannelmap(BlueprintPaintingHandle<ChannelMap> handle) {
        var type = handle.channelmap().type();
        var ss = (double) type.spacePerShank();
        var sc = (double) type.spacePerColumn();
        var sr = (double) type.spacePerRow();
        var offset = sc * type.nColumnPerShank();

        handle.setOffset(offset, 0);
        handle.setCorner(sc / 4, sr / 2);
        handle.setXonShankTransform((s, x) -> {
            var x0 = ss * s;
            return (x - x0) / 2 + x0 + sc / 4;
        });
    }

    @Override
    public void plotBlueprint(BlueprintPaintingHandle<ChannelMap> handle) {
        if (handle.hasFeature(Feature.conflict)) {
            plotConflictBlueprint(handle);
        } else {
            plotDefaultBlueprint(handle);
        }
    }

    private void plotConflictBlueprint(BlueprintPaintingHandle<ChannelMap> handle) {
        setConflictBlueprint(handle.blueprintToolkit());
    }

    private void plotDefaultBlueprint(BlueprintPaintingHandle<ChannelMap> handle) {
        var tool = handle.blueprintToolkit();
        tool.set(CATE_FULL, tool.mask(CATE_SET));
    }

    private void setConflictBlueprint(BlueprintToolkit<ChannelMap> blueprint) {
        var mc = blueprint.mask();
        var m0 = blueprint.mask(CATE_SET);
        var m1 = blueprint.mask(CATE_FULL);
        var m2 = blueprint.mask(CATE_HALF);
        var m4 = blueprint.mask(CATE_QUARTER);

        // selected conflict set/full categories
        var i0 = blueprint.invalid(mc, false);
        var r0 = m0.or(m1);
        var c0 = i0.and(r0);

        // set/full categories conflict half/quarter categories
        var i1 = blueprint.invalid(m0.or(m1));
        var r1 = m2.or(m4);
        var c1 = i1.and(r1);

        blueprint.set(CATE_UNSET);
        blueprint.set(1, c0.or(c1));
    }
}

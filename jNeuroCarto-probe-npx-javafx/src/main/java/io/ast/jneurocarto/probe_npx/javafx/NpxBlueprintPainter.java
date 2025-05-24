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
        var ss = (double) type.spacePerShank();
        var sc = (double) type.spacePerColumn();
        var sr = (double) type.spacePerRow();
        var offset = sc * type.nColumnPerShank();

        service.setOffset(offset, 0);
        service.setCorner(sc / 4, sr / 2);
        service.setXonShankTransform((s, x) -> {
            var x0 = ss * s;
            return (x - x0) / 2 + x0 + sc / 4;
        });

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
        var m0 = blueprint.mask(CATE_SET);
        var m1 = blueprint.mask(CATE_FULL);
        var m2 = blueprint.mask(CATE_HALF);
        var m4 = blueprint.mask(CATE_QUARTER);

        var i0 = blueprint.invalid(blueprint.mask().and(m0.or(m1)));
        var r0 = m1.or(m2).or(m4);
        var c0 = i0.and(r0);

        var i1 = blueprint.invalid(m0.or(m1));
        var r1 = m2.or(m4);
        var c1 = i1.and(r1);

        blueprint.set(CATE_UNSET);
        blueprint.set(1, c0.or(c1));
    }
}

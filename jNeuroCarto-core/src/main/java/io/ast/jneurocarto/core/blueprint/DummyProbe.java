package io.ast.jneurocarto.core.blueprint;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ProbeDescription;

/**
 * A dummy probe that only provide the number of shank, column and rows.
 * It will throw {@link UnsupportedOperationException} for most methods.
 */
@NullMarked
public class DummyProbe implements ProbeDescription<Object> {

    public final int nShanks;
    public final int nColumns;
    public final int nRows;

    public DummyProbe(int nShanks, int nColumns, int nRows) {
        this.nShanks = nShanks;
        this.nColumns = nColumns;
        this.nRows = nRows;
    }

    @Override
    public List<String> supportedProbeType() {
        return List.of("dummy");
    }

    @Override
    public String probeTypeDescription(String code) {
        return "dummy";
    }

    @Override
    public List<String> availableStates() {
        return List.of();
    }

    @Override
    public Map<Integer, String> allStates() {
        return Map.of();
    }

    @Override
    public List<String> availableCategories() {
        return List.of();
    }

    @Override
    public Map<Integer, String> allCategories() {
        return Map.of();
    }

    @Override
    public List<String> channelMapFileSuffix() {
        return List.of();
    }

    @Override
    public Object load(Path file) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(Path file, Object chmap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable String channelmapCode(Object chmap) {
        return "test";
    }

    @Override
    public Object newChannelmap(String code) {
        return new Object();
    }

    @Override
    public Object copyChannelmap(Object chmap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String despChannelmap(@Nullable Object chmap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ElectrodeDescription> allElectrodes(String code) {
        var ret = new ArrayList<ElectrodeDescription>();
        for (int i = 0; i < nShanks; i++) {
            for (int r = 0; r < nRows; r++) {
                for (int c = 0; c < nColumns; c++) {
                    var e = c + r * nColumns + i * nColumns * nRows;
                    ret.add(new ElectrodeDescription(i, c, r, e, 0));
                }
            }
        }
        return ret;
    }

    @Override
    public List<ElectrodeDescription> allChannels(Object chmap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ElectrodeDescription> allChannels(Object chmap, Collection<ElectrodeDescription> electrodes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean validateChannelmap(Object chmap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @Nullable ElectrodeDescription addElectrode(Object chmap, ElectrodeDescription e, boolean force) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeElectrode(Object chmap, ElectrodeDescription e, boolean force) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ElectrodeDescription> clearElectrodes(Object chmap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ElectrodeDescription copyElectrode(ElectrodeDescription e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isElectrodeCompatible(Object chmap, ElectrodeDescription e1, ElectrodeDescription e2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ElectrodeDescription> loadBlueprint(Path file) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ElectrodeDescription> loadBlueprint(Path file, Object chmap) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveBlueprint(Path file, List<ElectrodeDescription> electrodes) throws IOException {
        throw new UnsupportedOperationException();
    }
}

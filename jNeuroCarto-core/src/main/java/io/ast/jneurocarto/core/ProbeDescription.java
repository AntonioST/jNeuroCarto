package io.ast.jneurocarto.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface ProbeDescription<T> {

    /**
     * electrode default state
     */
    int STATE_UNUSED = 0;
    /**
     * electrode is selected as readout channel
     */
    int STATE_USED = 1;
    /**
     * electrode is disabled
     */
    int STATE_DISABLED = 2;

    /**
     * electrode initial category
     */
    int CATE_UNSET = 0;

    /**
     * electrode pre-select category. Electrode must be selected
     */
    int CATE_SET = 1;

    /**
     * electrode excluded category. Electrode must not be selected
     */
    int CATE_EXCLUDED = 2;

    /**
     * electrode low-priority category.
     */
    int CATE_LOW = 3;

    static @Nullable ProbeDescription<?> getProbeDescription(String family) {
        for (var provider : ServiceLoader.load(ProbeProvider.class)) {
            if (provider.provideProbeFamily().equals(family)) {
                return provider.getProbeDescription();
            }
        }
        return null;
    }

    /**
     * All supported probe type.
     *
     * @return
     */
    List<String> supportedProbeType();

    String probeTypeDescription(String code);

    List<String> availableStates();

    List<String> availableCategories();

    List<String> channelMapFileSuffix();

    T load(Path file) throws IOException;

    void save(Path file, T chmap) throws IOException;

    @Nullable
    default String channelmapCode(String code) {
        return supportedProbeType().contains(code) ? code : null;
    }

    @Nullable
    String channelmapCode(Object chmap);

    T newChannelmap(String code);

    default T newChannelmap(T chmap) {
        return newChannelmap(Objects.requireNonNull(channelmapCode(chmap)));
    }

    T copyChannelmap(T chmap);

    String despChannelmap(@Nullable T chmap);

    List<ElectrodeDescription> allElectrodes(String code);

    default List<ElectrodeDescription> allElectrodes(T chmap) {
        var code = channelmapCode(chmap);
        if (code == null) throw new IllegalArgumentException("Not a channelmap of " + getClass().getName());
        return allElectrodes(code);
    }

    List<ElectrodeDescription> allChannels(T chmap);

    List<ElectrodeDescription> allChannels(T chmap, List<ElectrodeDescription> subset);

    boolean validateChannelmap(T chmap);

    default @Nullable ElectrodeDescription getElectrode(List<ElectrodeDescription> electrodes, Object identify) {
        if (identify instanceof ElectrodeDescription desp) {
            return getElectrode(electrodes, desp.electrode());
        }

        for (var electrode : electrodes) {
            if (Objects.equals(identify, electrode)) {
                return electrode;
            }
        }

        return null;
    }

    default @Nullable ElectrodeDescription addElectrode(T chmap, ElectrodeDescription e) {
        return addElectrode(chmap, e, false);
    }

    @Nullable
    ElectrodeDescription addElectrode(T chmap, ElectrodeDescription e, boolean force);

    default boolean removeElectrode(T chmap, ElectrodeDescription e) {
        return removeElectrode(chmap, e, true);
    }

    boolean removeElectrode(T chmap, ElectrodeDescription e, boolean force);

    List<ElectrodeDescription> clearElectrodes(T chmap);

    ElectrodeDescription copyElectrode(ElectrodeDescription e);

    default List<ElectrodeDescription> copyElectrodes(Collection<ElectrodeDescription> electrodes) {
        return electrodes.stream().map(this::copyElectrode).toList();
    }

    boolean isElectrodeCompatible(T chmap, ElectrodeDescription e1, ElectrodeDescription e2);

    default List<ElectrodeDescription> getInvalidElectrodes(T chmap, ElectrodeDescription e, Collection<ElectrodeDescription> electrodes) {
        return electrodes.stream().filter(it -> !isElectrodeCompatible(chmap, e, it)).toList();
    }

    default List<ElectrodeDescription> getInvalidElectrodes(T chmap, Collection<ElectrodeDescription> e, Collection<ElectrodeDescription> electrodes) {
        return electrodes.stream()
          .filter(it -> e.stream().anyMatch(r -> !isElectrodeCompatible(chmap, r, it)))
          .toList();
    }

    default List<String> getElectrodeSelectors() {
        var ret = new ArrayList<String>();
        for (var provider : ServiceLoader.load(ElectrodeSelectorProvider.class)) {
            ret.addAll(provider.name(this));
        }
        return ret;
    }

    default <D extends ProbeDescription<?>> ElectrodeSelector<D, ?> newElectrodeSelector(String name) {
        for (var provider : ServiceLoader.load(ElectrodeSelectorProvider.class)) {
            if (provider.name(this).contains(name)) {
                return provider.newSelector(name);
            }
        }
        throw new IllegalArgumentException("");
    }

    void loadBlueprint(Path file, List<ElectrodeDescription> electrodes) throws IOException;

    void saveBlueprint(Path file, List<ElectrodeDescription> electrodes) throws IOException;

}

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

    static List<String> listProbeDescriptions() {
        return ProbeProviders.listProbeDescriptions();
    }

    static @Nullable ProbeDescription<?> getProbeDescription(String family) {
        return ProbeProviders.getProbeDescription(family);
    }

    /**
     * All supported probe type.
     *
     * @return list of code names.
     */
    List<String> supportedProbeType();

    String probeTypeDescription(String code);

    List<String> availableStates();

    Map<Integer, String> allStates();

    default @Nullable String stateOf(int code) {
        return allStates().get(code);
    }

    default OptionalInt stateOf(String desp) {
        for (var e : allStates().entrySet()) {
            if (e.getValue().equals(desp)) return OptionalInt.of(e.getKey());
        }
        return OptionalInt.empty();
    }

    List<String> availableCategories();

    Map<Integer, String> allCategories();

    default @Nullable String categoryOf(int code) {
        return allCategories().get(code);
    }

    default OptionalInt categoryOf(String desp) {
        for (var e : allCategories().entrySet()) {
            if (e.getValue().equals(desp)) return OptionalInt.of(e.getKey());
        }
        return OptionalInt.empty();
    }

    List<String> channelMapFileSuffix();

    T load(Path file) throws IOException;

    void save(Path file, T chmap) throws IOException;

    /**
     * Is the channelmap code supported.
     *
     * @param code
     * @return {@code code} itself. {@code null} if the code is not supported.
     */
    @Nullable
    default String channelmapCode(String code) {
        return supportedProbeType().contains(code) ? code : null;
    }

    /**
     * Return the code of the channelmap.
     *
     * @param chmap any channelmap
     * @return code from {@link #supportedProbeType()}. {@code null} if the channelmap is not supported.
     */
    @Nullable
    String channelmapCode(Object chmap);

    T newChannelmap(String code);

    default T newChannelmap(T chmap) {
        return newChannelmap(Objects.requireNonNull(channelmapCode(chmap)));
    }

    T copyChannelmap(T chmap);

    String despChannelmap(@Nullable T chmap);

    /**
     * Get all possible electrode set for the given channelmap code.
     *
     * @param code channelmap code, that came from {@link #supportedProbeType()}
     * @return
     */
    List<ElectrodeDescription> allElectrodes(String code);

    /**
     * Get all possible electrode set for the given channelmap code.
     *
     * @param chmap channelmap
     * @return
     */
    default List<ElectrodeDescription> allElectrodes(T chmap) {
        var code = channelmapCode(chmap);
        if (code == null) throw new IllegalArgumentException("Not a channelmap of " + getClass().getName());
        return allElectrodes(code);
    }

    /**
     * Get a list of a selected electrodes in the given channelmap.
     *
     * @param chmap channelmap
     * @return
     */
    List<ElectrodeDescription> allChannels(T chmap);

    /**
     * Get a list of a selected electrodes in the given channelmap.
     *
     * @param chmap      channelmap
     * @param electrodes source electrode set.
     * @return a sub-list of {@code electrodes}, but ordering by its channel identify.
     */
    List<ElectrodeDescription> allChannels(T chmap, Collection<ElectrodeDescription> electrodes);

    /// Is it a valid and completed channelmap?
    ///
    /// A valid and completed channelmap means:
    ///
    /// * This channelmap is able to save into a file
    /// * no electrode pair will break the probe restriction ({@link #isElectrodeCompatible(Object, ElectrodeDescription, ElectrodeDescription)}}).
    /// * The saved file can be read by other software or machines without any error or any mis-located electrode.
    ///
    /// @param chmap
    /// @return
    boolean validateChannelmap(T chmap);

    /**
     * Get an electrode from a set with a given identify *e*.
     *
     * @param electrodes source set.
     * @param identify   electrode identify that equals to the {@link ElectrodeDescription#electrode()}
     * @return found electrode.
     */
    default Optional<ElectrodeDescription> getElectrode(Collection<ElectrodeDescription> electrodes, Object identify) {
        if (identify instanceof ElectrodeDescription desp) {
            return getElectrode(electrodes, desp.electrode());
        }

        for (var electrode : electrodes) {
            if (Objects.equals(identify, electrode.electrode())) {
                return Optional.of(electrode);
            }
        }

        return Optional.empty();
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

    /// Does electrode {@code e1} and {@code e2} can be used in the {@code chmap} at the same time?
    ///
    /// This method's implementation should follow the rules in most cases:
    ///
    /// * `isElectrodeCompatible(M, e, e)` should return `false`
    /// * `isElectrodeCompatible(M, e1, e2) == isElectrodeCompatible(M, e2, e1)`
    ///
    /// @param chmap
    /// @param e1
    /// @param e2
    /// @return
    boolean isElectrodeCompatible(T chmap, ElectrodeDescription e1, ElectrodeDescription e2);

    /**
     * Collect the invalid electrodes that an electrode from {@code electrodes} will break the
     * {@link #isElectrodeCompatible(Object, ElectrodeDescription, ElectrodeDescription)}
     * with the electrode {@code e}.
     *
     * @param chmap
     * @param e
     * @param electrodes testing electrode set
     * @return a sub-list of {@code electrodes}.
     */
    default List<ElectrodeDescription> getInvalidElectrodes(T chmap, ElectrodeDescription e, Collection<ElectrodeDescription> electrodes) {
        return electrodes.stream().filter(it -> !isElectrodeCompatible(chmap, e, it)).toList();
    }

    /**
     * Collect the invalid electrodes that an electrode from {@code electrodes} will break the
     * {@link #isElectrodeCompatible(Object, ElectrodeDescription, ElectrodeDescription)}
     * with any electrode from {@code e}.
     *
     * @param chmap
     * @param e
     * @param electrodes testing electrode set
     * @return a sub-list of {@code electrodes}.
     */
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

    default ElectrodeSelector newElectrodeSelector(String name) {
        for (var provider : ServiceLoader.load(ElectrodeSelectorProvider.class)) {
            if (provider.name(this).contains(name)) {
                return provider.newSelector(name);
            }
        }
        throw new IllegalArgumentException("");
    }

    List<ElectrodeDescription> loadBlueprint(Path file) throws IOException;

    List<ElectrodeDescription> loadBlueprint(Path file, T chmap) throws IOException;

    void saveBlueprint(Path file, List<ElectrodeDescription> electrodes) throws IOException;

}

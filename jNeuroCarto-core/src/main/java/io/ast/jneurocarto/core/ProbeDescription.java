package io.ast.jneurocarto.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// A probe description for a family of probe kinds.
///
/// ### service
///
/// It is provided by [ProbeProvider]. Check it to get more information of
/// how to register a [ProbeDescription].
///
/// @param <T> channelmap
/// @see ProbeProvider
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
     * Predefined electrode initial category
     */
    int CATE_UNSET = 0;

    /**
     * Predefined electrode pre-select category. Electrode must be selected
     */
    int CATE_SET = 1;

    /**
     * Predefined electrode excluded category. Electrode must not be selected
     */
    int CATE_EXCLUDED = 2;

    /**
     * Predefined electrode low-priority category.
     */
    int CATE_LOW = 3;

    /*===========*
     * factories *
     *===========*/

    /**
     * {@return found probe description in the classpath.}
     *
     * @see ProbeProviders#listProbeDescriptions()
     */
    static List<String> listProbeDescriptions() {
        return ProbeProviders.listProbeDescriptions();
    }

    /**
     * Create a probe description.
     *
     * @param family probe family name.
     * @return a probe description. {@code null} if not found.
     * @see ProbeProviders#getProbeDescription(String)
     */
    static @Nullable ProbeDescription<?> getProbeDescription(String family) {
        return ProbeProviders.getProbeDescription(family);
    }

    /*===================*
     * probe information *
     *===================*/

    /**
     * All supported probe type.
     * <br>
     * It is used to dynamic generate the button in the application.
     *
     * @return list of code name of probes.
     */
    List<String> supportedProbeType();

    /**
     * The full name of the code.
     *
     * @param code probe code name, or other supported aliases.
     * @return the full name of the code.
     */
    String probeTypeDescription(String code);

    /**
     * a list of manipulatable states' name.
     * <br>
     * It is used to dynamic generate the button in the application.
     *
     * @return a list of manipulatable states' name.
     */
    List<String> availableStates();

    /**
     * {@return a map of all state code to the name of state}
     */
    Map<Integer, String> allStates();

    /**
     * The name of the state.
     *
     * @param code state code
     * @return the name of the state.
     */
    default @Nullable String stateOf(int code) {
        return allStates().get(code);
    }

    /**
     * The code of the state
     *
     * @param desp state name
     * @return the code of the state.
     */
    default OptionalInt stateOf(String desp) {
        for (var e : allStates().entrySet()) {
            if (e.getValue().equals(desp)) return OptionalInt.of(e.getKey());
        }
        return OptionalInt.empty();
    }

    /**
     * a list of manipulatable categories' name.
     * <br>
     * It is used to dynamic generate the button in the application.
     *
     * @return a list of manipulatable categories' name.
     */
    List<String> availableCategories();

    /**
     * {@return a map of all category code to the name of the category}
     */
    Map<Integer, String> allCategories();

    /**
     * The name of the category.
     *
     * @param code category code
     * @return the name of the category.
     */
    default @Nullable String categoryOf(int code) {
        return allCategories().get(code);
    }

    /**
     * The code of the category.
     *
     * @param desp category name
     * @return the code of the category.
     */
    default OptionalInt categoryOf(String desp) {
        for (var e : allCategories().entrySet()) {
            if (e.getValue().equals(desp)) return OptionalInt.of(e.getKey());
        }
        return OptionalInt.empty();
    }

    /*====================*
     * channelmap file IO *
     *====================*/

    /**
     * The filename extension for supported channelmap.
     * <br>
     * The first suffix in returned list is considered the primary format.
     *
     * @return list of file extensions, like {@code ".imro"} for the Neuropixels probe.
     */
    List<String> channelMapFileSuffix();

    /**
     * Load a channelmap file.
     *
     * @param file channelmap filepath
     * @return channelmap instance
     * @throws RuntimeException If file is not supported.
     * @throws IOException      any IO error.
     */
    T load(Path file) throws IOException;

    /**
     * Save a channelmap into a file.
     *
     * @param file  channelmap filepath
     * @param chmap channelmap instance
     * @throws RuntimeException errors when serializing the channelmap.
     * @throws IOException      any IO error.
     */
    void save(Path file, T chmap) throws IOException;

    /*============*
     * channelmap *
     *============*/

    /**
     * Identify a given channelmap code.
     *
     * @param code channelmap code or alias.
     * @return {@code code}. {@code null} if the code is not supported.
     */
    @Nullable
    default String channelmapCode(String code) {
        return supportedProbeType().contains(code) ? code : null;
    }

    /**
     * Identify a given channelmap, and return the corresponding code.
     *
     * @param chmap any channelmap
     * @return code from {@link #supportedProbeType()}. {@code null} if the channelmap is not supported.
     */
    @Nullable
    String channelmapCode(Object chmap);

    /**
     * Create a new, empty channelmap instance.
     *
     * @param code channelmap code or alias.
     * @return a channelmap instance
     * @throws RuntimeException code not supported
     */
    T newChannelmap(String code);

    /**
     * Create a new, empty channelmap instance with the same code of {@code chmap}.
     * <br>
     * It does not do the copy action. If you want to copy a channelmap instance,
     * use {@link #copyChannelmap(Object)} instead.
     *
     * @param chmap a reference channelmap instance
     * @return a channelmap instance
     * @throws RuntimeException unsupported {@code chmap}
     */
    default T newChannelmap(T chmap) {
        return newChannelmap(Objects.requireNonNull(channelmapCode(chmap), "unsupported"));
    }

    /**
     * Copy a channelmap instance, including properties of electrodes.
     *
     * @param chmap a reference channelmap instance
     * @return a channelmap instance
     */
    T copyChannelmap(T chmap);

    /**
     * A description for displaying the status of a channelmap instance.
     *
     * @param chmap a channelmap instance. {@code null} represent nothing.
     * @return a description
     */
    String despChannelmap(@Nullable T chmap);

    /// Is it a valid and completed channelmap?
    ///
    /// A valid and completed channelmap means:
    ///
    /// * This channelmap is able to save into a file
    /// * no electrode pair will break the probe restriction ([isElectrodeCompatible][#isElectrodeCompatible(Object, ElectrodeDescription, ElectrodeDescription)]).
    /// * The saved file can be read by other software or machines without any error or any mis-located electrode.
    ///
    /// @param chmap a channelmap instance
    /// @return `chmap` is a valid channelmap.
    boolean validateChannelmap(T chmap);

    /*============*
     * electrodes *
     *============*/

    /**
     * Get all possible electrode set for the given channelmap code.
     *
     * @param code channelmap code, that came from {@link #supportedProbeType()}
     * @return a list of {@link ElectrodeDescription}
     */
    List<ElectrodeDescription> allElectrodes(String code);

    /**
     * Get all possible electrode set for the given channelmap code.
     *
     * @param chmap channelmap
     * @return a list of {@link ElectrodeDescription}
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
     * @return a list of {@link ElectrodeDescription}
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

    /**
     * Get an electrode from a set with a given identify *e*.
     *
     * @param electrodes source electrode collection.
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

    /*=====================*
     * electrode selection *
     *=====================*/

    /**
     * Add an electrode {@code e} into {@code chmap}.
     *
     * @param chmap a channelmap instance
     * @param e     an electrode
     * @return The description of the added electrode. {@code null} if the action was failed.
     */
    default @Nullable ElectrodeDescription addElectrode(T chmap, ElectrodeDescription e) {
        return addElectrode(chmap, e, false);
    }

    /**
     * Add an electrode {@code e} into {@code chmap}.
     *
     * @param chmap a channelmap instance
     * @param e     an electrode
     * @param force force add. Overwrite by default, and ignore any error.
     * @return The description of the added electrode. {@code null} if the action was failed.
     */
    @Nullable
    ElectrodeDescription addElectrode(T chmap, ElectrodeDescription e, boolean force);

    /**
     * Remove an electrode {@code e} from the {@code chmap}.
     *
     * @param chmap a channelmap instance
     * @param e     an electrode
     * @return Was the electrode got removed.
     */
    default boolean removeElectrode(T chmap, ElectrodeDescription e) {
        return removeElectrode(chmap, e, true);
    }

    /**
     * Remove an electrode {@code e} from the {@code chmap}.
     *
     * @param chmap a channelmap instance
     * @param e     an electrode
     * @param force force remove and ignore any error.
     * @return Was the electrode got removed.
     */
    boolean removeElectrode(T chmap, ElectrodeDescription e, boolean force);

    /**
     * Remove all electrodes from the {@code chmap}.
     *
     * @param chmap a channelmap instance
     * @return removed electrodes
     */
    List<ElectrodeDescription> clearElectrodes(T chmap);

    /**
     * copy electrode data from {@code e}.
     *
     * @param e an electrode.
     * @return a copied electrode.
     */
    ElectrodeDescription copyElectrode(ElectrodeDescription e);

    /**
     * copy a set of electrode data from {@code electrodes}.
     *
     * @param electrodes an electrode collection
     * @return Copied electrodes.
     */
    default List<ElectrodeDescription> copyElectrodes(Collection<ElectrodeDescription> electrodes) {
        return electrodes.stream().map(this::copyElectrode).toList();
    }

    /// Does electrode `e1` and `e2` can be used in the `chmap` at the same time?
    ///
    /// This method's implementation should follow the rules in most cases:
    ///
    /// * `isElectrodeCompatible(M, e, e)` should return `false`
    /// * `isElectrodeCompatible(M, e1, e2) == isElectrodeCompatible(M, e2, e1)`
    ///
    /// @param chmap a channelmap instance
    /// @param e1    an electrode.
    /// @param e2    an electrode.
    /// @return `true` when `e1` and `e2` are compatible.
    boolean isElectrodeCompatible(T chmap, ElectrodeDescription e1, ElectrodeDescription e2);

    /**
     * Collect the invalid electrodes that an electrode from {@code electrodes} will break the
     * {@link #isElectrodeCompatible(Object, ElectrodeDescription, ElectrodeDescription) isElectrodeCompatible}
     * with the electrode {@code e}.
     *
     * @param chmap
     * @param e          a reference electrode.
     * @param electrodes testing electrode set
     * @return a sub-list of {@code electrodes}.
     */
    default List<ElectrodeDescription> getInvalidElectrodes(T chmap, ElectrodeDescription e, Collection<ElectrodeDescription> electrodes) {
        return electrodes.stream().filter(it -> !isElectrodeCompatible(chmap, e, it)).toList();
    }

    /**
     * Collect the invalid electrodes that an electrode from {@code electrodes} will break the
     * {@link #isElectrodeCompatible(Object, ElectrodeDescription, ElectrodeDescription) isElectrodeCompatible}
     * with any electrode from {@code e}.
     *
     * @param chmap      a channelmap instance
     * @param e          a reference electrode collection.
     * @param electrodes testing electrode set
     * @return a sub-list of {@code electrodes}.
     */
    default List<ElectrodeDescription> getInvalidElectrodes(T chmap, Collection<ElectrodeDescription> e, Collection<ElectrodeDescription> electrodes) {
        return electrodes.stream()
            .filter(it -> e.stream().anyMatch(r -> !isElectrodeCompatible(chmap, r, it)))
            .toList();
    }

    /*====================*
     * electrode selector *
     *====================*/

    /**
     * {@return a list of found and supported electrode selectors}
     *
     * @see ProbeProviders#getElectrodeSelectors(ProbeDescription)
     */
    default List<String> getElectrodeSelectors() {
        return ProbeProviders.getElectrodeSelectors(this);
    }

    /**
     * Create a new electrode selector.
     *
     * @param name selector name
     * @return a new electrode selector.
     * @see ProbeProviders#newElectrodeSelector(ProbeDescription, String)
     */
    default ElectrodeSelector newElectrodeSelector(String name) {
        return ProbeProviders.newElectrodeSelector(this, name);
    }

    /*===================*
     * blueprint file IO *
     *===================*/

    /**
     * load blueprint from {@code file}.
     *
     * @param file blueprint file
     * @return a list of electrodes.
     * @throws RuntimeException filename not supported
     * @throws IOException      any io error
     */
    List<ElectrodeDescription> loadBlueprint(Path file) throws IOException;

    /**
     * load blueprint from {@code file} for channelmap specific.
     *
     * @param file  blueprint file
     * @param chmap a channelmap instance
     * @return a list of electrodes.
     * @throws RuntimeException filename not supported
     * @throws IOException      any io error
     */
    List<ElectrodeDescription> loadBlueprint(Path file, T chmap) throws IOException;

    /**
     * save blueprint info {@code file}.
     *
     * @param file       blueprint file
     * @param electrodes a list of electrodes.
     * @throws RuntimeException filename not supported
     * @throws IOException      any io error
     */
    void saveBlueprint(Path file, List<ElectrodeDescription> electrodes) throws IOException;

    /*================*
     * probe geometry *
     *================*/

    /**
     * Get shank coordinate.
     *
     * @param code channelmap code
     * @return shank coordinate
     */
    default ShankCoordinate getShankCoordinate(String code) {
        return ShankCoordinate.ZERO;
    }

    /**
     * Get shank coordinate.
     *
     * @param chmap channelmap instance
     * @return shank coordinate
     * @throws RuntimeException if channelmap not supported
     */
    default ShankCoordinate getShankCoordinate(T chmap) {
        var code = Objects.requireNonNull(channelmapCode(chmap), "unknown probe");
        return getShankCoordinate(code);
    }
}

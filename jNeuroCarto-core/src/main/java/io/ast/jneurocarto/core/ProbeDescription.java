package io.ast.jneurocarto.core;

import java.util.ServiceLoader;

import org.jspecify.annotations.Nullable;

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

    /**
     * All supported probe type.
     *
     * @param <E>
     * @return
     */
    <E extends Enum<E>> Class<? extends E> supportedProbeType();

    static @Nullable ProbeDescription<?> getProbeDescription(String family) {
        for (var provider : ServiceLoader.load(ProbeProvider.class)) {
            if (provider.provideProbeFamily().equals(family)) {
                return provider.getProbeDescription();
            }
        }
        return null;
    }
}

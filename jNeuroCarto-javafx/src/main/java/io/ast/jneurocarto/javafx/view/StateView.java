package io.ast.jneurocarto.javafx.view;

import java.lang.reflect.ParameterizedType;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Indicate this [Plugin] has state `S` can be (re)store from/to `.config.json` file.
///
/// @param <S> state class which is json serializable (via jackson).
@NullMarked
public interface StateView<S> {

    /// Get the class of `S` from class declaration.
    /// @return class `S`.
    default Class<S> getStateClass() {
        for (var intf : getClass().getGenericInterfaces()) {
            if (intf instanceof Class<?> clazz) {
                if (clazz == StateView.class || clazz == GlobalStateView.class) {
                    throw new RuntimeException("inherit StateView without giving StateClass S.");
                }
            } else if (intf instanceof ParameterizedType pt) {
                var clazz = pt.getRawType();
                if (clazz == StateView.class || clazz == GlobalStateView.class) {
                    var ret = pt.getActualTypeArguments()[0];
                    if (ret instanceof Class<?>) {
                        return (Class<S>) ret;
                    }
                }
            }
        }
        throw new RuntimeException("cannot retrieve StateClass S for " + getClass().getSimpleName());
    }

    /// Store information in the state `S`.
    /// @return state instance.
    @Nullable
    S getState();

    /// Restore plugin state from the state instance.
    /// @param state state instance.
    void restoreState(@Nullable S state);
}

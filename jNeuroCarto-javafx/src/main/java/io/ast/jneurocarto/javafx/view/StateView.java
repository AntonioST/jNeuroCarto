package io.ast.jneurocarto.javafx.view;

import java.lang.reflect.ParameterizedType;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface StateView<S> {
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

    @Nullable
    S getState();

    void restoreState(@Nullable S state);
}

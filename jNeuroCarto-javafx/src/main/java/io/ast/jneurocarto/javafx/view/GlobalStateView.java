package io.ast.jneurocarto.javafx.view;

/// Indicate this [Plugin] has a global state `S` can be (re)store from/to user json config file.
///
/// @param <S> state class which is json serializable (via jackson).
public interface GlobalStateView<S> extends StateView<S> {
}

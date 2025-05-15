module io.ast.jneurocarto.javafx.base {
    requires static org.jspecify;

    requires org.slf4j;

    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;

    requires io.ast.jneurocarto.config;
    requires javafx.graphics;

    exports io.ast.jneurocarto.javafx.base;
}
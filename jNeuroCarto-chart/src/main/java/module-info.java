module io.ast.jneurocarto.chart {

    requires static org.jspecify;
    requires org.slf4j;
    requires hilla.endpoint;
    requires flow.server;
    requires flow.html.components;
    requires vaadin.ordered.layout.flow;
    requires spring.boot;
    requires spring.boot.autoconfigure;

    opens io.ast.jneurocarto.chart;
    opens io.ast.jneurocarto.chart.app;
}
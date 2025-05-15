package io.ast.jneurocarto.chart;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Div;

@Tag("plotly-chart")
@NpmPackage(value = "plotly.js", version = "3.0.1")
@JsModule("jneurocarto/jneurocarto-chart.ts")
public class Chart extends Div {
}

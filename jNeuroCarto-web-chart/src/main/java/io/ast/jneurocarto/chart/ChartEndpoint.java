package io.ast.jneurocarto.chart;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.hilla.Endpoint;

@Endpoint
@AnonymousAllowed
public class ChartEndpoint {

    public String test() {
        return "hello world";
    }
}

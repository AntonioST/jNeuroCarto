package io.ast.jneurocarto.chart.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import io.ast.jneurocarto.chart.Chart;

@SpringBootApplication
@Route("")
@PageTitle("jNeuroCarto-Chart")
public class Index extends VerticalLayout {
    public Index() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        add(new Chart());
    }

    public static void main(String[] args) {
        SpringApplication.run(Index.class, args);
    }
}

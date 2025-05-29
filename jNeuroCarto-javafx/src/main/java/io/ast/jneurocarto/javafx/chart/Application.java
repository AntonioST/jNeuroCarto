package io.ast.jneurocarto.javafx.chart;

import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {


    public interface ApplicationContent {
        default void setup(Scene scene) {
        }

        default void setup(VBox layout) {
        }

        void setup(InteractionXYChart chart);
    }

    private final ApplicationContent content;
    private final Logger log = LoggerFactory.getLogger(Application.class);

    public Application(ApplicationContent content) {
        this.content = content;
    }

    /*===========*
     * UI Layout *
     *===========*/

    private Stage stage;
    private Scene scene;
    private VBox layout;
    private InteractionXYChart chart;

    public void start(Stage stage) {
        this.stage = stage;
        log.debug("start");
        stage.setTitle("InteractionXYChart");
        stage.setScene(scene());

        content.setup(layout);
        content.setup(scene);

        stage.sizeToScene();
        stage.show();

        content.setup(chart);
    }

    private Scene scene() {
        layout = root();
        scene = new Scene(layout);
        return scene;
    }

    private VBox root() {
        chart = new InteractionXYChart();
        chart.setResetAxesBoundaries(0, 100, 0, 100);
        chart.resetAxesBoundaries();

        chart.setMinWidth(800);
        chart.setMinHeight(800);

        return new VBox(chart);
    }
}

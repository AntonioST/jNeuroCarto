package io.ast.jneurocarto.core.blueprint;

import java.util.List;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static io.ast.jneurocarto.core.blueprint.BlueprintAssertion.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BlueprintToolkitTest {


    @Test
    @Order(0)
    public void blueprintToString() {
        var bp = fromShape(1, 5, 2);
        bp.from(new int[]{
            1, 1,
            2, 2,
            0, 0,
            4, 4,
            5, 5,
        });
        assertEquals("""
            1 1
            2 2
            0 0
            4 4
            5 5
            """, bp.toString());

        bp = fromShape(2, 5, 2);
        bp.from(new int[]{
            1, 1,
            2, 2,
            0, 0,
            4, 4,
            5, 5,
            // ---
            5, 5,
            4, 4,
            0, 0,
            2, 2,
            1, 1,
        });

        assertEquals("""
            1 1|5 5
            2 2|4 4
            0 0|0 0
            4 4|2 2
            5 5|1 1
            """, bp.toString());
    }

    @Test
    public void move() {
        var bp = fromShape(2, 3, 2);
        bp.from(new int[]{
            2, 0,
            1, 2,
            0, 1,
            //---
            4, 0,
            3, 4,
            0, 3,
        });

        bp.move(-1);
        assertBlueprintEquals(bp, new int[]{
            1, 2,
            0, 1,
            0, 0,
            //---
            3, 4,
            0, 3,
            0, 0,
        });

        bp.move(1);
        assertBlueprintEquals(bp, new int[]{
            0, 0,
            1, 2,
            0, 1,
            //---
            0, 0,
            3, 4,
            0, 3,
        });
    }

    @Test
    public void moveBlueprint() {
        var bp = fromShape(2, 3, 2);
        var blueprint = new int[]{
            2, 0,
            1, 2,
            0, 1,
            //---
            4, 0,
            3, 4,
            0, 3,
        };
        var o = blueprint.clone();

        assertBlueprintEquals(bp, new int[]{
            1, 2,
            0, 1,
            0, 0,
            //---
            3, 4,
            0, 3,
            0, 0,
        }, bp.move(blueprint, -1));
        assertArrayEquals(o, blueprint);

        assertBlueprintEquals(bp, new int[]{
            0, 0,
            2, 0,
            1, 2,
            //---
            0, 0,
            4, 0,
            3, 4,
        }, bp.move(blueprint, 1));
        assertArrayEquals(o, blueprint);
    }

    @Test
    public void noMoveBlueprint() {
        var bp = fromShape(2, 3, 2);
        var blueprint = new int[]{
            2, 0,
            1, 2,
            0, 1,
            //---
            4, 0,
            3, 4,
            0, 3,
        };
        var o = blueprint.clone();

        assertBlueprintEquals(bp, new int[]{
            2, 0,
            1, 2,
            0, 1,
            //---
            4, 0,
            3, 4,
            0, 3,
        }, bp.move(blueprint, 0));
        assertArrayEquals(o, blueprint);
    }

    @Test
    public void moveBlueprintCategory() {
        var bp = fromShape(2, 3, 2);
        var blueprint = new int[]{
            2, 0,
            1, 2,
            0, 1,
            //---
            4, 0,
            3, 4,
            0, 3,
        };
        var o = blueprint.clone();

        assertBlueprintEquals(bp, new int[]{
            0, 2,
            1, 0,
            0, 1,
            //---
            4, 0,
            3, 4,
            0, 3,
        }, bp.move(blueprint, -1, 2));
        assertArrayEquals(o, blueprint);

        assertBlueprintEquals(bp, new int[]{
            0, 0,
            2, 0,
            0, 2,
            //---
            4, 0,
            3, 4,
            0, 3,
        }, bp.move(blueprint, 1, 2));
        assertArrayEquals(o, blueprint);

        assertBlueprintEquals(bp, new int[]{
            2, 0,
            0, 2,
            1, 0,
            //---
            4, 0,
            3, 4,
            0, 3,
        }, bp.move(blueprint, 1, 1));
        assertArrayEquals(o, blueprint);
    }

    @Test
    public void moveWithDirection() {
        var bp = fromShape(1, 6, 6);
        var reset = new int[]{
            0, 0, 0, 0, 0, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 0, 0, 0, 0, 0,
        };

        bp.from(reset);
        bp.move(new BlueprintToolkit.Movement(0, 0));
        assertBlueprintEquals(bp, reset);

        bp.from(reset);
        bp.move(new BlueprintToolkit.Movement(1, 0));
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 1, 1, 1, 1,
            0, 0, 1, 1, 1, 1,
            0, 0, 1, 1, 1, 1,
            0, 0, 1, 1, 1, 1,
            0, 0, 0, 0, 0, 0,
        });

        bp.from(reset);
        bp.move(new BlueprintToolkit.Movement(-1, 0));
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            1, 1, 1, 1, 0, 0,
            1, 1, 1, 1, 0, 0,
            1, 1, 1, 1, 0, 0,
            1, 1, 1, 1, 0, 0,
            0, 0, 0, 0, 0, 0,
        });

        bp.from(reset);
        bp.move(new BlueprintToolkit.Movement(0, 1));
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
        });

        bp.from(reset);
        bp.move(new BlueprintToolkit.Movement(0, -1));
        assertBlueprintEquals(bp, new int[]{
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
        });

        bp.from(reset);
        bp.move(new BlueprintToolkit.Movement(1, 1));
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 1, 1, 1, 1,
            0, 0, 1, 1, 1, 1,
            0, 0, 1, 1, 1, 1,
            0, 0, 1, 1, 1, 1,
        });
    }

    @Test
    public void clustering() {
        var bp = fromShape(1, 5, 2);
        assertClusteringEquals(bp, new int[]{
            1, 1,
            0, 1,
            1, 1,
            0, 1,
            1, 1,
        }, bp.findClustering(new int[]{
            1, 1,
            0, 1,
            1, 1,
            0, 1,
            1, 1,
        }, false));

        assertClusteringEquals(bp, new int[]{
            1, 1,
            0, 1,
            2, 2,
            0, 3,
            3, 3,
        }, bp.findClustering(new int[]{
            1, 1,
            0, 1,
            2, 2,
            0, 1,
            1, 1,
        }, false));
    }

    @Test
    public void clusteringWithDiagonal() {
        var bp = fromShape(1, 5, 2);
        assertClusteringEquals(bp, new int[]{
            1, 1,
            0, 1,
            2, 0,
            0, 3,
            3, 3,
        }, bp.findClustering(new int[]{
            1, 1,
            0, 1,
            1, 0,
            0, 1,
            1, 1,
        }, false));

        assertClusteringEquals(bp, new int[]{
            1, 1,
            0, 1,
            1, 0,
            0, 1,
            1, 1,
        }, bp.findClustering(new int[]{
            1, 1,
            0, 1,
            1, 0,
            0, 1,
            1, 1,
        }, true));

        assertClusteringEquals(bp, new int[]{
            0, 0,
            1, 1,
            1, 1,
            1, 1,
            0, 0,
        }, bp.findClustering(new int[]{
            0, 0,
            1, 1,
            1, 1,
            1, 1,
            0, 0,
        }, true));
    }

    @Test
    public void clusteringCategory() {
        var bp = fromShape(1, 5, 2);
        assertClusteringEquals(bp, new int[]{
            1, 1,
            0, 1,
            1, 1,
            0, 1,
            1, 1,
        }, bp.findClustering(new int[]{
            1, 1,
            0, 1,
            1, 1,
            0, 1,
            1, 1,
        }, 1, false));

        assertClusteringEquals(bp, new int[]{
            1, 1,
            0, 1,
            0, 0,
            0, 2,
            2, 2,
        }, bp.findClustering(new int[]{
            1, 1,
            0, 1,
            2, 2,
            0, 1,
            1, 1,
        }, 1, false));

        assertClusteringEquals(bp, new int[]{
            1, 1,
            0, 1,
            1, 0,
            0, 1,
            1, 1,
        }, bp.findClustering(new int[]{
            1, 1,
            0, 1,
            1, 0,
            0, 1,
            1, 1,
        }, 1, true));
    }

    @Test
    public void clusteringEdge() {
        var bp = fromShape(1, 6, 6);
        // 5 6 7
        // 4 8 0
        // 3 2 1
        bp.from(new int[]{
            0, 0, 0, 0, 0, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 0, 0, 0, 0, 0,
        });
        assertClusteringEdgeEquals(new ClusteringEdges(
            1, 0,
            List.of(
                new ClusteringEdges.Corner(1, 1, 5),
                new ClusteringEdges.Corner(2, 1, 6),
                new ClusteringEdges.Corner(3, 1, 6),
                new ClusteringEdges.Corner(4, 1, 7),
                new ClusteringEdges.Corner(4, 2, 0),
                new ClusteringEdges.Corner(4, 3, 0),
                new ClusteringEdges.Corner(4, 4, 1),
                new ClusteringEdges.Corner(3, 4, 2),
                new ClusteringEdges.Corner(2, 4, 2),
                new ClusteringEdges.Corner(1, 4, 3),
                new ClusteringEdges.Corner(1, 3, 4),
                new ClusteringEdges.Corner(1, 2, 4)
            )
        ), bp.getClusteringEdges());

        bp.from(new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 1, 1, 0, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 0, 1, 1, 0, 0,
            0, 0, 0, 0, 0, 0,
        });
        assertClusteringEdgeEquals(new ClusteringEdges(
            1, 0,
            List.of(
                new ClusteringEdges.Corner(2, 1, 5),
                new ClusteringEdges.Corner(3, 1, 7),
                new ClusteringEdges.Corner(3, 2, 7),
                new ClusteringEdges.Corner(4, 2, 7),
                new ClusteringEdges.Corner(4, 3, 1),
                new ClusteringEdges.Corner(3, 3, 1),
                new ClusteringEdges.Corner(3, 4, 1),
                new ClusteringEdges.Corner(2, 4, 3),
                new ClusteringEdges.Corner(2, 3, 3),
                new ClusteringEdges.Corner(1, 3, 3),
                new ClusteringEdges.Corner(1, 2, 5),
                new ClusteringEdges.Corner(2, 2, 5)
            )
        ), bp.getClusteringEdges());

        bp.from(new int[]{
            0, 1, 0, 0, 0, 0,
            0, 1, 1, 0, 0, 0,
            0, 1, 1, 1, 0, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 1,
            0, 0, 0, 0, 0, 0,
        });
        assertClusteringEdgeEquals(new ClusteringEdges(
            1, 0,
            List.of(
                new ClusteringEdges.Corner(1, 0, 5),
                new ClusteringEdges.Corner(1, 1, 7),
                new ClusteringEdges.Corner(2, 1, 7),
                new ClusteringEdges.Corner(2, 2, 7),
                new ClusteringEdges.Corner(3, 2, 7),
                new ClusteringEdges.Corner(3, 3, 7),
                new ClusteringEdges.Corner(4, 3, 7),
                new ClusteringEdges.Corner(4, 4, 7),
                new ClusteringEdges.Corner(5, 4, 7),
                new ClusteringEdges.Corner(5, 4, 1),
                new ClusteringEdges.Corner(4, 4, 2),
                new ClusteringEdges.Corner(3, 4, 2),
                new ClusteringEdges.Corner(2, 4, 2),
                new ClusteringEdges.Corner(1, 4, 3),
                new ClusteringEdges.Corner(1, 3, 4),
                new ClusteringEdges.Corner(1, 2, 4),
                new ClusteringEdges.Corner(1, 1, 4)
            )
        ), bp.getClusteringEdges());

    }

    @Test
    public void clusteringEdgeRemoveSmallCorner() {
        var bp = fromShape(1, 6, 6);
        // 5 6 7
        // 4 8 0
        // 3 2 1

        bp.from(new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 1, 1, 0, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 0, 1, 1, 0, 0,
            0, 0, 0, 0, 0, 0,
        });
        assertClusteringEdgeEquals(new ClusteringEdges(
            1, 0,
            List.of(
                new ClusteringEdges.Corner(2, 1, 5),
                new ClusteringEdges.Corner(3, 1, 7),
                new ClusteringEdges.Corner(4, 2, 7),
                new ClusteringEdges.Corner(4, 3, 1),
                new ClusteringEdges.Corner(3, 4, 1),
                new ClusteringEdges.Corner(2, 4, 3),
                new ClusteringEdges.Corner(1, 3, 3),
                new ClusteringEdges.Corner(1, 2, 5)
            )
        ), bp.getClusteringEdges().get(0).smallCornerRemoving(1, 1));

    }

    @Test
    public void fill() {
        var bp = fromShape(1, 5, 2);
        bp.from(new int[]{
            1, 1,
            0, 1,
            1, 1,
            0, 1,
            1, 1,
        });

        bp.fill();
        assertBlueprintEquals(bp, new int[]{
            1, 1,
            1, 1,
            1, 1,
            1, 1,
            1, 1,
        });

        bp.from(new int[]{
            0, 1,
            1, 1,
            1, 1,
            1, 1,
            1, 0,
        });

        bp.fill();
        assertBlueprintEquals(bp, new int[]{
            1, 1,
            1, 1,
            1, 1,
            1, 1,
            1, 1,
        });

        bp.from(new int[]{
            0, 0,
            0, 1,
            1, 1,
            1, 0,
            0, 0,
        });

        bp.fill();
        assertBlueprintEquals(bp, new int[]{
            0, 0,
            1, 1,
            1, 1,
            1, 1,
            0, 0,
        });

        bp.from(new int[]{
            0, 0,
            1, 1,
            1, 1,
            1, 1,
            0, 0,
        });

        bp.fill();
        assertBlueprintEquals(bp, new int[]{
            0, 0,
            1, 1,
            1, 1,
            1, 1,
            0, 0,
        });
    }

    @Test
    public void fillWithThreshold() {
        var bp = fromShape(1, 5, 2);
        bp.from(new int[]{
            2, 2,
            2, 2,
            2, 0,
            0, 1,
            1, 1,
        });

        bp.fill(new BlueprintToolkit.AreaThreshold(4, 10));
        assertBlueprintEquals(bp, new int[]{
            2, 2,
            2, 2,
            2, 2,
            0, 1,
            1, 1,
        });

        bp.from(new int[]{
            2, 2,
            2, 2,
            2, 0,
            0, 1,
            1, 1,
        });

        bp.fill(new BlueprintToolkit.AreaThreshold(0, 4));
        assertBlueprintEquals(bp, new int[]{
            2, 2,
            2, 2,
            2, 0,
            1, 1,
            1, 1,
        });
    }

    @Test
    public void extend() {
        var bp = fromShape(1, 5, 2);
        bp.from(new int[]{
            0, 0,
            0, 0,
            1, 1,
            0, 0,
            0, 0,
        });

        bp.extend(1, 1);
        assertBlueprintEquals(bp, new int[]{
            0, 0,
            1, 1,
            1, 1,
            1, 1,
            0, 0,
        });
    }

    @Test
    public void extendWithValue() {
        var bp = fromShape(1, 5, 2);
        bp.from(new int[]{
            0, 0,
            0, 0,
            1, 1,
            0, 0,
            0, 0,
        });

        bp.extend(1, 1, 2);
        assertBlueprintEquals(bp, new int[]{
            0, 0,
            2, 2,
            1, 1,
            2, 2,
            0, 0,
        });
    }

    @Test
    public void expendWithDirection() {
        var bp = fromShape(1, 6, 6);
        var reset = new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 1, 1, 0, 0,
            0, 0, 1, 1, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
        };

        bp.from(reset);
        bp.extend(1, new BlueprintToolkit.AreaChange(1, 0, 0, 0), BlueprintToolkit.AreaThreshold.ALL);
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 1, 1, 0, 0,
            0, 0, 1, 1, 0, 0,
            0, 0, 1, 1, 0, 0,
            0, 0, 0, 0, 0, 0,
        });

        bp.from(reset);
        bp.extend(1, new BlueprintToolkit.AreaChange(0, 1, 0, 0), BlueprintToolkit.AreaThreshold.ALL);
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 1, 1, 0, 0,
            0, 0, 1, 1, 0, 0,
            0, 0, 1, 1, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
        });

        bp.from(reset);
        bp.extend(1, new BlueprintToolkit.AreaChange(0, 0, 1, 0), BlueprintToolkit.AreaThreshold.ALL);
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 1, 1, 1, 0, 0,
            0, 1, 1, 1, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
        });

        bp.from(reset);
        bp.extend(1, new BlueprintToolkit.AreaChange(0, 0, 0, 1), BlueprintToolkit.AreaThreshold.ALL);
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 1, 1, 1, 0,
            0, 0, 1, 1, 1, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
        });

        bp.from(reset);
        bp.extend(1, new BlueprintToolkit.AreaChange(1, 1, 1, 1), BlueprintToolkit.AreaThreshold.ALL);
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 0, 0, 0, 0, 0,
        });

    }

    @Test
    public void reduce() {
        var bp = fromShape(1, 7, 2);
        bp.from(new int[]{
            0, 0,
            1, 1,
            1, 1,
            1, 1,
            1, 1,
            1, 1,
            0, 0,
        });

        bp.reduce(1, 1);
        assertBlueprintEquals(bp, new int[]{
            0, 0,
            0, 0,
            1, 1,
            1, 1,
            1, 1,
            0, 0,
            0, 0,
        });

        bp.from(new int[]{
            2, 2,
            1, 1,
            1, 1,
            1, 1,
            1, 1,
            1, 1,
            0, 0,
        });

        bp.reduce(1, 1);
        assertBlueprintEquals(bp, new int[]{
            2, 2,
            0, 0,
            1, 1,
            1, 1,
            1, 1,
            0, 0,
            0, 0,
        });
    }

    @Test
    public void reduceWithDirection() {
        var bp = fromShape(1, 6, 6);
        var reset = new int[]{
            0, 0, 0, 0, 0, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 0, 0, 0, 0, 0,
        };

        bp.from(reset);
        bp.reduce(1, new BlueprintToolkit.AreaChange(1, 0, 0, 0), BlueprintToolkit.AreaThreshold.ALL);
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
        });

        bp.from(reset);
        bp.reduce(1, new BlueprintToolkit.AreaChange(0, 1, 0, 0), BlueprintToolkit.AreaThreshold.ALL);
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 0, 0, 0, 0, 0,
        });

        bp.from(reset);
        bp.reduce(1, new BlueprintToolkit.AreaChange(0, 0, 1, 0), BlueprintToolkit.AreaThreshold.ALL);
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 1, 1, 1, 0,
            0, 0, 1, 1, 1, 0,
            0, 0, 1, 1, 1, 0,
            0, 0, 1, 1, 1, 0,
            0, 0, 0, 0, 0, 0,
        });

        bp.from(reset);
        bp.reduce(1, new BlueprintToolkit.AreaChange(0, 0, 0, 1), BlueprintToolkit.AreaThreshold.ALL);
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 1, 1, 1, 0, 0,
            0, 1, 1, 1, 0, 0,
            0, 1, 1, 1, 0, 0,
            0, 1, 1, 1, 0, 0,
            0, 0, 0, 0, 0, 0,
        });

        bp.from(reset);
        bp.reduce(1, new BlueprintToolkit.AreaChange(1, 1, 1, 1), BlueprintToolkit.AreaThreshold.ALL);
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 1, 1, 0, 0,
            0, 0, 1, 1, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
        });
    }

    @Test
    public void reduceOverArea() {
        var bp = fromShape(1, 6, 6);
        var reset = new int[]{
            0, 0, 0, 0, 0, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 1, 1, 1, 1, 0,
            0, 0, 0, 0, 0, 0,
        };

        bp.from(reset);
        bp.reduce(1, new BlueprintToolkit.AreaChange(2, 2, 0, 0), BlueprintToolkit.AreaThreshold.ALL);
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
        });

        bp.from(reset);
        bp.reduce(1, new BlueprintToolkit.AreaChange(0, 0, 2, 2), BlueprintToolkit.AreaThreshold.ALL);
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
        });

        bp.from(reset);
        bp.reduce(1, new BlueprintToolkit.AreaChange(0, 1, 1, 0), BlueprintToolkit.AreaThreshold.ALL);
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 1, 1, 1, 0,
            0, 0, 1, 1, 1, 0,
            0, 0, 1, 1, 1, 0,
            0, 0, 0, 0, 0, 0,
        });

        bp.from(reset);
        bp.reduce(1, new BlueprintToolkit.AreaChange(0, 2, 2, 0), BlueprintToolkit.AreaThreshold.ALL);
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 1, 1, 0,
            0, 0, 0, 1, 1, 0,
            0, 0, 0, 0, 0, 0,
        });

        bp.from(reset);
        bp.reduce(1, new BlueprintToolkit.AreaChange(0, 3, 3, 0), BlueprintToolkit.AreaThreshold.ALL);
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 1, 0,
            0, 0, 0, 0, 0, 0,
        });

        bp.from(reset);
        bp.reduce(1, new BlueprintToolkit.AreaChange(0, 4, 4, 0), BlueprintToolkit.AreaThreshold.ALL);
        assertBlueprintEquals(bp, new int[]{
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0,
        });
    }

    @Test
    public void reduceWithValue() {
        var bp = fromShape(1, 7, 2);
        bp.from(new int[]{
            0, 0,
            1, 1,
            1, 1,
            1, 1,
            1, 1,
            1, 1,
            0, 0,
        });

        bp.reduce(1, 1, 2);
        assertBlueprintEquals(bp, new int[]{
            0, 0,
            2, 2,
            1, 1,
            1, 1,
            1, 1,
            2, 2,
            0, 0,
        });
    }

    @Test
    public void reduceToNone() {
        var bp = fromShape(1, 4, 2);
        bp.from(new int[]{
            0, 0,
            1, 1,
            1, 1,
            0, 0,
        });

        bp.reduce(1, 1);
        assertBlueprintEquals(bp, new int[]{
            0, 0,
            0, 0,
            0, 0,
            0, 0,
        });
    }

    @Test
    public void interpolateNaNWithOneSizeKernel() {
        var bp = fromShape(1, 5, 2);
        var x = Double.NaN;
        var origin = new double[]{
            0, 0,
            1, x,
            x, 1,
            1, x,
            0, 0,
        };
        var expect = new double[]{
            0, 0,
            1, x,
            x, 1,
            1, x,
            0, 0,
        };
        var actual = bp.interpolateNaN(origin, 1, BlueprintToolkit.InterpolateMethod.mean);
        assertArrayEquals(expect, actual);
        assertArrayEquals(expect, origin, "origin has beed changed");
    }

    @Test
    public void interpolateNaN() {
        var bp = fromShape(1, 5, 2);
        var x = Double.NaN;
        var origin = new double[]{
            0, 0,
            1, x,
            x, 1,
            1, x,
            0, 0,
        };
        var copied = origin.clone();
        var expect = new double[]{
            0, 0,
            1, .5,
            1, 1,
            1, .5,
            0, 0,
        };
        var actual = bp.interpolateNaN(origin, 3, BlueprintToolkit.InterpolateMethod.mean);
        assertArrayEquals(expect, actual);
        assertArrayEquals(copied, origin, "origin has beed changed");
    }

    @Test
    public void interpolateNaNWithKernelMin() {
        var bp = fromShape(1, 5, 2);
        var x = Double.NaN;
        var origin = new double[]{
            0, 0,
            1, x,
            x, 1,
            1, x,
            0, 0,
        };
        var copied = origin.clone();
        var expect = new double[]{
            0, 0,
            1, 0,
            1, 1,
            1, 0,
            0, 0,
        };
        var actual = bp.interpolateNaN(origin, 3, BlueprintToolkit.InterpolateMethod.min);
        assertArrayEquals(expect, actual);
        assertArrayEquals(copied, origin, "origin has beed changed");
    }

    @Test
    public void interpolateNaNWithKernelMax() {
        var bp = fromShape(1, 5, 2);
        var x = Double.NaN;
        var origin = new double[]{
            0, 0,
            1, x,
            x, 1,
            1, x,
            0, 0,
        };
        var copied = origin.clone();
        var expect = new double[]{
            0, 0,
            1, 1,
            1, 1,
            1, 1,
            0, 0,
        };
        var actual = bp.interpolateNaN(origin, 3, BlueprintToolkit.InterpolateMethod.max);
        assertArrayEquals(expect, actual);
        assertArrayEquals(copied, origin, "origin has beed changed");
    }

    @Test
    public void interpolateNaNWithKernelZero() {
        var bp = fromShape(1, 5, 2);
        var x = Double.NaN;
        var origin = new double[]{
            0, 1,
            1, x,
            x, 2,
            1, x,
            0, 3,
        };
        var copied = origin.clone();
        var expect = new double[]{
            0, 1,
            1, 0,
            0, 2,
            1, 0,
            0, 3,
        };
        var actual = bp.interpolateNaN(origin, 3, BlueprintToolkit.InterpolateMethod.zero);
        assertArrayEquals(expect, actual);
        assertArrayEquals(copied, origin, "origin has beed changed");
    }

}

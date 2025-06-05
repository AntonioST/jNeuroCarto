package io.ast.jneurocarto.core.blueprint;

import java.util.List;
import java.util.Objects;

import org.opentest4j.AssertionFailedError;

import static org.junit.jupiter.api.Assertions.*;

public class BlueprintAssertion {

    private BlueprintAssertion() {
        throw new RuntimeException();
    }

    public static BlueprintToolkit<Object> fromShape(int ns, int ny, int nx) {
        return BlueprintToolkit.dummy(ns, ny, nx);
    }

    public static void assertBlueprintEquals(BlueprintToolkit<?> bp, int[] expect) {
        assertBlueprintEquals(bp, expect, bp.ref());
    }

    public static void assertBlueprintEquals(BlueprintToolkit<?> bp, int[] expect, int[] actual) {
        try {
            assertArrayEquals(expect, actual);
        } catch (AssertionFailedError e) {
            String message;

            try {
                message = buildBlueprintComparingMessage(bp, expect, actual);
            } catch (Exception ex) {
                throw e;
            }

            throw new AssertionFailedError(message, e.getExpected(), e.getActual(), e);
        }
    }

    public static void assertClusteringEquals(BlueprintToolkit<?> bp, int[] expect, int[] actual) {
        assertClusteringEquals(bp, new Clustering(expect), new Clustering(actual));
    }

    public static void assertClusteringEquals(BlueprintToolkit<?> bp, int[] expect, Clustering actual) {
        assertClusteringEquals(bp, new Clustering(expect), actual);
    }

    public static void assertClusteringEquals(BlueprintToolkit<?> bp, Clustering expect, Clustering actual) {
        try {
            assertEquals(expect.length(), actual.length(), "different length");

            var totalGroup = expect.groupNumber();
            var actualGroup = actual.groupNumber();
            assertEquals(totalGroup, actualGroup, "different group number");

            var tmp = new Clustering(actual);
            for (var expGroup : expect.groups()) {
                var index = expect.indexGroup(expGroup);
                var actGroup = assertDoesNotThrow(() -> clusteringGet(tmp.clustering(), index), "not unique group with given index");
                assertTrue(actGroup > 0, "not a group with given index");
                clusteringSet(tmp.clustering(), index, 0);
                assertEquals(--actualGroup, tmp.groupNumber(), "group does not match with given index");
            }
            assertEquals(0, tmp.groupNumber(), "has extra groups");

        } catch (AssertionFailedError e) {
            String message;
            try {
                message = buildBlueprintComparingMessage(bp, expect.clustering(), actual.clustering());
            } catch (Exception ex) {
                throw e;
            }

            throw new AssertionFailedError(message, e.getExpected(), e.getActual(), e);

        } catch (Throwable e) {
            String message;
            try {
                message = buildBlueprintComparingMessage(bp, expect.clustering(), actual.clustering());
            } catch (Exception ex) {
                throw e;
            }

            throw new AssertionFailedError(message, expect, actual, e);
        }
    }

    public static String buildBlueprintComparingMessage(BlueprintToolkit<?> bp, int[] expect, int[] actual) {
        var expIter = bp.toString(expect).lines().iterator();
        var actIter = bp.toString(actual).lines().iterator();
        var expLen = 0;
        var actLen = 0;
        String expText;
        String actText;

        var message = new StringBuilder("\n");

        while (expIter.hasNext() && actIter.hasNext()) {
            expText = expIter.next();
            actText = actIter.next();
            expLen = expText.length();
            actLen = actText.length();
            message.append(expText).append("  ").append(actText).append('\n');
        }

        if (expIter.hasNext()) {
            actText = " ".repeat(actLen);
            while (expIter.hasNext()) {
                message.append(expIter.next()).append("  ").append(actText).append('\n');
            }
        } else if (actIter.hasNext()) {
            expText = " ".repeat(expLen);
            while (actIter.hasNext()) {
                message.append(expText).append("  ").append(actIter.next()).append('\n');
            }
        }

        message.append("%%-%ds".formatted(expLen).formatted("exp")).append("  ")
            .append("%%-%ds".formatted(actLen).formatted("act")).append('\n');

        return message.toString();
    }

    private static int clusteringGet(int[] clustering, int[] i) {
        var ret = -1;
        for (var j : i) {
            var g = clustering[j];
            if (g < 0) throw new RuntimeException();
            if (ret < 0) {
                ret = g;
            } else if (ret != g) {
                throw new RuntimeException();
            }
        }
        return ret;
    }

    private static void clusteringSet(int[] clustering, int[] i, int group) {
        for (var j : i) {
            clustering[j] = group;
        }
    }

    public static void assertClusteringEdgeEquals(List<ClusteringEdges> expect, List<ClusteringEdges> actual) {
        var size = expect.size();
        assertEquals(size, actual.size(), "different edges list size");
        for (int i = 0; i < size; i++) {
            assertClusteringEdgeEquals(expect.get(i), actual.get(i));
        }
    }

    public static void assertClusteringEdgeEquals(ClusteringEdges expect, List<ClusteringEdges> actual) {
        assertEquals(1, actual.size(), "different edges list size");
        assertClusteringEdgeEquals(expect, actual.get(0));
    }

    public static void assertClusteringEdgeEquals(ClusteringEdges expect, ClusteringEdges actual) {
        assertEquals(expect.category(), actual.category(), "different category");
        assertEquals(expect.shank(), actual.shank(), "different shank");
        var size = expect.edges().size();
        try {
            assertEquals(size, actual.edges().size(), "different edges size");
        } catch (AssertionFailedError e) {
            var message = buildClusteringEdgesComparingMessage(expect, actual);
            throw new AssertionFailedError(message, e.getExpected(), e.getActual(), e);
        }

        try {
            for (int i = 0; i < size; i++) {
                assertEquals(expect.edges().get(i), actual.edges().get(i), "different edge at " + i);
            }
        } catch (AssertionFailedError e) {
            var message = buildClusteringEdgesComparingMessage(expect, actual);
            throw new AssertionFailedError(message, e.getExpected(), e.getActual(), e);
        }
    }

    private static String buildClusteringEdgesComparingMessage(ClusteringEdges expect, ClusteringEdges actual) {
        var sb = new StringBuilder();
        sb.append('\n');

        var exit = expect.edges().iterator();
        var acit = actual.edges().iterator();
        String expt;
        String actt;
        var i = 0;

        while (exit.hasNext() && acit.hasNext()) {
            var ex = exit.next();
            var ac = acit.next();
            var t = ex.equals(ac);
            expt = Objects.toString(ex);
            actt = Objects.toString(ac);
            sb.append("%2d ".formatted(i++)).append(expt).append(t ? " == " : " != ").append(actt).append('\n');
        }

        if (exit.hasNext()) {
            while (exit.hasNext()) {
                var ex = exit.next();
                expt = Objects.toString(ex);
                sb.append("%2d ".formatted(i++)).append(expt).append(" != \n");
            }
        } else if (acit.hasNext()) {
            while (acit.hasNext()) {
                var ac = acit.next();
                actt = Objects.toString(ac);
                sb.append("%2d ".formatted(i++)).append(" ".repeat(actt.length())).append(" != ").append(actt).append('\n');
            }
        }
        return sb.toString();
    }
}

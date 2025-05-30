package io.ast.jneurocarto.test;

import org.opentest4j.AssertionFailedError;

import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;
import io.ast.jneurocarto.core.blueprint.Clustering;

import static org.junit.jupiter.api.Assertions.*;

public class BlueprintAssertion {

    private BlueprintAssertion() {
        throw new RuntimeException();
    }

    public static BlueprintToolkit<Object> fromShape(int ns, int ny, int nx) {
        return BlueprintToolkit.dummy(ns, ny, nx);
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
}

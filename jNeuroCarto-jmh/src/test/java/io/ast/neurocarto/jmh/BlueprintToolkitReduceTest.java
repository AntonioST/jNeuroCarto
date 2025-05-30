package io.ast.neurocarto.jmh;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.ast.jneurocarto.core.blueprint.BlueprintToolkit;

import static io.ast.jneurocarto.core.blueprint.BlueprintAssertion.assertBlueprintEquals;

public class BlueprintToolkitReduceTest {

    @Test
    public void sharedSetupCode6() {
        var shared = new BM_BlueprintToolkitReduce.Shared();
        shared.size = 6;
        shared.setup();

        assertBlueprintEquals(shared.toolkit, new int[]{
          0, 0, 0, 0, 0, 0,
          0, 1, 1, 1, 1, 0,
          0, 1, 1, 1, 1, 0,
          0, 1, 1, 1, 1, 0,
          0, 1, 1, 1, 1, 0,
          0, 0, 0, 0, 0, 0,
        });
    }

    @Test
    public void reduceBaseline() {
        var shared = new BM_BlueprintToolkitReduce.Shared();
        shared.size = 6;
        shared.setup();

        assertBlueprintEquals(shared.toolkit, shared.blueprint);
        Assertions.assertEquals(16, shared.toolkit.count(1));
        var output = BM_BlueprintToolkitReduce.reduceBaseline(shared, 1, new BlueprintToolkit.AreaChange(1, 1, 1, 1));
        assertBlueprintEquals(shared.toolkit, new int[]{
          0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0,
          0, 0, 1, 1, 0, 0,
          0, 0, 1, 1, 0, 0,
          0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0,
        }, output);
    }

    @Test
    public void reduceByIndex() {
        var shared = new BM_BlueprintToolkitReduce.Shared();
        shared.size = 6;
        shared.setup();

        assertBlueprintEquals(shared.toolkit, shared.blueprint);
        Assertions.assertEquals(16, shared.toolkit.count(1));
        var output = BM_BlueprintToolkitReduce.reduceByIndex(shared, 1, new BlueprintToolkit.AreaChange(1, 1, 1, 1));
        assertBlueprintEquals(shared.toolkit, new int[]{
          0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0,
          0, 0, 1, 1, 0, 0,
          0, 0, 1, 1, 0, 0,
          0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0,
        }, output);
    }

    @Test
    public void reduceByMask() {
        var shared = new BM_BlueprintToolkitReduce.Shared();
        shared.size = 6;
        shared.setup();

        assertBlueprintEquals(shared.toolkit, shared.blueprint);
        Assertions.assertEquals(16, shared.toolkit.count(1));
        var output = BM_BlueprintToolkitReduce.reduceByMask(shared, 1, new BlueprintToolkit.AreaChange(1, 1, 1, 1));
        assertBlueprintEquals(shared.toolkit, new int[]{
          0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0,
          0, 0, 1, 1, 0, 0,
          0, 0, 1, 1, 0, 0,
          0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0,
        }, output);
    }
}

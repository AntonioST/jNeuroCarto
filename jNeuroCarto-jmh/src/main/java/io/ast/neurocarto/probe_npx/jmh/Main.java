package io.ast.neurocarto.probe_npx.jmh;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class Main {
    public static void main(String[] args) throws RunnerException {
        var opt = new OptionsBuilder()
          .include(BM_ChannelmapUtil.class.getSimpleName())
          .forks(1)
          .build();

        new Runner(opt).run();
    }
}

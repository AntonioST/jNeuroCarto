package io.ast.jneurocarto.core.cli;

import java.util.Arrays;
import java.util.List;

import picocli.CommandLine;

public class CommaStringListConverter implements CommandLine.ITypeConverter<List<String>> {
    @Override
    public List<String> convert(String value) {
        return Arrays.asList(value.split(","));
    }
}

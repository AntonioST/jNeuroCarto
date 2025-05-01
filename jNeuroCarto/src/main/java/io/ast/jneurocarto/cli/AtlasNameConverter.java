package io.ast.jneurocarto.cli;

import picocli.CommandLine;

public class AtlasNameConverter implements CommandLine.ITypeConverter<String> {
    @Override
    public String convert(String value) {
        try {
            var resolution = Integer.parseInt(value);
            return "allen_mouse_" + resolution + "um";
        } catch (NumberFormatException e) {
            return value;
        }
    }
}

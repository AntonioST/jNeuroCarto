package io.ast.jneurocarto.core.cli;

import picocli.CommandLine;

public class AtlasNameConverter implements CommandLine.ITypeConverter<String> {
    @Override
    public String convert(String value) {
        switch (value) {
        case "''":
        case "\"\"":
            return "";
        }

        try {
            var resolution = Integer.parseInt(value);
            return "allen_mouse_" + resolution + "um";
        } catch (NumberFormatException e) {
            return value;
        }
    }
}

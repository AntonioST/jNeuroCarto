package io.ast.jneurocarto.core.cli;

import picocli.CommandLine;

/**
 * A command line converter for processing brain atlas name,
 * which allow just input '25' to specify 'allen_mouse_25um'.
 */
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

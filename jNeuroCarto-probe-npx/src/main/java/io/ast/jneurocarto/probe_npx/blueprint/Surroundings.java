package io.ast.jneurocarto.probe_npx.blueprint;

import io.ast.jneurocarto.core.BlueprintTool;

final class Surroundings {
    private Surroundings() {
        throw new RuntimeException();
    }

    static void surrounding(BlueprintTool<?> tool, int e, boolean diagonal, int[] output) {
        assert output.length == 8;
        var s = tool.shank()[e];
        var x = tool.posx()[e];
        var y = tool.posy()[e];

        if (diagonal) {
            for (int i = 0; i < 8; i++) {
                output[i] = surrounding(tool, s, x, y, i);
            }
        } else {
            for (int i = 0; i < 4; i++) {
                // 0, 2, 4, 6
                output[i] = surrounding(tool, s, x, y, i * 2);
                // 1, 3, 5, 7
                output[i + 1] = -1;
            }
        }
    }

    static int surrounding(BlueprintTool<?> tool, int e, int c) {
        var s = tool.shank()[e];
        var x = tool.posx()[e];
        var y = tool.posy()[e];
        return surrounding(tool, s, x, y, c);
    }

    static int surrounding(BlueprintTool<?> tool, int s, int x, int y, int c) {
        return switch (c % 8) {
            case 0 -> tool.index(s, x + 1, y);
            case 1, -7 -> tool.index(s, x + 1, y + 1);
            case 2, -6 -> tool.index(s, x + 0, y + 1);
            case 3, -5 -> tool.index(s, x - 1, y + 1);
            case 4, -4 -> tool.index(s, x - 1, y + 0);
            case 5, -3 -> tool.index(s, x - 1, y - 1);
            case 6, -2 -> tool.index(s, x + 0, y - 1);
            case 7, -1 -> tool.index(s, x + 1, y - 1);
            default -> throw new IllegalArgumentException();
        };
    }
}

package io.ast.jneurocarto.javafx.utils;

import javafx.scene.Node;

public final class StylesheetsUtils {
    public static boolean hasStyleClass(Node node, String style) {
        var list = node.getStyleClass();
        return list.contains(style);
    }

    public static boolean addStyleClass(Node node, String style) {
        var list = node.getStyleClass();
        if (list.contains(style)) {
            return true;
        } else {
            list.add(style);
            return false;
        }
    }

    public static boolean removeStyleClass(Node node, String style) {
        var list = node.getStyleClass();
        return list.remove(style);
    }

//    public static void addStylesheets(Node node, String css) throws IOException {
//        var dir = Path.of("/tmp/.jneurocarto");
//        Files.createDirectories(dir);
//
//        var prefix = "io.ast.jneurocarto.javafx.utils.StylesheetsUtil." + node.getClass().getSimpleName();
//        Path file = Files.createTempFile(dir, prefix, ".css");
//        Cleaner.create().register(node, () -> {
//            try {
//                Files.deleteIfExists(file);
//            } catch (IOException e) {
//            }
//        });
//
//        Files.writeString(file, css);
//        node.getStyleClass().add(file.toUri().toString());
//    }


}

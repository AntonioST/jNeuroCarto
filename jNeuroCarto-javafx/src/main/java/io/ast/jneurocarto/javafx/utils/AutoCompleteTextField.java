package io.ast.jneurocarto.javafx.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class AutoCompleteTextField extends TextField {

    public interface Completor {
        @Nullable
        List<String> complete(String content, int cursor);
    }

    private final Completor completor;
    private boolean duringTyping;
    private @Nullable List<String> candidates = null;
    private int candidateIndex;

    private AutoCompleteTextField() {
        if (this instanceof Completor completor) {
            this.completor = completor;
        } else {
            throw new RuntimeException("itself is not a Completor");
        }
        setup();
    }

    public AutoCompleteTextField(Completor completor) {
        this.completor = completor;
        setup();
    }

    private void setup() {
        addEventFilter(KeyEvent.KEY_PRESSED, this::onKeyType);
        textProperty().addListener((_, _, text) -> onKeyType(text));
    }

    private void onKeyType(KeyEvent e) {
        var c = e.getCode();
        if ((c.isLetterKey() || c.isDigitKey()) && !(e.isControlDown() || e.isAltDown())) {
            duringTyping = true;
        } else if (c == KeyCode.UP || c == KeyCode.DOWN) {
            var index = candidateIndex + (c == KeyCode.UP ? -1 : 1);
            var p = getCaretPosition();
            Platform.runLater(() -> updateCandidate(index, p));
            e.consume();
        } else if (c == KeyCode.RIGHT) {
            var t = getText();
            positionCaret(t.length());
            completeFor(t);
        }
    }

    private void onKeyType(String text) {
        if (duringTyping) {
            duringTyping = false;
            completeFor(text);
        }
    }

    private void completeFor(String text) {
        var pos = Math.min(getCaretPosition() + 1, text.length());

        this.candidates = null;
        var candidates = completor.complete(text, pos);
        if (candidates != null) {
            candidates = candidates.stream().filter(it -> it.startsWith(text)).toList();
            this.candidates = candidates;
            var n = candidates.size();
            if (n > 0) {
                var i = candidates.get(0).equals(text) && n > 1 ? 1 : 0;
                Platform.runLater(() -> updateCandidate(i, pos));
            }
        }
    }

    private void updateCandidate(int index, int pos) {
        if (candidates == null) return;
        if (!(0 <= index && index < candidates.size())) return;
        candidateIndex = index;
        var text = candidates.get(index);

        setText(text);
        selectRange(text.length(), pos);
    }

    public static class OfPathField extends AutoCompleteTextField implements AutoCompleteTextField.Completor {

        public OfPathField() {
            super();
        }

        /*============*
         * properties *
         *============*/

        public final ObjectProperty<Path> pathProperty = new SimpleObjectProperty<>(Path.of("."));

        public final Path getRoot() {
            return pathProperty.get();
        }

        public final void setRoot(Path value) {
            pathProperty.set(value);
        }

        public final BooleanProperty allowMultipleFileProperty = new SimpleBooleanProperty();

        public final boolean isAllowMultipleFileProperty() {
            return allowMultipleFileProperty.get();
        }

        public final void setAllowMultipleFileProperty(boolean value) {
            allowMultipleFileProperty.set(value);
        }

        public final BooleanProperty allowEscapeRootProperty = new SimpleBooleanProperty();

        public final boolean isAllowEscapeRootProperty() {
            return allowEscapeRootProperty.get();
        }

        public final void setAllowEscapeRootProperty(boolean value) {
            allowEscapeRootProperty.set(value);
        }

        /*==========*
         * complete *
         *==========*/

        @Override
        public @Nullable List<String> complete(String content, int cursor) {
            content = content.substring(0, cursor);
            Stream<String> ret;

            if (isAllowMultipleFileProperty()) {
                var r = content.lastIndexOf(' ');

                if (r < 0) {
                    ret = complete(content);
                } else {
                    var last = content.substring(r + 1);
                    var prev = content.substring(0, r + 1);
                    ret = complete(last).map(it -> prev + it);
                }
            } else {
                ret = complete(content);
            }

            try {
                return ret.toList();
            } finally {
                ret.close();
            }
        }

        public List<String> completeFile(String content) {
            try (var ret = complete(content)) {
                return ret.toList();
            }
        }

        private Stream<String> complete(String content) {
            Path root = pathProperty.get().toAbsolutePath().normalize();
            Path dir;
            String dirname;
            String filename;

            if (content.endsWith("/")) {
                dir = root.resolve(content);
                dirname = content;
                filename = null;
            } else {
                var path = Path.of(content);
                filename = path.getFileName().toString();
                dir = path.getParent();
                if (dir == null) {
                    dir = root;
                    dirname = "";
                } else {
                    dirname = dir + "/";
                    dir = root.resolve(dir);
                }
            }

            dir = dir.toAbsolutePath().normalize();
            if (!Files.exists(dir)) {
                return Stream.of(content);
            }
            if (!isAllowEscapeRootProperty() && !dir.startsWith(root)) {
                return Stream.of(content);
            }

            Stream<Path> ret;

            try {
                ret = Files.list(dir);
            } catch (IOException e) {
                return Stream.of(content);
            }

            return ret.map(p -> {
                  if (Files.isDirectory(p)) {
                      return p.getFileName().toString() + "/";
                  } else {
                      return p.getFileName().toString();
                  }
              })
              .filter(it -> filename == null || it.startsWith(filename))
              .sorted(String::compareToIgnoreCase)
              .map(it -> dirname + it)
              .onClose(ret::close);
        }
    }
}

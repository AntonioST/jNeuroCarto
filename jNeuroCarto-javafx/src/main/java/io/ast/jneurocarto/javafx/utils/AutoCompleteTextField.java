package io.ast.jneurocarto.javafx.utils;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.regex.Pattern;
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

    public AutoCompleteTextField(Completor completor) {
        this.completor = completor;

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

    public static class PathCompletor implements Completor {

        /*============*
         * properties *
         *============*/

        public final ObjectProperty<Path> pathProperty = new SimpleObjectProperty<>(Path.of("."));

        public final BooleanProperty onlyDirectoryProperty = new SimpleBooleanProperty(false);

        public final ObjectProperty<@Nullable PathMatcher> fileMatcherProperty = new SimpleObjectProperty<>(null);

        public final BooleanProperty allowMultipleFileProperty = new SimpleBooleanProperty();

        public final BooleanProperty allowEscapeRootProperty = new SimpleBooleanProperty();

        /*===========*
         * completor *
         *===========*/


        @Override
        public @Nullable List<String> complete(String content, int cursor) {
            content = content.substring(0, cursor);
            Stream<String> ret;

            if (allowMultipleFileProperty.get()) {
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

        public List<String> completeFor(String content) {
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
            if (!allowEscapeRootProperty.get() && !dir.startsWith(root)) {
                return Stream.of(content);
            }

            Stream<Path> ret;

            try {
                ret = Files.list(dir);
            } catch (IOException e) {
                return Stream.of(content);
            }

            if (onlyDirectoryProperty.get()) {
                ret = ret.filter(Files::isDirectory);
            } else {
                var pm = fileMatcherProperty.get();
                if (pm != null) {
                    ret = ret.filter(path -> Files.isDirectory(path) || pm.matches(path.getFileName()));
                }
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

    public static class OfPathField extends AutoCompleteTextField {

        /*============*
         * properties *
         *============*/

        public final ObjectProperty<Path> pathProperty = new SimpleObjectProperty<>();

        public final Path getRoot() {
            return pathProperty.get();
        }

        public final void setRoot(Path value) {
            pathProperty.set(value);
        }

        public final BooleanProperty onlyDirectoryProperty = new SimpleBooleanProperty(false);

        public final boolean isOnlyDirectory() {
            return onlyDirectoryProperty.get();
        }

        public final void setOnlyDirectory(boolean value) {
            onlyDirectoryProperty.set(value);
        }

        public final ObjectProperty<@Nullable PathMatcher> fileMatcherProperty = new SimpleObjectProperty<>(null);

        public final @Nullable PathMatcher getFileMatcher() {
            return fileMatcherProperty.get();
        }

        public final void setFileMatcher(@Nullable String pattern) {
            if (pattern == null) {
                fileMatcherProperty.set(null);
            } else {
                fileMatcherProperty.set(FileSystems.getDefault().getPathMatcher("glob:" + pattern));
            }
        }

        public final void setFileMatcher(@Nullable Pattern pattern) {
            if (pattern == null) {
                fileMatcherProperty.set(null);
            } else {
                fileMatcherProperty.set(FileSystems.getDefault().getPathMatcher("regex:" + pattern.pattern()));
            }
        }

        public final void setFileMatcher(@Nullable PathMatcher matcher) {
            fileMatcherProperty.set(matcher);
        }

        public final BooleanProperty allowMultipleFileProperty = new SimpleBooleanProperty();

        public final boolean isAllowMultipleFile() {
            return allowMultipleFileProperty.get();
        }

        public final void setAllowMultipleFile(boolean value) {
            allowMultipleFileProperty.set(value);
        }

        public final BooleanProperty allowEscapeRootProperty = new SimpleBooleanProperty();

        public final boolean isAllowEscapeRoot() {
            return allowEscapeRootProperty.get();
        }

        public final void setAllowEscapeRoot(boolean value) {
            allowEscapeRootProperty.set(value);
        }

        public final BooleanProperty validateContentProperty = new SimpleBooleanProperty();

        public final boolean isValidateContent() {
            return validateContentProperty.get();
        }

        public final void setValidateContent(boolean value) {
            validateContentProperty.set(value);
        }

        /*=============*
         * constructor *
         *=============*/

        public OfPathField() {
            var completor = new PathCompletor();
            super(completor);
            pathProperty.bindBidirectional(completor.pathProperty);
            onlyDirectoryProperty.bindBidirectional(completor.onlyDirectoryProperty);
            fileMatcherProperty.bindBidirectional(completor.fileMatcherProperty);
            allowMultipleFileProperty.bindBidirectional(completor.allowMultipleFileProperty);
            allowEscapeRootProperty.bindBidirectional(completor.allowEscapeRootProperty);
            FormattedTextField.install(this, this::validate);
        }

        protected @Nullable String validate(String content) {
            if (!isValidateContent() || content.isEmpty()) return null;

            var root = getRoot();
            if (isAllowMultipleFile()) {
                for (var file : content.split(" +")) {
                    var message = validate(root.resolve(file));
                    if (message != null) return message;
                }
            } else {
                return validate(root.resolve(content));
            }

            return null;
        }

        public @Nullable String validate(Path file) {
            if (isOnlyDirectory() && !Files.isDirectory(file)) {
                return file + " is not a directory";
            }

            var pm = getFileMatcher();
            if (pm != null) {
                if (Files.isDirectory(file)) {
                    return file + " is a directory";
                }

                if (!pm.matches(file.getFileName())) {
                    return "";
                }
            }

            return null;
        }
    }
}

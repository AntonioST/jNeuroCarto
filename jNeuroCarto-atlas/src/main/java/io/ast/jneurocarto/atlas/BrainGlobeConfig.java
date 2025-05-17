package io.ast.jneurocarto.atlas;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BrainGlobeConfig {

    public static final String CONFIG_FILENAME = "bg_config.conf";

    private final @Nullable Path file;
    private final Path brainGlobeDir;
    private final Path internDownloadDir;

    public BrainGlobeConfig(@Nullable Path file, String brainglobeDir, String internDownloadDir) {
        this(file, Path.of(brainglobeDir), Path.of(internDownloadDir));
    }

    public BrainGlobeConfig(@Nullable Path file, Path brainglobeDir, Path internDownloadDir) {
        this.file = file;
        this.brainGlobeDir = brainglobeDir;
        this.internDownloadDir = internDownloadDir;
    }

    public static Path getDefaultConfigFile() {
        Path configDir;
        var dir = System.getenv("BRAINGLOBE_CONFIG_DIR");
        if (dir != null) {
            configDir = Path.of(dir);
        } else {
            configDir = Path.of(System.getProperty("user.home")).resolve(".config/brainglobe");
        }

        return configDir.resolve(CONFIG_FILENAME);
    }

    public static BrainGlobeConfig getDefault() {
        var def = Path.of(System.getProperty("user.home")).resolve(".brainglobe").toAbsolutePath().toString();
        return new BrainGlobeConfig(getDefaultConfigFile(), def, def);
    }

    public static BrainGlobeConfig load() throws IOException {
        return load(getDefaultConfigFile());
    }

    public static BrainGlobeConfig load(Path file) throws IOException {
        var prop = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            prop.load(reader);
            return load(file, prop);
        }
    }

    public static BrainGlobeConfig load(Properties prop) {
        return load(null, prop);
    }

    private static BrainGlobeConfig load(@Nullable Path file, Properties prop) {
        return new BrainGlobeConfig(file, prop.getProperty("brainglobe_dir"),
          prop.getProperty("interm_download_dir"));
    }

    public @Nullable Path getConfigFile() {
        return file;
    }

    public Path getBrainGlobeDir() {
        return brainGlobeDir;
    }

    public Path getInternDownloadDir() {
        return internDownloadDir;
    }
}

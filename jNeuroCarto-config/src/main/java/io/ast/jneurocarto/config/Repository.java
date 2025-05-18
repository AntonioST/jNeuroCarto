package io.ast.jneurocarto.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.ast.jneurocarto.config.cli.CartoConfig;
import io.ast.jneurocarto.core.ElectrodeDescription;
import io.ast.jneurocarto.core.ProbeDescription;

@NullMarked
public class Repository {

    private final Logger log = LoggerFactory.getLogger(Repository.class);

    private final JsonConfig userConfig = new JsonConfig();
    private final JsonConfig viewConfig = new JsonConfig();
    private final CartoConfig config;

    public Repository(CartoConfig config) {
        this.config = config;
    }

    public Path getUserConfigDir() {
        return getUserConfigDir(config);
    }

    public Path getUserConfigDir(CartoConfig config) {
        if (config.debug) {
            return Path.of(".");
        }

        String d;
        if ((d = System.getenv("XDG_CONFIG_HOME")) != null) return Path.of(d).resolve("neurocarto");
        if ((d = System.getenv("APPDATA")) != null) return Path.of(d).resolve("neurocarto");

        d = System.getProperty("user.home");
        return switch (System.getProperty("os.name")) {
            case "Linux" -> Path.of(d).resolve(".config/neurocarto");
            default -> Path.of(d).resolve(".neurocarto");
        };
    }

    public Path getUserCacheDir() {
        return getUserCacheDir(config);
    }

    public static Path getUserCacheDir(CartoConfig config) {
        if (config.debug) {
            return Path.of(".");
        }

        String d;
        if ((d = System.getenv("XDG_CACHE_HOME")) != null) return Path.of(d).resolve("neurocarto");

        d = System.getProperty("user.home");
        return switch (System.getProperty("os.name")) {
            case "Linux" -> Path.of(d).resolve(".cache/neurocarto");
            default -> Path.of(d).resolve(".neurocarto/cache");
        };
    }

    public Path getUserCacheFile(String filename) {
        if (config.debug) {
            return Path.of(".neurocarto." + filename);
        }

        return getUserCacheDir().resolve(filename);
    }

    public Path getUserDataDir() {
        return getUserDataDir(config);
    }

    public static Path getUserDataDir(CartoConfig config) {

        if (config.debug) {
            return Path.of(".");
        }

        String d;
        if ((d = System.getenv("XDG_DATA_HOME")) != null) return Path.of(d).resolve("neurocarto");

        d = System.getProperty("user.home");
        return switch (System.getProperty("os.name")) {
            case "Linux" -> Path.of(d).resolve(".local/share/neurocarto");
            default -> Path.of(d).resolve(".neurocarto/share");
        };
    }

    public static final String USER_CONFIG_FILENAME = "neurocarto.config.json";

    public Path getUserConfigFile() {
        var p = config.configFile;
        if (p != null) {
            var f = Path.of(p);
            if (Files.isDirectory(f)) {
                return f.resolve(USER_CONFIG_FILENAME);
            }
            return f;
        }

        if (config.debug) {
            return Path.of("." + USER_CONFIG_FILENAME);
        }

        return getUserConfigDir().resolve(USER_CONFIG_FILENAME);
    }

    public JsonConfig loadUserConfigs() {
        return loadUserConfigs(false);
    }

    public JsonConfig loadUserConfigs(boolean reset) {
        var f = getUserConfigFile();

        log.debug("load user config : {}", f);
        JsonConfig c;
        try {
            c = JsonConfig.load(f);
        } catch (FileNotFoundException e) {
            log.debug("user config not found : {}", f);
            return userConfig;
        } catch (JsonProcessingException e) {
            log.debug("bad user config not found : {}", f);
            var n = f.getFileName().toString();
            var i = n.lastIndexOf('.');
            var t = f.getParent().resolve(n.substring(i) + "_backup" + n.substring(i));
            try {
                Files.move(f, t);
                log.debug("rename to {}", t);
            } catch (IOException ex) {
                log.debug("fail to rename to {}. skipped", t);
            }
            return userConfig;
        } catch (IOException e) {
            log.debug("loadUserConfig", e);
            return userConfig;
        }

        if (reset) {
            userConfig.clear();
        }

        userConfig.update(c);

        return userConfig;
    }


    public void saveUserConfigs() throws IOException {
        var f = getUserConfigFile();
        log.debug("save user config : {}", f);
        userConfig.save(f);
    }

    public CartoUserConfig getUserConfig() {
        CartoUserConfig config;
        try {
            config = userConfig.get(CartoUserConfig.class);
            if (config != null) return config;
        } catch (JsonProcessingException e) {
            log.warn("bad {} in user config, use a default one.", JsonConfig.getName(CartoUserConfig.class));
            config = new CartoUserConfig();
        }

        if (config == null) {
            log.debug("no {} in user config, add a default one.", JsonConfig.getName(CartoUserConfig.class));
            config = new CartoUserConfig();
        }

        userConfig.put(config);
        return config;
    }

    public Path getCurrentResourceRoot() {
        var ret = config.chmapRoot;
        if (ret == null) {
            ret = Path.of(".");
        }
        return ret;
    }

    public void changeResourceRoot(Path root) throws IOException {
        var path = root.toAbsolutePath().normalize();
        if (!Files.isDirectory(path)) {
            throw new NotDirectoryException(root.toString());
        }

        log.info("change root to {}", path);
        config.chmapRoot = path;
    }

    public List<Path> listChannelmapFiles(ProbeDescription<?> probe, boolean recursive) throws IOException {
        var root = getCurrentResourceRoot();

        var suffix = probe.channelMapFileSuffix();

        String pattern;
        if (suffix.isEmpty()) {
            return List.of();
        } else if (suffix.size() == 1) {
            pattern = "glob:*" + suffix.get(0);
        } else {
            pattern = suffix.stream()
              .map(it -> it.substring(1))
              .collect(Collectors.joining(",", "glob:*.{", "}"));
        }

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher(pattern);

        try (var stream = Files.walk(root, recursive ? Integer.MAX_VALUE : 1)) {
            return stream.filter(Files::isRegularFile)
              .filter(matcher::matches)
              .sorted(Comparator.comparing(it -> it.getFileName().toString()))
              .toList();
        }
    }

    public String getChannelmapName(ProbeDescription<?> probe, String name) {
        var suffix = probe.channelMapFileSuffix();
        if (suffix.isEmpty()) {
            return name;
        }
        if (!name.equals(suffix.get(0))) {
            name += suffix.get(0);
        }

        return name;
    }

    public Path getChannelmapFile(ProbeDescription<?> probe, String name) {
        var root = getCurrentResourceRoot();
        return root.resolve(getChannelmapName(probe, name));
    }

    public Path getChannelmapFile(ProbeDescription<?> probe, Path channelmapFile) {
        return channelmapFile.getParent().resolve(getChannelmapName(probe, channelmapFile.getFileName().toString()));
    }

    public <T> T loadChannelmapFile(ProbeDescription<T> probe, String name) throws IOException {
        return loadChannelmapFile(probe, getChannelmapFile(probe, name));
    }

    public <T> T loadChannelmapFile(ProbeDescription<T> probe, Path channelmapFile) throws IOException {
        log.info("load channelmap : {}", channelmapFile);
        return probe.load(channelmapFile);
    }

    public Path saveChannelmapFilename(ProbeDescription<?> probe, Path channelmapFile) {
        var filename = channelmapFile.getFileName().toString();
        var i = filename.lastIndexOf('.');
        filename = filename.substring(0, i);

        var suffix = probe.channelMapFileSuffix().get(0);

        var dir = channelmapFile.getParent();
        Path ret;

        i = 0;
        while (Files.exists(ret = dir.resolve(filename + "_" + i + suffix))) {
            i++;
        }

        return ret;
    }

    public <T> Path saveChannelmapFile(ProbeDescription<T> probe, T chmap, String name) throws IOException {
        var ret = getChannelmapFile(probe, name);
        saveChannelmapFile(probe, chmap, ret);
        return ret;
    }

    public <T> void saveChannelmapFile(ProbeDescription<T> probe, T chmap, Path channelmapFile) throws IOException {
        Files.createDirectories(channelmapFile.getParent());
        log.info("save channelmap : {}", channelmapFile);
        probe.save(channelmapFile, chmap);
    }

    public Path getBlueprintFile(ProbeDescription<?> probe, String name) {
        return getBlueprintFile(probe, getChannelmapFile(probe, name));
    }

    public Path getBlueprintFile(ProbeDescription<?> probe, Path channelmapFile) {
        var d = channelmapFile.getParent();
        var n = channelmapFile.getFileName().toString();

        var suffix = probe.channelMapFileSuffix();
        if (suffix.isEmpty()) {
            return d.resolve(n + ".blueprint.npy");
        }

        assert n.equals(suffix.get(0));
        var len = suffix.get(0).length();
        return d.resolve(n.substring(0, n.length() - len) + ".blueprint.npy");
    }

    public List<ElectrodeDescription> loadBlueprintFile(ProbeDescription<?> probe, String name) throws IOException {
        return loadBlueprintFile(probe, getBlueprintFile(probe, name));
    }

    public List<ElectrodeDescription> loadBlueprintFile(ProbeDescription<?> probe, Path blueprintFile) throws IOException {
        if (!blueprintFile.getFileName().toString().endsWith(".blueprint.npy")) {
            blueprintFile = getBlueprintFile(probe, blueprintFile);
        }

        log.debug("load blueprint : {}", blueprintFile);
        return probe.loadBlueprint(blueprintFile);
    }

    public Path saveBlueprintFile(ProbeDescription<?> probe, List<ElectrodeDescription> blueprint, String name) throws IOException {
        var ret = getBlueprintFile(probe, name);
        saveBlueprintFile(probe, blueprint, ret);
        return ret;
    }

    public void saveBlueprintFile(ProbeDescription<?> probe, List<ElectrodeDescription> blueprint, Path blueprintFile) throws IOException {
        if (!blueprintFile.getFileName().toString().endsWith(".blueprint.npy")) {
            blueprintFile = getBlueprintFile(probe, blueprintFile);
        }

        Files.createDirectories(blueprintFile.getParent());
        log.debug("save blueprint : {}", blueprintFile);
        probe.saveBlueprint(blueprintFile, blueprint);
    }

    public Path getViewConfigFile(ProbeDescription<?> probe, String name) {
        return getViewConfigFile(probe, getChannelmapFile(probe, name));
    }

    public Path getViewConfigFile(ProbeDescription<?> probe, Path channelmapFile) {
        var d = channelmapFile.getParent();
        var n = channelmapFile.getFileName().toString();

        var suffix = probe.channelMapFileSuffix();
        if (suffix.isEmpty()) {
            return d.resolve(n + ".config.json");
        }

        assert n.equals(suffix.get(0));
        var len = suffix.get(0).length();
        return d.resolve(n.substring(0, n.length() - len) + ".config.json");
    }

    public JsonConfig loadViewConfigFile(ProbeDescription<?> probe, String name, boolean reset) throws IOException {
        return loadViewConfigFile(probe, getViewConfigFile(probe, name), reset);
    }

    public JsonConfig loadViewConfigFile(ProbeDescription<?> probe, Path viewConfigFile, boolean reset) throws IOException {
        if (!viewConfigFile.getFileName().toString().endsWith(".config.json")) {
            viewConfigFile = getBlueprintFile(probe, viewConfigFile);
        }

        log.debug("load view config : {}", viewConfigFile);
        JsonConfig c;
        try {
            c = JsonConfig.load(viewConfigFile);
        } catch (JsonProcessingException e) {
            log.warn("bad view config file : {}", viewConfigFile);
            return viewConfig;
        }

        if (reset) {
            viewConfig.clear();
        }

        viewConfig.update(c);

        return viewConfig;

    }

    public void saveViewConfigFile(ProbeDescription<?> probe, String name) throws IOException {
        saveViewConfigFile(probe, getViewConfigFile(probe, name));
    }

    public void saveViewConfigFile(ProbeDescription<?> probe, Path viewConfigFile) throws IOException {
        if (!viewConfigFile.getFileName().toString().endsWith(".config.json")) {
            viewConfigFile = getBlueprintFile(probe, viewConfigFile);
        }

        log.debug("save view config : {}", viewConfigFile);
        viewConfig.save(viewConfigFile);
    }

    public @Nullable <T> T getViewConfig(Class<T> configClass) {
        try {
            return viewConfig.get(configClass);
        } catch (JsonProcessingException e) {
            log.warn("bad {} in view config", JsonConfig.getName(configClass));
            return null;
        }
    }

    public <T> void setViewConfig(T config) {
        viewConfig.put(config);
    }

    public @Nullable <T> T getGlobalConfig(Class<T> configClass) {
        try {
            return userConfig.get(configClass);
        } catch (JsonProcessingException e) {
            log.warn("bad {} in user config", JsonConfig.getName(configClass));
            return null;
        }
    }

    public <T> void setGlobalConfig(T config) {
        userConfig.put(config);
    }
}

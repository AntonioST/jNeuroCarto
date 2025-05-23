package io.ast.jneurocarto.javafx.atlas;

import java.io.IOException;
import java.nio.file.Path;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.LoggerFactory;

import io.ast.jneurocarto.atlas.BrainGlobeConfig;
import io.ast.jneurocarto.atlas.BrainGlobeDownloader;
import io.ast.jneurocarto.atlas.cli.Main;
import io.ast.jneurocarto.core.cli.CartoConfig;

@NullMarked
public final class AtlasBrainService {

    private AtlasBrainService() {
        throw new RuntimeException();
    }

    /*=================*
     * System property *
     *=================*/

    public static boolean isPreloadAtlasBrain() {
        return Boolean.parseBoolean(System.getProperty("io.ast.jneurocarto.javafx.atlas.preload", "true"));
    }

    /*==================*
     * BrainGlobeConfig *
     *==================*/

    public static BrainGlobeConfig getConfig(CartoConfig config) {
        return getConfig(config.atlasConfig);
    }

    public static BrainGlobeConfig getConfig(CartoConfig.@Nullable AtlasConfig config) {
        try {
            Path path;
            if (config == null) {
                return BrainGlobeConfig.load();
            } else if ((path = config.atlasRoot) != null) {
                return new BrainGlobeConfig(null, path, path);
            } else if ((path = config.atlasConfig) != null) {
                return BrainGlobeConfig.load(path);
            } else {
                return BrainGlobeConfig.load();
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(AtlasBrainService.class).warn("getConfig", e);
            return BrainGlobeConfig.getDefault();
        }
    }

    public static BrainGlobeConfig getConfig(Main.ConfigOptions config) {
        return config.getConfig();
    }

    /*======================*
     * BrainGlobeDownloader *
     *======================*/

    public static BrainGlobeDownloader newDownloader(CartoConfig config) {
        return newDownloader(getConfig(config)).atlasName(config.atlasName);
    }

    public static BrainGlobeDownloader newDownloader(BrainGlobeConfig config) {
        return BrainGlobeDownloader.builder().setConfig(config);
    }

    public static BrainGlobeDownloader newDownloader(BrainGlobeConfig config, String name) {
        return newDownloader(config).atlasName(name);
    }

    /*================*
     * DownloadResult *
     *================*/

    public static BrainGlobeDownloader.DownloadResult loadAtlas(CartoConfig config) {
        return loadAtlas(getConfig(config), config.atlasName);
    }

    public static BrainGlobeDownloader.DownloadResult loadAtlas(BrainGlobeConfig config, String name) {
        return newDownloader(config).atlasName(name).download(false);
    }

    public static BrainGlobeDownloader.DownloadResult download(CartoConfig config) {
        return download(getConfig(config), config.atlasName);
    }

    public static BrainGlobeDownloader.DownloadResult download(BrainGlobeConfig config, String name) {
        return newDownloader(config).atlasName(name).download();
    }

    public static BrainGlobeDownloader.DownloadResult download(BrainGlobeDownloader downloader, String name, boolean force) {
        return downloader.atlasName(name).download(force);
    }
}

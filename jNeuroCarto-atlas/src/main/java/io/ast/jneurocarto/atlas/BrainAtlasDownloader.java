package io.ast.jneurocarto.atlas;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BrainAtlasDownloader {

    private String atlasName;
    private @Nullable Path downloadDir;
    private boolean checkLatest = true;

    private BrainAtlasDownloader(String atlasName) {
        this.atlasName = atlasName;
    }

    /*=================
     * factory methods *
     *=================*/

    public static BrainAtlasDownloader downloader(String atlasName) {
        return new BrainAtlasDownloader(atlasName);
    }

    /*===================
     * getter and setter *
     *===================*/

    public @Nullable String getAtlasName() {
        return atlasName;
    }

    public @Nullable Path getDownloadDir() {
        if (downloadDir == null) {
            downloadDir = getDefaultDownloadDir();
        }
        return downloadDir;
    }

    private static Path getDefaultDownloadDir() {
        String d;
        if ((d = System.getenv("XDG_CACHE_HOME")) != null) return Path.of(d).resolve("brainglobe");

        d = System.getProperty("user.home");
        return switch (System.getProperty("os.name")) {
            case "Linux" -> Path.of(d).resolve(".cache/brainglobe");
            default -> Path.of(d).resolve(".brainglobe");
        };
    }

    public BrainAtlasDownloader setDownloadDir(Path downloadDir) {
        this.downloadDir = downloadDir;
        return this;
    }

    public boolean isCheckLatest() {
        return checkLatest;
    }

    public BrainAtlasDownloader setCheckLatest(boolean checkLatest) {
        this.checkLatest = checkLatest;
        return this;
    }

    /*=============
     * information *
     *=============*/

    public String localFullName() {
        //XXX Unsupported Operation BrainAtlasDownloader.localFullName
        throw new UnsupportedOperationException();

    }

    public Optional<String> localVersion() {
        //XXX Unsupported Operation BrainAtlasDownloader.localVersion
        throw new UnsupportedOperationException();
    }

    /*==========
     * download *
     *==========*/

    public BrainAtlasDownloader download() throws IOException {
        return download(false);
    }

    public BrainAtlasDownloader download(boolean force) throws IOException {
        //XXX Unsupported Operation BrainAtlasDownloader.download
        throw new UnsupportedOperationException();
    }

    public BrainAtlas get() {
        //XXX Unsupported Operation BrainAtlasDownloader.get
        throw new UnsupportedOperationException();
    }

}

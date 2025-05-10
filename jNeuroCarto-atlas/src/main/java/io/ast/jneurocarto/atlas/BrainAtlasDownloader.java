package io.ast.jneurocarto.atlas;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class BrainAtlasDownloader {

    private static final String REMOTE_URL = "https://gin.g-node.org/brainglobe/atlases/raw/master/";

    private String atlasName;
    private @Nullable Path downloadDir;
    private boolean checkLatest = true;
    private Logger log = LoggerFactory.getLogger(BrainAtlasDownloader.class);

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

    public String getAtlasName() {
        return atlasName;
    }

    public Path getDownloadDir() {
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

    public Optional<String> localFullName() {
        var pattern = FileSystems.getDefault().getPathMatcher("glob:" + atlasName + "_v*");
        try (var dirs = Files.list(getDownloadDir())) {
            return dirs.filter(pattern::matches)
              .findFirst()
              .map(it -> it.getFileName().toString());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public Optional<String> localVersion() {
        return localFullName().map(it -> it.replaceFirst(".*_v", ""));
    }

    public Optional<String> remoteVersion() {
        var remote = REMOTE_URL + "last_versions.conf";

        Properties property;
        try {
            log.debug("curl:{}", remote);
            var content = curl(remote);

            property = new Properties();
            property.load(new StringReader(content));
        } catch (Exception e) {
            log.warn("curl", e);
            return Optional.empty();
        }

        return Optional.ofNullable(property.getProperty(atlasName));
    }

    private static String curl(String url) throws IOException, InterruptedException {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public Optional<String> remoteUrl() {
        return remoteVersion().map(it -> String.format("%s/%s_v%s.tar.gz", REMOTE_URL, atlasName, it));
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

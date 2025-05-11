package io.ast.jneurocarto.atlas;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

@NullMarked
public class BrainGlobeDownloader {

    public static final String REMOTE_URL = "https://gin.g-node.org/brainglobe/atlases/raw/master/";
    public static final String LAST_VERSION_FILENAME = "last_versions.conf";

    private BrainGlobeConfig config;
    private @Nullable String atlasName;
    private @Nullable Path downloadDir;
    private boolean checkLatest = true;
    private boolean dryrun = false;
    private Logger log = LoggerFactory.getLogger(BrainGlobeDownloader.class);

    private BrainGlobeDownloader() {
        BrainGlobeConfig config;
        try {
            config = BrainGlobeConfig.load();
        } catch (IOException e) {
            log.warn("BrainGlobeDownloader(BrainGlobeConfig.load)", e);
            config = BrainGlobeConfig.getDefault();
        }
        this.config = config;
    }

    /*=================
     * factory methods *
     *=================*/

    public static BrainGlobeDownloader builder() {
        return new BrainGlobeDownloader();
    }

    /*===================
     * getter and setter *
     *===================*/

    public @Nullable String atlasName() {
        return atlasName;
    }

    public BrainGlobeDownloader atlasName(@Nullable String atlasName) {
        this.atlasName = atlasName;
        return this;
    }

    public Path getDownloadDir() {
        if (downloadDir == null) {
            downloadDir = Path.of(config.getBrainGlobeDir());
        }
        return downloadDir;
    }

    public BrainGlobeDownloader setDownloadDir(Path downloadDir) {
        this.downloadDir = downloadDir;
        return this;
    }

    public boolean isCheckLatest() {
        return checkLatest;
    }

    public BrainGlobeDownloader setCheckLatest(boolean checkLatest) {
        this.checkLatest = checkLatest;
        return this;
    }

    public boolean dryrun() {
        return dryrun;
    }

    public BrainGlobeDownloader dryrun(boolean dryrun) {
        this.dryrun = dryrun;
        return this;
    }

    public BrainGlobeDownloader setConfig(BrainGlobeConfig config) {
        this.config = config;
        downloadDir = Path.of(config.getBrainGlobeDir());
        return this;
    }

    /*=============*
     * information *
     *=============*/

    public Optional<String> localFullName() {
        var atlas = Objects.requireNonNull(atlasName, "miss getAtlasName()");
        return localFullName(atlas);
    }

    public Optional<String> localFullName(String atlas) {
        var pattern = FileSystems.getDefault().getPathMatcher("glob:" + atlas + "_v*");
        try (var dirs = Files.list(getDownloadDir())) {
            return dirs
              .filter(Files::isDirectory)
              .map(Path::getFileName)
              .filter(pattern::matches)
              .findFirst()
              .map(Path::toString);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public Optional<String> localVersion() {
        return localFullName().map(it -> it.replaceFirst(".*_v", ""));
    }

    public Optional<String> localVersion(String atlas) {
        return localFullName(atlas).map(it -> it.replaceFirst(".*_v", ""));
    }

    public Path getLastVersionFile() {
        return getDownloadDir().resolve(LAST_VERSION_FILENAME);
    }

    public Optional<String> remoteVersion() {
        return remoteVersion(true);
    }

    /**
     * @param force force download.
     * @return
     */
    public Optional<String> remoteVersion(boolean force) {
        var atlas = Objects.requireNonNull(atlasName, "miss getAtlasName()");
        var prop = getLastVersions(force);
        if (prop == null) return Optional.empty();
        return Optional.ofNullable(prop.get(atlas));
    }

    private @Nullable Map<String, String> lastVersionCache;

    public @Nullable Map<String, String> getLastVersions() {
        return getLastVersions(false);
    }

    /**
     * @param force force download.
     * @return
     */
    public @Nullable Map<String, String> getLastVersions(boolean force) {
        if (lastVersionCache != null && !force) {
            return lastVersionCache;
        }

        var file = getLastVersionFile();

        String content = force ? null : loadLastVersion(file);
        if (content == null) {
            var remote = REMOTE_URL + LAST_VERSION_FILENAME;
            content = downloadLastVersion(file, remote);
            if (content == null) {
                return null;
            }
        }

        try {
            var property = new Properties();
            property.load(new StringReader(content));
            property.remove("[atlases]");
            lastVersionCache = new HashMap<>();
            property.forEach((k, v) -> lastVersionCache.put((String) k, (String) v));
            return lastVersionCache;
        } catch (IOException e) {
            log.warn("getLastVersion", e);
            return null;
        }
    }

    private @Nullable String loadLastVersion(Path file) {
        if (!Files.exists(file)) return null;

        try {
            log.debug("read {}", file);
            return Files.readString(file);
        } catch (IOException e) {
            log.warn("loadLastVersion", e);
            return null;
        }
    }

    private @Nullable String downloadLastVersion(Path file, String url) {
        if (dryrun) return null;

        String content;

        try {
            log.debug("curl {}", url);
            content = curlText(url).get();
        } catch (IOException e) {
            log.warn("downloadLastVersion", e);
            return null;
        }

        log.debug("write {}", file);
        try (var writer = Files.newBufferedWriter(file, CREATE, TRUNCATE_EXISTING)) {
            writer.write(content);
        } catch (IOException e) {
            log.warn("downloadLastVersion", e);
        }
        return content;
    }


    public boolean checkLatestVersion() {
        var atlas = Objects.requireNonNull(atlasName, "miss getAtlasName()");
        return checkLatestVersion(atlas);
    }

    public boolean checkLatestVersion(String atlas) {
        var prop = getLastVersions();
        if (prop == null) return false;

        var version = prop.get(atlas);
        if (version == null) return false;

        return localVersion(atlas).map(version::equals).orElse(false);
    }

    /*==========*
     * download *
     *==========*/

    public Optional<String> remoteUrl() {
        var atlas = Objects.requireNonNull(atlasName, "miss getAtlasName()");
        return remoteUrl(atlas);
    }

    public Optional<String> remoteUrl(String atlas) {
        return remoteVersion().map(it -> remoteUrl(atlas, it));
    }

    public String remoteUrl(String atlas, String version) {
        return String.format("%s%s_v%s.tar.gz", REMOTE_URL, atlas, version);
    }

    public DownloadResult download() throws IOException {
        return download(false);
    }

    public DownloadResult download(boolean force) throws IOException {
        var atlas = Objects.requireNonNull(atlasName, "miss getAtlasName()");

        var versions = getLastVersions();
        if (versions == null) {
            throw new RuntimeException("unable to download " + LAST_VERSION_FILENAME);
        }
        if (!versions.containsKey(atlas)) {
            throw new IllegalArgumentException(atlas + " is not a valid atlas name!");
        }
        var remote = versions.get(atlas);
        assert remote != null;
        log.debug("remote version {}={}", atlas, remote);

        var downloadDir = getDownloadDir();
        var local = localVersion(atlas);
        local.ifPresent(it -> log.debug("local version {}={}", atlas, it));

        if (!force && local.isPresent() && remote.equals(local.get())) {
            return new DownloadResult(atlas, downloadDir.resolve(local.get()));
        }

        if (!force) log.info("{} local version is out-of-date", atlas);

        if (dryrun) throw new RuntimeException("dry run mode");

        var remoteVersion = versions.get(atlas);
        assert remoteVersion != null;

        var remoteUrl = remoteUrl(atlas, remoteVersion);
        log.info("download {} from {}", atlas, remoteUrl);

        var i = remoteUrl.lastIndexOf('/');
        var filename = remoteUrl.substring(i + 1);
        var downloadFile = Path.of(config.getInternDownloadDir()).resolve(filename);
        try {
            Files.createDirectories(downloadFile.getParent());
            if (!curlBinary(remoteUrl, downloadFile)) {
                throw new IOException("download fail");
            }

            log.debug("extra to {}", downloadDir);
            var atlasDir = extract(downloadDir, downloadFile);
            return new DownloadResult(atlas, atlasDir);
        } finally {
            Files.deleteIfExists(downloadFile);
        }
    }

    private static Path extract(Path output, Path downloadFile) throws IOException {
        Path ret = null;

        Files.createDirectories(output);
        try (var input = Files.newInputStream(downloadFile);
             var gzi = new GZIPInputStream(new BufferedInputStream(input));
             var tar = new TarArchiveInputStream(gzi)) {

            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                var path = output.resolve(entry.getName()).normalize();
                if (!path.startsWith(output)) {
                    throw new IOException("unsafe tar unpacking : " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(path);
                    if (ret == null) {
                        ret = output.resolve(output.relativize(path).getName(0));
                    }
                } else {
                    Files.createDirectories(path.getParent());
                    try (var out = Files.newOutputStream(path, CREATE, TRUNCATE_EXISTING)) {
                        tar.transferTo(out);
                    }
                }
            }
        }

        if (ret == null) {
            throw new IOException("none directory is extracted");
        }

        return ret;
    }

    public record DownloadResult(String atlas, Path path) {

        public String version() {
            return path.getFileName().toString().replaceFirst(".*_v", "");
        }

        public BrainAtlas get() throws IOException {
            return new BrainAtlas(path);
        }
    }

    /*======*
     * http *
     *======*/

    private static Optional<String> curlText(String url) throws IOException {
        var request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .build();

        HttpResponse<String> response;
        try (var client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }

        if (response.statusCode() == 200) {
            return Optional.of(response.body());
        } else {
            return Optional.empty();
        }
    }

    private static boolean curlBinary(String url, Path file) throws IOException {

        var request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .build();

        HttpResponse<Path> response;
        try (var client = HttpClient.newHttpClient()) {
            response = client.send(request, HttpResponse.BodyHandlers.ofFile(file));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return response.statusCode() == 200;
    }
}

/// Workaround for maven-javadoc-plugin snippet resolution bug in modular aggregate builds.
///
/// ### Problem:
///
/// When using the `javadoc:aggregate` goal with JPMS (`module-info.java`), and Javadoc snippets,
/// the Maven Javadoc plugin generates and uses a temporary `--module-source-path` pointing to:
///
///     target/reports/apidocs/src/<module-name>/
///
/// However, it does not copy or link the actual source files or `module-info.java` into that location.
/// As a result, any {\@snippet} tags in Javadoc comments fail because the snippet source files
/// (e.g., `snippet-files/Example.java`) cannot be resolved from the expected package paths.
///
/// ### Solution:
///
/// This script manually links real source directories (and `module-info.java`) for each module
/// into the staging directory expected by the plugin. This allows the aggregate Javadoc task
/// to resolve snippet files correctly using the default `--module-source-path` layout.
///
/// ### Usage:
///
/// It is used by exec-maven-plugin with goal the `exec:exec@fix-javadoc-src`.
/// It is ran before `javadoc:aggregate`.
///
void main(String[] args) {
    var d = Path.of(args[0]);

    for (int i = 1, length = args.length; i < length; i+= 2) {
        var m = args[i];
        var s = args[i + 1];
        var md = d.resolve(m);
        var sd = Path.of(s);

        if (!Files.isDirectory(sd)) {
            IO.println(sd + " not a directory");
        } else {
            try {
                if (!Files.isDirectory(md)) {
                    Files.createDirectories(md);
                }

                try (var ds = Files.list(sd)) {
                    var dl = ds.toList();
                    for (var p : dl) {
                        var t = md.resolve(p.getFileName());
                        Files.deleteIfExists(t);
                        Files.createSymbolicLink(t, p.toAbsolutePath());
                    }
                }
            } catch (IOException e) {
                IO.println(e);
            }
        }
    }
}
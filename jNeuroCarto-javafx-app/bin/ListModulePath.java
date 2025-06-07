import static java.nio.file.StandardOpenOption.*;

void main(String[] args) throws IOException {
    var inputdir = Path.of(args[0]);
    var outputFile = Path.of(args[1]);

    try (var out = new PrintStream(Files.newOutputStream(outputFile, CREATE, TRUNCATE_EXISTING));
         var dir = Files.list(inputdir)) {
        var line = dir.filter(Files::isRegularFile)
            .filter(f -> f.getFileName().toString().endsWith(".jar"))
            .map(Path::toString)
            .collect(Collectors.joining(":"));
        out.println(line);
    }
}
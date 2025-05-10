package io.ast.jneurocarto.atlas;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Structures implements Iterable<Structure> {

    private Structure[] structures;

    public Structures(Structure[] structures) {
        this.structures = structures;
    }

    public static Structures load(Path file) throws IOException {
        if (!Files.exists(file)) throw new FileNotFoundException(file.toString());
        var data = new ObjectMapper().readValue(file.toFile(), Structure[].class);
        return new Structures(data);
    }

    @Override
    public Iterator<Structure> iterator() {
        return Arrays.asList(structures).iterator();
    }
}

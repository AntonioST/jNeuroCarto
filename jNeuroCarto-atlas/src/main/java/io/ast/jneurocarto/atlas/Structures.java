package io.ast.jneurocarto.atlas;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import org.jspecify.annotations.NullMarked;
import com.fasterxml.jackson.databind.ObjectMapper;

@NullMarked
public class Structures implements Iterable<Structure> {

    private final Structure[] structures;

    public Structures(Structure[] structures) {
        this.structures = structures;
    }

    public static Structures load(Path file) throws IOException {
        if (!Files.exists(file)) throw new FileNotFoundException(file.toString());
        var data = new ObjectMapper().readValue(file.toFile(), Structure[].class);
        return new Structures(data);
    }

    public Optional<Structure> get(int id) {
        for (var structure : structures) {
            if (structure.id() == id) return Optional.of(structure);
        }
        return Optional.empty();
    }

    public Optional<Structure> get(String acronym) {
        for (var structure : structures) {
            if (structure.acronym().equals(acronym)) return Optional.of(structure);
        }
        return Optional.empty();
    }

    public Optional<Structure> parent(int id) {
        return get(id).map(this::parent);
    }

    public Optional<Structure> parent(String acronym) {
        return get(acronym).map(this::parent);
    }

    public Structure parent(Structure s) {
        return get(s.parent()).get();
    }

    public Optional<List<Structure>> parents(int id) {
        return get(id).map(this::parents);
    }

    public Optional<List<Structure>> parents(String acronym) {
        return get(acronym).map(this::parents);
    }

    public List<Structure> parents(Structure s) {
        var ret = new ArrayList<Structure>();
        Structure p = s;
        while (!p.isRoot()) {
            ret.add(p = parent(p));
        }
        return ret;
    }

    public Optional<List<Structure>> children(int id) {
        return get(id).map(this::children);
    }

    public Optional<List<Structure>> children(String acronym) {
        return get(acronym).map(this::children);
    }

    public List<Structure> children(Structure parent) {
        var ret = new ArrayList<Structure>();
        for (var s : structures) {
            if (s.parent() == parent.id()) ret.add(s);
        }
        return ret;
    }

    public void forAllChildren(int id, Consumer<Structure> consumer) {
        get(id).ifPresent(it -> forAllChildren(it, consumer));
    }

    public void forAllChildren(String acronym, Consumer<Structure> consumer) {
        get(acronym).ifPresent(it -> forAllChildren(it, consumer));
    }

    public void forAllChildren(Structure parent, Consumer<Structure> consumer) {
        for (var s : structures) {
            if (s.parent() == parent.id()) {
                consumer.accept(s);
                forAllChildren(s, consumer);
            }
        }
    }

    @Override
    public Iterator<Structure> iterator() {
        return Arrays.asList(structures).iterator();
    }
}

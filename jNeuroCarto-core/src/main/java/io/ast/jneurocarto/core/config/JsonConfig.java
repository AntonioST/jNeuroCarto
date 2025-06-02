package io.ast.jneurocarto.core.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

@NullMarked
public class JsonConfig {

    private static final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, JsonNode> maps;

    public JsonConfig() {
        maps = new HashMap<>();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    private JsonConfig(Map<String, JsonNode> maps) {
        this.maps = maps;
    }

    public static JsonConfig load(Path file) throws IOException {
        if (!Files.exists(file)) throw new FileNotFoundException(file.toString());
        var node = mapper.readTree(file.toFile());
        return asConfig((ObjectNode) node);
    }

    public static JsonConfig load(InputStream file) throws IOException {
        var node = mapper.readTree(file);
        return asConfig((ObjectNode) node);
    }

    private static JsonConfig asConfig(ObjectNode node) {
        var ret = new HashMap<String, JsonNode>(node.size());
        for (var iter = node.fields(); iter.hasNext(); ) {
            var entry = iter.next();
            ret.put(entry.getKey(), entry.getValue());
        }
        return new JsonConfig(ret);
    }

    public void save(Path file) throws IOException {
        Files.createDirectories(file.toAbsolutePath().getParent());
        mapper.writeValue(file.toFile(), maps);
    }

    public boolean contains(String name) {
        return maps.containsKey(name);
    }

    public boolean contains(Class<?> clazz) {
        return contains(getName(clazz));
    }

    public static String getName(Class<?> clazz) {
        var root = clazz.getAnnotation(JsonRootName.class);
        return root != null ? root.value() : clazz.getSimpleName();
    }

    public Set<String> fields() {
        return maps.keySet();
    }

    public <T> @Nullable T get(Class<T> clazz) throws JsonProcessingException {
        return get(clazz, getName(clazz));
    }

    public <T> @Nullable T get(Class<T> clazz, String name) throws JsonProcessingException {
        var value = maps.get(name);
        if (value == null) return null;
        return mapper.treeToValue(value, clazz);
    }

    public <T> @Nullable List<T> getAsList(Class<T> clazz) throws JsonProcessingException {
        return getAsList(clazz, getName(clazz));
    }

    public <T> @Nullable List<T> getAsList(Class<T> clazz, String name) throws JsonProcessingException {
        var value = maps.get(name);
        if (value == null) return null;
        return mapper.treeToValue(value, new TypeReference<>() {
        });
    }

    public @Nullable JsonConfig getAsConfig(String name) throws JsonProcessingException {
        var value = maps.get(name);
        if (value instanceof ObjectNode obj) {
            return asConfig(obj);
        } else {
            return null;
        }
    }

    public void put(Object value) {
        put(getName(value.getClass()), value);
    }

    public void put(String name, Object value) {
        maps.put(name, mapper.valueToTree(value));
    }

    public void update(JsonConfig config) {
        maps.putAll(config.maps);
    }

    public void clear() {
        maps.clear();
    }

    public Set<String> keySet() {
        return maps.keySet();
    }

    public Set<Entry> entrySet() {
        return maps.entrySet().stream()
            .map(Entry::new)
            .collect(Collectors.toSet());
    }

    public void forEach(Consumer<Entry> consumer) {
        Objects.requireNonNull(consumer);
        maps.forEach((name, node) -> consumer.accept(new Entry(name, node)));
    }

    public static final class Entry {
        private final String name;
        private final JsonNode node;

        Entry(String name, JsonNode node) {
            this.name = name;
            this.node = node;
        }

        Entry(Map.Entry<String, JsonNode> entry) {
            this(entry.getKey(), entry.getValue());
        }

        public String name() {
            return name;
        }

        public JsonNode node() {
            return node;
        }

        public <T> T get(Class<T> clazz) throws JsonProcessingException {
            return mapper.treeToValue(node, clazz);
        }

        public <T> List<T> getAsList(Class<T> clazz) throws JsonProcessingException {
            return mapper.treeToValue(node, new TypeReference<>() {
            });
        }
    }

}

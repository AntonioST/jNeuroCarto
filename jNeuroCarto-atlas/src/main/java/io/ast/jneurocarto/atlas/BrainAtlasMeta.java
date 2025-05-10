package io.ast.jneurocarto.atlas;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BrainAtlasMeta {
    /* {
    "name": "allen_mouse",
    "citation": "Wang et al 2020, https://doi.org/10.1016/j.cell.2020.04.007",
    "atlas_link": "http://www.brain-map.org",
    "species": "Mus musculus",
    "symmetric": true,
    "resolution": [25.0, 25.0, 25.0],
    "orientation": "asr",
    "version": "1.2",
    "shape": [528, 320, 456],
    "trasform_to_bg": [[1.0, 0.0, 0.0, 0.0], [0.0, 1.0, 0.0, 0.0], [0.0, 0.0, 1.0, 0.0], [0.0, 0.0, 0.0, 1.0]],
    "additional_references": []}
    */

    public String name;

    public String citation;

    @JsonAlias("atlas_link")
    public String atlasLink;

    public String species;

    public boolean symmetric;

    public double[] resolution;

    public String orientation;

    public String version;

    public int[] shape;

    @JsonAlias("trasform_to_bg")
    public double[][] transform2Bg;

    @JsonAlias("additional_references")
    public List<String> additionalReferences;

    public static BrainAtlasMeta load(Path file) throws IOException {
        if (!Files.exists(file)) throw new FileNotFoundException(file.toString());
        return new ObjectMapper().readValue(file.toFile(), BrainAtlasMeta.class);
    }

    @Override
    public String toString() {
        return "BrainAtlasMeta{" +
               "name='" + name + '\'' +
               ", citation='" + citation + '\'' +
               ", atlasLink='" + atlasLink + '\'' +
               ", species='" + species + '\'' +
               ", symmetric=" + symmetric +
               ", resolution=" + Arrays.toString(resolution) +
               ", orientation='" + orientation + '\'' +
               ", version='" + version + '\'' +
               ", shape=" + Arrays.toString(shape) +
               ", transform2Bg=" + Arrays.deepToString(transform2Bg) +
               ", additionalReferences=" + additionalReferences +
               '}';
    }
}

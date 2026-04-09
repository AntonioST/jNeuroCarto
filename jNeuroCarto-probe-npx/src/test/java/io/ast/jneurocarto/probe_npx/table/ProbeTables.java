package io.ast.jneurocarto.probe_npx.table;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

@NullMarked
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProbeTables {

    @JsonProperty("neuropixels_probes")
    public Map<String, ProbeInfo> probes;

    @JsonProperty("z_imro_formats")
    public ProbeFormats formats;

    public static ProbeTables readFrom(InputStream stream) throws IOException {
        var mapper = new ObjectMapper();
        return mapper.readValue(stream, ProbeTables.class);
    }
}

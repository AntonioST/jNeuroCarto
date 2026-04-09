package io.ast.jneurocarto.probe_npx.table;

import java.util.Arrays;

import org.jspecify.annotations.NullMarked;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@NullMarked
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProbeInfo {
    @JsonProperty("datasheet")
    public String datasheet;

    @JsonProperty("description")
    public String description;

    @JsonProperty("electrode_pitch_horz_um")
    public float electrodeHorzUm;

    @JsonProperty("electrode_pitch_vert_um")
    public float electrodeVertUm;

    @JsonProperty("shank_pitch_um")
    public float shankUm;

    @JsonProperty("cols_per_shank")
    public float columnsPerShank;

    @JsonProperty("rows_per_shank")
    public int rowsPerShank;

    @JsonProperty("electrodes_per_shank")
    public int electrodesPerShank;

    @JsonProperty("total_electrodes")
    public int electrodesTotal;

    @JsonProperty("num_shanks")
    public int numShanks;

    @JsonProperty("num_readout_channels")
    public int numChannels;

    @JsonProperty("channels_per_bank")
    public int channelsPerBank;

    public int[] apGainList;

    @JsonProperty("ap_gain_list")
    public void setApGainList(String value) {
        apGainList = Arrays.stream(value.split(",")).mapToInt(Integer::parseInt).toArray();
    }

    public int[] lfGainList;

    @JsonProperty("lf_gain_list")
    public void setLfGainList(String value) {
        lfGainList = Arrays.stream(value.split(",")).mapToInt(Integer::parseInt).toArray();
    }


}

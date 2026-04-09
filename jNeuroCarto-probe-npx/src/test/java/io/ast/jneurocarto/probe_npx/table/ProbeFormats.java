package io.ast.jneurocarto.probe_npx.table;

import java.util.*;
import java.util.regex.Pattern;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@NullMarked
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProbeFormats {

    private final Map<String, Map<String, String>> formats = new HashMap<>();

    @JsonAnySetter
    public void add(String key, String value) {
        if (key.startsWith("imro_") && key.endsWith("_val_def")) {
            var name = key.replaceFirst("imro_(.+?)_val_def", "$1");
            var format = new HashMap<String, String>();
            for (var element : value.split(" ")) {
                var i = element.indexOf(':');
                var k = element.substring(0, i).intern();
                var v = element.substring(i + 1);
                format.put(k, v);
            }
            formats.put(name, format);
        }
    }

    public Set<String> keys() {
        return formats.keySet();
    }

    public ProbeFormat get(String name) {
        var f = Objects.requireNonNull(formats.get(name), "unknown name : " + name);
        return new ProbeFormat(name, f);
    }

    public static class ProbeFormat {
        public final String name;
        private final Map<String, String> format;

        public ProbeFormat(String name, Map<String, String> format) {
            this.name = name;
            this.format = format;
        }

        public int[] types() {
            var t = Objects.requireNonNull(format.get("type"), "type");
            if (t.startsWith("{") && t.endsWith("}")) {
                return Arrays.stream(t.split("[{},]"))
                  .filter(it -> !it.isEmpty())
                  .mapToInt(Integer::parseInt)
                  .toArray();
            } else {
                return new int[]{Integer.parseInt(t)};
            }
        }

        public String numChannels() {
            return format.get("num_channels");
        }

        public @Nullable Range channel() {
            var r = format.get("channel");
            return r == null ? null : Range.parse(r);
        }

        public @Nullable Range electrode() {
            var r = format.get("electrode");
            return r == null ? null : Range.parse(r);
        }

        public @Nullable Range bank() {
            var r = format.get("bank");
            return r == null ? null : Range.parse(r);
        }

        public String refIdsExpr() {
            return format.get("ref_id");
        }

        public List<String[]> refIds() {
            var refs = Objects.requireNonNull(format.get("ref_id"), "ref_id");
            var pattern = Pattern.compile("\\((\\d+),(.+?)\\)");
            var matcher = pattern.matcher(refs);
            var ret = new ArrayList<String[]>();
            int start = 0;
            while (matcher.find(start)) {
                var code = Integer.parseInt(matcher.group(1));
                var content = matcher.group(2).split(",");
                if (ret.size() == code) {
                    ret.add(content);
                } else {
                    throw new RuntimeException("ref code mismatch");
                }
                start = matcher.end();
            }
            return ret;
        }

        public @Nullable String bankMasksExpr() {
            return format.get("bank_mask");
        }

        public @Nullable List<String[]> bankMasks() {
            var masks = format.get("bank_mask");
            if (masks == null) return null;
            var pattern = Pattern.compile("\\((.+?)\\)");
            var matcher = pattern.matcher(masks);
            var ret = new ArrayList<String[]>();
            int start = 0;
            while (matcher.find(start)) {
                ret.add(matcher.group(1).split(","));
                start = matcher.end();
            }
            return ret;
        }

        public @Nullable String colModeExpr() {
            return format.get("col_mode");
        }

        public @Nullable List<String> colMode() {
            var modes = format.get("col_mode");
            if (modes == null) return null;
            var pattern = Pattern.compile("\\((\\d+),(\\w+)\\)");
            var matcher = pattern.matcher(modes);
            var ret = new ArrayList<String>();
            int start = 0;
            while (matcher.find(start)) {
                var code = Integer.parseInt(matcher.group(1));
                var mode = matcher.group(2);
                if (ret.size() == code) {
                    ret.add(mode);
                } else {
                    throw new RuntimeException("mode code mismatch : ret=" + ret + " != code=" + code + ", col_mode=" + modes);
                }
                start = matcher.end();
            }
            return ret;
        }

        public @Nullable String apGain() {
            return format.get("ap_gain");
        }

        public @Nullable String lfGain() {
            return format.get("lf_gain");
        }

        public @Nullable Range apHighPassFilter() {
            var r = format.get("ap_hipas_flt");
            return r == null ? null : Range.parse(r);
        }
    }

    public record Range(String from, String to) {
        public static Range parse(String content) {
            if (!content.startsWith("[") || !content.endsWith("]")) {
                throw new IllegalArgumentException("not a range : " + content);
            }

            var i = content.indexOf(',');
            var f = content.substring(1, i);
            var t = content.substring(i + 1, content.length() - 1);
            return new Range(f, t);
        }
    }
}

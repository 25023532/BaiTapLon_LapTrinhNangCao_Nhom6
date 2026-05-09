package org.example.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JSON helper — không dùng thư viện ngoài (zero-dep).
 * Chỉ xử lý flat Map<String,String> để giữ đơn giản.
 * Production: thay bằng Jackson / Gson.
 */
public class JsonUtil {

    /** Map → JSON string */
    public static String toJson(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{");
            map.forEach((k, v) -> {
                sb.append("\"").append(k).append("\":");
                if (v instanceof String)
                    sb.append("\"").append(v).append("\"");
                else if (v instanceof Map || v instanceof java.util.List)
                    sb.append(toJson(v));
                else
                    sb.append(v);
                sb.append(",");
            });
            if (sb.charAt(sb.length()-1) == ',')
                sb.deleteCharAt(sb.length()-1);
            return sb.append("}").toString();
        }
        if (obj instanceof java.util.List<?> list) {
            StringBuilder sb = new StringBuilder("[");
            list.forEach(item -> sb.append(toJson(item)).append(","));
            if (sb.charAt(sb.length()-1) == ',')
                sb.deleteCharAt(sb.length()-1);
            return sb.append("]").toString();
        }
        return String.valueOf(obj);
    }

    /** JSON string → flat Map<String,String> (single-level) */
    public static Map<String, String> parseFlat(String json) {
        Map<String, String> map = new LinkedHashMap<>();
        String s = json.trim().replaceAll("^\\{|\\}$", "");
        for (String pair : s.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replaceAll("\"", "");
                String val = kv[1].trim().replaceAll("\"", "");
                map.put(key, val);
            }
        }
        return map;
    }

    /** JSON string → Object (Map hoặc List) */
    @SuppressWarnings("unchecked")
    public static Object parse(String json) {
        return parseFlat(json); // simplified — dùng Jackson trong production
    }
}

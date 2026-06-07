package com.netscout;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Writes scan results to a JSON file using only the standard library.
 * (No Gson / Jackson — keeping the project dependency-free.)
 *
 * Output format:
 * {
 *   "scan_time": "2025-07-15T14:30:00",
 *   "total_hosts": 3,
 *   "results": [
 *     {
 *       "ip": "192.168.1.1",
 *       "hostname": "router.local",
 *       "latency_ms": 4,
 *       "open_ports": [22, 80, 443]
 *     },
 *     ...
 *   ]
 * }
 */
public class JsonExporter {

    public static void export(List<ScanResult> results, String filePath) throws IOException {
        // Make sure parent directories exist
        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"scan_time\": \"").append(timestamp).append("\",\n");
        json.append("  \"total_hosts\": ").append(results.size()).append(",\n");
        json.append("  \"results\": [\n");

        for (int i = 0; i < results.size(); i++) {
            ScanResult r = results.get(i);
            json.append("    {\n");
            json.append("      \"ip\": \"").append(r.ip).append("\",\n");
            json.append("      \"hostname\": ")
                .append(r.hostname != null ? "\"" + escape(r.hostname) + "\"" : "null")
                .append(",\n");
            json.append("      \"latency_ms\": ").append(r.latencyMs).append(",\n");
            json.append("      \"open_ports\": ").append(toJsonArray(r.openPorts)).append("\n");
            json.append("    }");
            if (i < results.size() - 1) json.append(",");
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}\n");

        Files.writeString(path, json.toString());
    }

    // -------------------------------------------------------------------------

    private static String toJsonArray(List<Integer> list) {
        if (list.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    /** Minimal JSON string escaping. */
    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

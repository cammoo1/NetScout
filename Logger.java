package com.netscout;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Simple logger that writes timestamped lines to both stdout (INFO level)
 * and optionally a file under logs/.
 *
 * File name format: logs/netscout_2025-07-15_143000.log
 */
public class Logger {

    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter FILE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss");

    private final boolean   fileEnabled;
    private BufferedWriter  writer;

    public Logger(boolean fileEnabled) {
        this.fileEnabled = fileEnabled;
        if (fileEnabled) {
            try {
                Files.createDirectories(Paths.get("logs"));
                String name = "logs/netscout_"
                        + LocalDateTime.now().format(FILE_FORMAT) + ".log";
                writer = new BufferedWriter(new FileWriter(name, true));
                System.out.println("[INFO] Logging to: " + name);
            } catch (IOException e) {
                System.err.println("[WARN] Could not open log file: " + e.getMessage());
                fileEnabled = false; // fall back to console-only
            }
        }
    }

    public void log(String message) {
        String line = "[" + LocalDateTime.now().format(TS_FORMAT) + "] " + message;
        if (fileEnabled && writer != null) {
            try {
                writer.write(line);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                // Silently skip log write failures during a scan
            }
        }
    }

    public void close() {
        if (writer != null) {
            try { writer.close(); } catch (IOException ignored) {}
        }
    }
}

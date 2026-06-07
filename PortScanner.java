package com.netscout;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;

/**
 * Scans a list of TCP ports on each live host using a shared thread pool.
 *
 * A port is considered "open" if a TCP connection succeeds within the timeout.
 * Results are added directly to each ScanResult's openPorts list.
 */
public class PortScanner {

    // Friendly names for the most common ports — purely for display
    private static final java.util.Map<Integer, String> SERVICE_NAMES = new java.util.HashMap<>();

    static {
        SERVICE_NAMES.put(21,   "FTP");
        SERVICE_NAMES.put(22,   "SSH");
        SERVICE_NAMES.put(23,   "Telnet");
        SERVICE_NAMES.put(25,   "SMTP");
        SERVICE_NAMES.put(53,   "DNS");
        SERVICE_NAMES.put(80,   "HTTP");
        SERVICE_NAMES.put(110,  "POP3");
        SERVICE_NAMES.put(143,  "IMAP");
        SERVICE_NAMES.put(443,  "HTTPS");
        SERVICE_NAMES.put(3306, "MySQL");
        SERVICE_NAMES.put(3389, "RDP");
        SERVICE_NAMES.put(8080, "HTTP-Alt");
        SERVICE_NAMES.put(8443, "HTTPS-Alt");
    }

    private final int    threads;
    private final int    timeoutMs;
    private final Logger logger;

    public PortScanner(int threads, int timeoutMs, Logger logger) {
        this.threads   = threads;
        this.timeoutMs = timeoutMs;
        this.logger    = logger;
    }

    /**
     * For each host in `results`, tries to open a TCP socket on every port in `ports`.
     * Populates result.openPorts for each successful connection.
     */
    public void scan(List<ScanResult> results, List<Integer> ports) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (ScanResult host : results) {
            for (int port : ports) {
                final int p = port;
                pool.submit(() -> {
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress(host.ip, p), timeoutMs);
                        // If we get here, the port accepted the connection
                        synchronized (host.openPorts) {
                            host.openPorts.add(p);
                        }
                        String service = SERVICE_NAMES.getOrDefault(p, "unknown");
                        System.out.printf("  [open]  %s : %d/%s%n", host.ip, p, service);
                        logger.log("OPEN  " + host.ip + ":" + p + " (" + service + ")");
                    } catch (Exception ignored) {
                        // Port is closed or filtered, expected, not an error
                    }
                });
            }
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.MINUTES);

        // Sort open ports for cleaner output
        for (ScanResult r : results) {
            java.util.Collections.sort(r.openPorts);
        }
    }
}

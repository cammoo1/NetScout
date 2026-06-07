package com.netscout;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pings every IP in the provided list concurrently using a thread pool.
 * For each live host, records:
 *   - Latency (ms)
 *   - Resolved hostname (reverse DNS)
 *
 * Uses InetAddress.isReachable(); works on all platforms.
 */
public class HostDiscovery {

    private final int     threads;
    private final int     timeoutMs;
    private final Logger  logger;

    public HostDiscovery(int threads, int timeoutMs, Logger logger) {
        this.threads   = threads;
        this.timeoutMs = timeoutMs;
        this.logger    = logger;
    }

    /** Returns a list of ScanResult objects for every reachable host. */
    public List<ScanResult> discover(List<String> hosts) throws InterruptedException {
        List<ScanResult> live    = Collections.synchronizedList(new ArrayList<>());
        ExecutorService  pool    = Executors.newFixedThreadPool(threads);
        AtomicInteger    scanned = new AtomicInteger(0);
        int              total   = hosts.size();

        for (String ip : hosts) {
            pool.submit(() -> {
                try {
                    InetAddress addr  = InetAddress.getByName(ip);
                    long        start = System.currentTimeMillis();
                    boolean     alive = addr.isReachable(timeoutMs);
                    long        end   = System.currentTimeMillis();

                    if (alive) {
                        ScanResult result   = new ScanResult(ip);
                        result.latencyMs    = end - start;

                        // Reverse DNS — getCanonicalHostName() returns the IP 
                        // if no PTR record exists, we filter that out.
                        String canonical = addr.getCanonicalHostName();
                        result.hostname  = canonical.equals(ip) ? null : canonical;

                        live.add(result);

                        String display = result.hostname != null
                                ? ip + " (" + result.hostname + ")"
                                : ip;
                        System.out.printf("[+] %-45s  %dms%n", display, result.latencyMs);
                        logger.log("LIVE  " + display + "  latency=" + result.latencyMs + "ms");
                    }

                } catch (Exception e) {
                    logger.log("ERROR pinging " + ip + ": " + e.getMessage());
                } finally {
                    int done = scanned.incrementAndGet();
                    // Print a progress tick every 10% of range
                    if (done % Math.max(1, total / 10) == 0) {
                        System.out.printf("    ... %d / %d addresses checked%n", done, total);
                    }
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.MINUTES);
        return live;
    }
}

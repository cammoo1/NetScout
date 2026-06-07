package com.netscout;

import java.util.*;



/**
 * NetScout — Lightweight Network Scanner
 *
 * Entry point. Parses CLI arguments, kicks off host discovery,
 * port scanning, and handles output (JSON + optional file logging).
 *
 * Usage:
 *   java -jar netscout.jar <CIDR> [options]
 *
 * Example:
 *   java -jar netscout.jar 192.168.1.0/24 --ports 22,80,443 --log --json output/results.json
 */
public class NetScout {

    static final String VERSION = "1.0.0";

    public static void main(String[] args) throws Exception {
        printBanner();

        if (args.length > 0 && (args[0].equals("--help") || args[0].equals("-h"))) {
            printHelp();
            return;
        }
 
        if (args.length == 0) {
            args = new String[]{ "192.168.40.0/24" };
        }   

        // --- Parse Arguments ---
        String cidr        = args[0];
        List<Integer> ports = Arrays.asList(21, 22, 23, 25, 53, 80, 110, 143, 443, 3306, 3389, 8080, 8443);
        String jsonOutput  = null;
        boolean fileLog    = false;
        int threads        = 50;
        int timeout        = 1000; // ms

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "--ports":
                    ports = parsePorts(args[++i]);
                    break;
                case "--json":
                    jsonOutput = args[++i];
                    break;
                case "--log":
                    fileLog = true;
                    break;
                case "--threads":
                    threads = Integer.parseInt(args[++i]);
                    break;
                case "--timeout":
                    timeout = Integer.parseInt(args[++i]);
                    break;
                default:
                    System.err.println("[WARN] Unknown argument: " + args[i]);
            }
        }

        Logger logger = new Logger(fileLog);
        logger.log("Starting NetScout v" + VERSION);
        logger.log("Target range : " + cidr);
        logger.log("Ports        : " + ports);
        logger.log("Threads      : " + threads);
        logger.log("Timeout      : " + timeout + "ms");
        System.out.println();

        // --- Expand CIDR to list of IPs ---
        List<String> hosts = CidrExpander.expand(cidr);
        logger.log("Scanning " + hosts.size() + " addresses...\n");

        // --- Discover live hosts ---
        HostDiscovery discovery = new HostDiscovery(threads, timeout, logger);
        List<ScanResult> liveHosts = discovery.discover(hosts);

        System.out.println("\n[INFO] Found " + liveHosts.size() + " live host(s). Starting port scan...\n");
        logger.log("Live hosts found: " + liveHosts.size());

        // --- Port scan each live host ---
        PortScanner scanner = new PortScanner(threads, timeout, logger);
        scanner.scan(liveHosts, ports);

        // --- Print summary table ---
        printResults(liveHosts);

        // --- JSON export ---
        if (jsonOutput != null) {
            JsonExporter.export(liveHosts, jsonOutput);
            System.out.println("\n[INFO] Results saved to: " + jsonOutput);
            logger.log("JSON exported to: " + jsonOutput);
        }

        logger.close();
        System.out.println("\n[INFO] Scan complete.\n");
    }

    // -------------------------------------------------------------------------

    static List<Integer> parsePorts(String raw) {
        List<Integer> list = new ArrayList<>();
        for (String p : raw.split(",")) {
            String trimmed = p.trim();
            if (trimmed.contains("-")) {
                String[] range = trimmed.split("-");
                int start = Integer.parseInt(range[0]);
                int end   = Integer.parseInt(range[1]);
                for (int i = start; i <= end; i++) list.add(i);
            } else {
                list.add(Integer.parseInt(trimmed));
            }
        }
        return list;
    }

    static void printResults(List<ScanResult> results) {
        System.out.println("=".repeat(70));
        System.out.printf("%-18s %-35s %-12s %s%n", "IP Address", "Hostname", "Latency", "Open Ports");
        System.out.println("=".repeat(70));
        for (ScanResult r : results) {
            String hostname = r.hostname != null ? r.hostname : "(unresolved)";
            String latency  = r.latencyMs >= 0 ? r.latencyMs + "ms" : "N/A";
            String ports    = r.openPorts.isEmpty() ? "none detected" : r.openPorts.toString();
            System.out.printf("%-18s %-35s %-12s %s%n", r.ip, hostname, latency, ports);
        }
        System.out.println("=".repeat(70));
    }

    static void printBanner() {
        System.out.println("""
        \033[36m
          _   _      _   ____                 _
         | \\ | | ___| |_/ ___|  ___ ___  _   _| |_
         |  \\| |/ _ \\ __\\___ \\ / __/ _ \\| | | | __|
         | |\\  |  __/ |_ ___) | (_| (_) | |_| | |_
         |_| \\_|\\___|\\__|____/ \\___\\___/ \\__,_|\\__|
        \033[0m
          Lightweight Network Scanner  v1.0.0
          github.com/cammoo1/netscout
        """);
    }

    static void printHelp() {
        System.out.println("""
        Usage:
          java -jar netscout.jar <CIDR> [Options]

        Arguments:
          <CIDR>              Target range, e.g. 192.168.1.0/24

        Options:
          --ports <list>      Comma-separated ports or ranges (e.g. 22,80,443 or 20-25)
                              Default: 21,22,23,25,53,80,110,143,443,3306,3389,8080,8443
          --json  <file>      Export results to a JSON file
          --log               Write timestamped logs to logs/ directory
          --threads <n>       Thread pool size (default: 50)
          --timeout <ms>      Connection timeout in ms (default: 1000)
          --help              Show this help message

        Examples:
          java -jar netscout.jar 192.168.1.0/24
          java -jar netscout.jar 10.0.0.0/24 --ports 22,80,443 --json output/scan.json --log
          java -jar netscout.jar 192.168.0.0/16 --threads 100 --timeout 500
        """);
    }
}

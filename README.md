> A lightweight, command-line network scanner written in Java using no external dependencies.

NetScout finds active hosts on a local network, checks for open TCP ports, resolves hostnames via reverse DNS, and logs everything to structured output. Built from scratch using only the Java standard library as a hands-on way to explore network programming, socket I/O, and concurrent execution.

---

## Features

| Feature / Description |
|---|---|
| **Host Discovery** | Pings all hosts in a CIDR range to find active devices |
| **Port Scanning** | Checks a configurable list of TCP ports on each live host |
| **Hostname Resolution** | Performs reverse DNS lookups to identify devices by name |
| **Response Time Tracking** | Records ping latency for each discovered host |
| **Concurrent Scanning** | Uses `ThreadPoolExecutor` to scan many hosts simultaneously |
| **JSON Export** | Saves results to a structured `.json` file with `--json` |
| **File Logging** | Writes timestamped logs to `logs/` with `--log` |
| **Cross-Platform** | Works on Linux, macOS, and Windows (JDK 17+) |

---

## Quick Start

### Prerequisites
- Java 17 or higher (`java --version`)

### Build

```bash
git clone https://github.com/cammoo1/netscout.git
cd netscout
chmod +x build.sh
./build.sh
```

This compiles all sources and produces `netscout.jar` in the project root.

### Run

```bash
# Scan a /24 range with defaults
java -jar netscout.jar 192.168.1.0/24

# Specify custom ports
java -jar netscout.jar 192.168.1.0/24 --ports 22,80,443,8080

# Export results to JSON and enable file logging
java -jar netscout.jar 10.0.0.0/24 --json output/results.json --log

# Tune performance (more threads, shorter timeout)
java -jar netscout.jar 192.168.1.0/24 --threads 100 --timeout 500
```

---

## Usage

```
Usage:
  java -jar netscout.jar <CIDR> [options]

Arguments:
  <CIDR>              Target network range  (e.g. 192.168.1.0/24)

Options:
  --ports <list>      Comma-separated ports or ranges  (e.g. 22,80,443 or 20-25)
                      Default: 21,22,23,25,53,80,110,143,443,3306,3389,8080,8443
  --json  <file>      Export results to a JSON file
  --log               Write timestamped logs to logs/ directory
  --threads <n>       Thread pool size  (default: 50)
  --timeout <ms>      Connection/ping timeout in ms  (default: 1000)
  --help              Shows help message
```

---

## Example Output

```
  _   _      _   ____                 _
 | \ | | ___| |_/ ___|  ___ ___  _   _| |_
 |  \| |/ _ \ __\___ \ / __/ _ \| | | | __|
 | |\  |  __/ |_ ___) | (_| (_) | |_| | |_
 |_| \_|\___|\__|____/ \___\___/ \__,_|\__|

  Lightweight Network Scanner  v1.0.0

[INFO] Scanning 254 addresses...

[+] 192.168.1.1   (router.local)              2ms
[+] 192.168.1.42  (desktop.local)             5ms
[+] 192.168.1.101 (raspberrypi.local)         8ms

[INFO] Found 3 live host(s). Starting port scan...

  [open]  192.168.1.1   : 80/HTTP
  [open]  192.168.1.1   : 443/HTTPS
  [open]  192.168.1.42  : 22/SSH
  [open]  192.168.1.101 : 22/SSH
  [open]  192.168.1.101 : 80/HTTP

======================================================================
IP Address         Hostname                             Latency      Open Ports
======================================================================
192.168.1.1        router.local                         2ms          [80, 443]
192.168.1.42       desktop.local                        5ms          [22]
192.168.1.101      raspberrypi.local                    8ms          [22, 80]
======================================================================
```

### JSON output (`--json output/results.json`)

```json
{
  "scan_time": "2025-07-15T14:30:00",
  "total_hosts": 3,
  "results": [
    {
      "ip": "192.168.1.1",
      "hostname": "router.local",
      "latency_ms": 2,
      "open_ports": [80, 443]
    },
    {
      "ip": "192.168.1.42",
      "hostname": "desktop.local",
      "latency_ms": 5,
      "open_ports": [22]
    },
    {
      "ip": "192.168.1.101",
      "hostname": "raspberrypi.local",
      "latency_ms": 8,
      "open_ports": [22, 80]
    }
  ]
}
```

---

## Project Structure

```
netscout/
├── src/
│   └── main/java/com/netscout/
│       ├── NetScout.java        # Entry point — arg parsing, output, banner
│       ├── CidrExpander.java    # CIDR → list of IPs
│       ├── HostDiscovery.java   # Concurrent ping + reverse DNS
│       ├── PortScanner.java     # Concurrent TCP port scanning
│       ├── JsonExporter.java    # Writes results to .json (stdlib only)
│       ├── Logger.java          # Timestamped file + console logger
│       └── ScanResult.java      # Data class for a single host's results
├── build.sh                     # Compile + package script
├── .gitignore
└── README.md
```

---

## How It Works

1. **CIDR Expansion** — `CidrExpander` converts the input range (e.g. `192.168.1.0/24`) into a flat list of host IPs by masking the network address and iterating over host bits.

2. **Host Discovery** — `HostDiscovery` submits one task per IP to a `ThreadPoolExecutor`. Each task calls `InetAddress.isReachable()` and records latency. Reachable hosts get a reverse DNS lookup via `getCanonicalHostName()`.

3. **Port Scanning** — `PortScanner` opens a `Socket` with `connect(InetSocketAddress, timeout)` on every port for every live host. A successful connection means the port is open; a timeout or refused connection is caught and silently skipped.

4. **Output** — Results are printed in a formatted table. Optionally, `JsonExporter` serializes everything to a `.json` file using manual string building (no external JSON library needed).

---

## Default Ports Scanned

| Port | Service |
|------|---------|
| 21 | FTP |
| 22 | SSH |
| 23 | Telnet |
| 25 | SMTP |
| 53 | DNS |
| 80 | HTTP |
| 110 | POP3 |
| 143 | IMAP |
| 443 | HTTPS |
| 3306 | MySQL |
| 3389 | RDP |
| 8080 | HTTP-Alt |
| 8443 | HTTPS-Alt |

---

## Notes & Limitations

- **Permissions** — On some systems, ICMP ping requires elevated privileges. If host discovery misses devices, try running with `sudo`.
- **Firewalls** — Hosts with firewalls may block pings or port probes and won't appear in results even if active.
- **Network scope** — Designed for local networks. Scanning ranges you don't own may be illegal and is not the intended use.
- **No external dependencies** — Intentionally uses only `java.net`, `java.util.concurrent`, and `java.io`.

---

## License

MIT — free to use, modify, and distribute.

---

*Built as a learning project to explore network fundamentals, socket programming, and concurrent I/O in Java.*

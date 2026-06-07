package com.netscout;

import java.util.ArrayList;
import java.util.List;

/**
 * Expands a CIDR notation string (e.g. "192.168.1.0/24")
 * into a flat list of host IP strings.
 *
 * Supports /8 through /32 ranges.
 * /32  — single host
 * /24  — 254 hosts  (skips .0 and .255)
 * /16  — ~65k hosts
 */
public class CidrExpander {

    public static List<String> expand(String cidr) {
        List<String> ips = new ArrayList<>();

        String[] parts   = cidr.split("/");
        String   baseIp  = parts[0];
        int      prefix  = (parts.length > 1) ? Integer.parseInt(parts[1]) : 32;

        // Convert base IP to a 32-bit integer
        long base = ipToLong(baseIp);

        // Number of host bits
        int hostBits = 32 - prefix;
        long count   = 1L << hostBits; 

        // Apply the network mask so we always start from the network address
        long mask    = ~((1L << hostBits) - 1) & 0xFFFFFFFFL;
        long network = base & mask;

        for (long i = 1; i < count - 1; i++) {          // skips network (0) and broadcast (last)
            ips.add(longToIp(network + i));
        }

        // /32 — just the single host
        if (prefix == 32) {
            ips.clear();
            ips.add(baseIp);
        }

        return ips;
    }

    // -------------------------------------------------------------------------

    private static long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        long result = 0;
        for (String octet : octets) {
            result = (result << 8) | (Long.parseLong(octet) & 0xFF);
        }
        return result;
    }

    private static String longToIp(long ip) {
        return ((ip >> 24) & 0xFF) + "."
             + ((ip >> 16) & 0xFF) + "."
             + ((ip >>  8) & 0xFF) + "."
             + ( ip        & 0xFF);
    }
}

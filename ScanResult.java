package com.netscout;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds all discovered data for a single host.
 */
public class ScanResult {

    public final String      ip;
    public String            hostname;
    public long              latencyMs = -1;
    public List<Integer>     openPorts = new ArrayList<>();

    public ScanResult(String ip) {
        this.ip = ip;
    }
}

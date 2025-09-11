package io.github.nety.integrationcheck.cluster;

import java.net.InetAddress;
import java.net.UnknownHostException;

public final class NodeId {

    private NodeId() {}

    public static String current() throws UnknownHostException {
        return System.getenv().getOrDefault("POD_NAME", InetAddress.getLocalHost().getHostName());
    }
}

package com.duprasville.limiters.comms;

import java.util.concurrent.ConcurrentHashMap;

public class Communicator {
    ConcurrentHashMap<Long, Node> nodeIdToNode = new ConcurrentHashMap<>();

    public void sendTo(Node src, Node dst, Message msg) {
        dst.deliver(src, dst, msg);
    }

    public Node getNodeById(long nodeId) {
        if (nodeId < 0) throw new IllegalArgumentException("nodeId must be positive");
        return nodeIdToNode.computeIfAbsent(nodeId, Node::new);
    }
}

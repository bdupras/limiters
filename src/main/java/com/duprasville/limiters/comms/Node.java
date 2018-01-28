package com.duprasville.limiters.comms;

public class Node {
    private final long nodeId;
    private MessageReceiver recv;

    Node(long nodeId) {
        this.nodeId = nodeId;
    }

    public long getNodeId() {
        return nodeId;
    }

    public void onReceive(MessageReceiver recv) {
        this.recv = recv;
    }

    public void deliver(Node src, Node dst, Message msg) {
        recv.apply(src, dst, msg);
    }

    @Override
    public String toString() {
        return "Node{" +
                "nodeId=" + nodeId +
                '}';
    }
}

package com.duprasville.limiters.comms;

public class TestCommNode implements CommNode {
    private final long nodeId;
    private MessageReceiver recv;

    TestCommNode(long nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public void onReceive(MessageReceiver recv) {
        this.recv = recv;
    }

    @Override
    public void deliver(CommNode src, CommNode dst, Message msg) {
        recv.apply(src, dst, msg);
    }

    @Override
    public String toString() {
        return "CommNode{" +
                "nodeId=" + nodeId +
                '}';
    }
}

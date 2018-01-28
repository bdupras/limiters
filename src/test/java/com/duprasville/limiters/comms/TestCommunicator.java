package com.duprasville.limiters.comms;

import java.util.concurrent.ConcurrentHashMap;

public class TestCommunicator implements Communicator {
    ConcurrentHashMap<Long, CommNode> nodeIdToNode = new ConcurrentHashMap<>();

    @Override
    public void sendTo(CommNode src, CommNode dst, Message msg) {
        dst.deliver(src, dst, msg);
    }

    @Override
    public CommNode getCommNodeById(long nodeId) {
        if (nodeId < 0) throw new IllegalArgumentException();
        return nodeIdToNode.computeIfAbsent(nodeId, TestCommNode::new);
    }

    public Message newMessage(Object payload) {
        return new TestMessage(payload);
    }
}

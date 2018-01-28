package com.duprasville.limiters.comms;

import java.util.Set;
import java.util.stream.Collectors;

public class CommsFactory {
    public static Message newMessage(Object payload) {
        return new Message(payload);
    }

    public static Node newNode(long nodeId) {
        return new Node(nodeId);
    }

    public static Set<Node> newNodeSet(Set<Long> nodeIdSet) {
        return nodeIdSet.stream().map(Node::new).collect(Collectors.toSet());
    }

    public static Communicator newCommunicator() {
        return new Communicator();
    }

}

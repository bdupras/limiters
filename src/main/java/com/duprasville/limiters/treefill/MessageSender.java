package com.duprasville.limiters.treefill;

import com.duprasville.limiters.treefill.domain.Message;

public interface MessageSender {
    void send(Message message);
    long anyAvailableNode(long[] nodes);
}

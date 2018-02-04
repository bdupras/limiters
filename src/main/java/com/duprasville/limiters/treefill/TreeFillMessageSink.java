package com.duprasville.limiters.treefill;

import com.duprasville.limiters.comms.Message;
import com.duprasville.limiters.comms.MessageSink;

public interface TreeFillMessageSink extends MessageSink {
    void receive(Inform inform);
    void receive(Detect detect);
    void receive(Full full);

    @Override
    default void receive(Message message) { // private in java 9
        if (!(message instanceof TreeFillMessage))
            throw new IllegalStateException("Treefill cannot handle messages of type: " + message.getClass().getName());
        ((TreeFillMessage) message).deliver(this);
    }
}

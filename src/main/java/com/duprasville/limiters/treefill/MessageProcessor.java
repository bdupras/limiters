package com.duprasville.limiters.treefill;

import com.duprasville.limiters.treefill.domain.Acquire;
import com.duprasville.limiters.treefill.domain.ChildFull;
import com.duprasville.limiters.treefill.domain.Detect;
import com.duprasville.limiters.treefill.domain.Inform;
import com.duprasville.limiters.treefill.domain.Message;
import com.duprasville.limiters.treefill.domain.RoundFull;

public interface MessageProcessor {
    void process(Acquire acquire);
    void process(Inform inform);
    void process(Detect detect);
    void process(ChildFull childFull);
    void process(RoundFull roundFull);

    default void process(Message message) {
        if (message instanceof Acquire) {
            process((Acquire) message);
        } else if (message instanceof Detect) {
            process((Detect) message);
        } else if (message instanceof ChildFull) {
            process((ChildFull) message);
        } else if (message instanceof RoundFull) {
            process((RoundFull) message);
        } else if (message instanceof Inform) {
            process((Inform) message);
        } else {
            throw new IllegalStateException("Unknown message type: " + message.getClass().getName());
        }
    }
}

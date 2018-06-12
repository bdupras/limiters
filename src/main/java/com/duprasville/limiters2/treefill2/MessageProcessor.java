package com.duprasville.limiters2.treefill2;

public interface MessageProcessor {
    void process(Acquire acquire);
    void process(Inform inform);
    void process(Detect detect);
    void process(Full full);
    void process(RoundFull roundFull);

    default void process(Message message) {
        if (message instanceof Acquire) {
            process((Acquire) message);
        } else if (message instanceof Detect) {
            process((Detect) message);
        } else if (message instanceof Full) {
            process((Full) message);
        } else if (message instanceof RoundFull) {
            process((RoundFull) message);
        } else if (message instanceof Inform) {
            process((Inform) message);
        } else {
            throw new IllegalStateException("Unknown message type: " + message.getClass().getName());
        }
    }
}

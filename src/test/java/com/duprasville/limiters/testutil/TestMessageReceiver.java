package com.duprasville.limiters.testutil;

import com.duprasville.limiters.treefill.*;

import java.util.function.Consumer;

public class TestMessageReceiver implements MessageReceiver, MessageProcessor {
    Consumer<Acquire> onAcquire = (msg) -> {};
    Consumer<Detect> onDetect = (msg) -> {};
    Consumer<Full> onFull = (msg) -> {};
    Consumer<RoundFull> onRoundFull = (msg) -> {};
    Consumer<Inform> onInform = (msg) -> {};

    public void onAcquire(Consumer<Acquire> onAcquire) {
        this.onAcquire = onAcquire;
    }

    public void onDetect(Consumer<Detect> onDetect) {
        this.onDetect = onDetect;
    }

    public void onFull(Consumer<Full> onFull) {
        this.onFull = onFull;
    }

    public void onRoundFull(Consumer<RoundFull> onRoundFull) {
        this.onRoundFull = onRoundFull;
    }

    public void onInform(Consumer<Inform> onInform) {
        this.onInform = onInform;
    }

    @Override
    public void receive(Message message) {
        process(message);
    }

    @Override
    public void process(Acquire acquire) {
        onAcquire.accept(acquire);
    }

    @Override
    public void process(Inform inform) {
        onInform.accept(inform);
    }

    @Override
    public void process(Detect detect) {
        onDetect.accept(detect);
    }

    @Override
    public void process(Full full) {
        onFull.accept(full);
    }

    @Override
    public void process(RoundFull roundFull) {
        onRoundFull.accept(roundFull);
    }
}

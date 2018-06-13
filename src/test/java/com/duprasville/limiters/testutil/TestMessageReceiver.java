package com.duprasville.limiters.testutil;

import com.duprasville.limiters.treefill.*;
import com.duprasville.limiters.treefill.domain.Acquire;
import com.duprasville.limiters.treefill.domain.Detect;
import com.duprasville.limiters.treefill.domain.ChildFull;
import com.duprasville.limiters.treefill.domain.Inform;
import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.treefill.domain.RoundFull;

import java.util.function.Consumer;

public class TestMessageReceiver implements MessageReceiver, MessageProcessor {
    Consumer<Acquire> onAcquire = (msg) -> {};
    Consumer<Detect> onDetect = (msg) -> {};
    Consumer<ChildFull> onFull = (msg) -> {};
    Consumer<RoundFull> onRoundFull = (msg) -> {};
    Consumer<Inform> onInform = (msg) -> {};

    public void onAcquire(Consumer<Acquire> onAcquire) {
        this.onAcquire = onAcquire;
    }

    public void onDetect(Consumer<Detect> onDetect) {
        this.onDetect = onDetect;
    }

    public void onFull(Consumer<ChildFull> onFull) {
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
    public void process(ChildFull childFull) {
        onFull.accept(childFull);
    }

    @Override
    public void process(RoundFull roundFull) {
        onRoundFull.accept(roundFull);
    }
}

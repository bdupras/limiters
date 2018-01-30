package com.duprasville.limiters.treefill;

import java.util.function.Consumer;

public class TestTreeFillMessageSink implements TreeFillMessageSink {
    Consumer<Inform> onInform = (msg) -> {};
    Consumer<Detect> onDetect = (msg) -> {};

    @Override
    public void receive(Inform inform) {
        onInform.accept(inform);
    }

    public void onInform(Consumer<Inform> onInform) {
        this.onInform = onInform;
    }

    @Override
    public void receive(Detect detect) {
        onDetect.accept(detect);
    }

    public void onDetect(Consumer<Detect> onDetect) {
        this.onDetect = onDetect;
    }

}

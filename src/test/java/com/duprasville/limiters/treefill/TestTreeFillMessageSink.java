package com.duprasville.limiters.treefill;

import java.util.function.Consumer;

public class TestTreeFillMessageSink implements TreeFillMessageSink {
    Consumer<Inform> onInform = (msg) -> {};
    Consumer<Detect> onDetect = (msg) -> {};
    Consumer<Full> onFull = (msg) -> {};
    Consumer<WindowFull> onWindowFull = (msg) -> {};

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

    @Override
    public void receive(Full full) {
        onFull.accept(full);
    }

    @Override
    public void receive(WindowFull windowFull) {
        onWindowFull.accept(windowFull);
    }

    public void onDetect(Consumer<Detect> onDetect) {
        this.onDetect = onDetect;
    }

    public void onFull(Consumer<Full> onFull) {
        this.onFull = onFull;
    }

    public void onWindowFull(Consumer<WindowFull> onWindowFull) {
        this.onWindowFull = onWindowFull;
    }

}

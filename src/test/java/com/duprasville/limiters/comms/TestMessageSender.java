package com.duprasville.limiters.comms;

public class TestMessageSender implements MessageSender {
    TestMessageOnSend onSend = null;
    TestMessageOnSendAny onSendAny = null;

    @Override
    public void send(long src, long dst, Object msg) {
        if (null != onSend) onSend.apply(src, dst, msg);
    }

    public void onSend(TestMessageOnSend onSend) {
        this.onSend = onSend;
    }

    @Override
    public void sendAny(long src, long[] dst, Object msg) {
        if (null != onSendAny) onSendAny.apply(src, dst, msg);
    }

    public void onSendAny(TestMessageOnSendAny onSendAny) {
        this.onSendAny = onSendAny;
    }
}

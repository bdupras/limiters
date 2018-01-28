package com.duprasville.limiters.comms;

import java.util.Deque;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;

public class TestMessageSender implements MessageSender {
    TestMessageOnSend onSend = null;
    TestMessageOnSendAny onSendAny = null;
    private MessageReceiver onReceive;
    public final Deque<Object> sent = new ConcurrentLinkedDeque<>();
    public final Deque<Object> received = new ConcurrentLinkedDeque<>();

    @Override
    public void send(long src, long dst, Object msg) {
        sent.add(msg);
        if (null != onSend) onSend.apply(src, dst, msg);
    }

    public void onSend(TestMessageOnSend onSend) {
        this.onSend = onSend;
    }

    @Override
    public void sendAny(long src, long[] dst, Object msg) {
        int i = new Random().nextInt(dst.length);
        send(src, dst[i], msg);
        if (null != onSendAny) onSendAny.apply(src, dst, msg);
    }

    public void onSendAny(TestMessageOnSendAny onSendAny) {
        this.onSendAny = onSendAny;
    }

    @Override
    public void receive(long src, long dst, Object msg) {
        received.add(msg);
        if (null != this.onReceive) onReceive.apply(src, dst, msg);
    }

    @Override
    public void onReceive(MessageReceiver receiver) {
        this.onReceive = receiver;
    }
}

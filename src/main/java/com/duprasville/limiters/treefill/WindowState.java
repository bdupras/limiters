package com.duprasville.limiters.treefill;

import java.util.concurrent.Executor;

import com.duprasville.limiters.api.Message;
import com.duprasville.limiters.api.MessageDeliverator;
import com.duprasville.limiters.treefill.domain.Acquire;
import com.duprasville.limiters.treefill.domain.ChildFull;
import com.duprasville.limiters.treefill.domain.Detect;
import com.duprasville.limiters.treefill.domain.Inform;
import com.duprasville.limiters.treefill.domain.RoundFull;
import com.duprasville.limiters.util.SerialExecutor;

public class WindowState implements MessageProcessor {
    private final MessageDeliverator messageDeliverator;
    private final long windowFrame;
    private final WindowConfig windowConfig;
    private final SerialExecutor messageExecutor;
    private final NodeConfig nodeConfig;

    public WindowState(
            WindowConfig windowConfig,
            Executor executor,
            MessageDeliverator messageDeliverator,
            long windowFrame
    ) {
        this.windowConfig = windowConfig;
        this.nodeConfig = windowConfig.nodeConfig;
        this.messageExecutor = new SerialExecutor(executor);
        this.messageDeliverator = messageDeliverator;
        this.windowFrame = windowFrame;
    }

//    public boolean tryAcquire(long permits) {
//        receive(new Acquire(nodeConfig.nodeId, nodeConfig.nodeId, windowFrame, 0L, permits));
//        sendInform(0L, "Woot!!");
//        return true;
//    }

    @Override
    public void process(Acquire acquire) {
        System.out.println(acquire.toString());
    }

    @Override
    public void process(Inform inform) {
        System.out.println(inform.toString());
    }

    @Override
    public void process(Detect detect) {
        System.out.println(detect.toString());
    }

    @Override
    public void process(ChildFull childFull) {
        System.out.println(childFull.toString());
    }

    @Override
    public void process(RoundFull roundFull) {
        System.out.println(roundFull.toString());
    }

//    @Override
    public void receive(Message message) {
        messageExecutor.execute(() -> this.process(message));
    }

    private void sendInform(long dst, String msg) {
        messageDeliverator.send(new Inform(nodeConfig.nodeId, dst, windowFrame, msg));
    }
}

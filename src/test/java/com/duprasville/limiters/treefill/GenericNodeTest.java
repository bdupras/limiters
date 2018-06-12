package com.duprasville.limiters.treefill;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class GenericNodeTest {
    private MessageDeliverator mockMessageProcessor;

    @BeforeEach
    void init() {
        mockMessageProcessor = new MockMessageDeliverator();
    }

    @Test
    void testAcquire() throws ExecutionException, InterruptedException {
        GenericNode node = new GenericNode(1, 1, 10, false, mockMessageProcessor);

        assert(node.acquire().get());
    }

    @Test
    void testReceive() {
    }


    class MockMessageDeliverator implements MessageDeliverator {
        List<Message> messagesSent = new ArrayList<>();

        @Override
        public void send(Message message) {
            messagesSent.add(message);
        }
    }
}

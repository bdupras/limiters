package com.duprasville.limiters.comms;

import org.junit.jupiter.api.Test;

import static com.duprasville.limiters.comms.CommsFactory.*;
import static com.duprasville.limiters.comms.CommsFactory.newMessage;
import static java.lang.String.format;

class CommsTest {
    @Test
    void comms() {
        Object payload = "Hello World";
        Message msg = newMessage(payload);
        Communicator io = newCommunicator();
        Node a = newNode(13L);
        a.onReceive((s, d, m) -> System.out.println(format("src=%s received msg=%s from dst=%s", s, m, d)));

        Node b = newNode(42L);
        b.onReceive((s, d, m) -> System.out.println(format("src=%s received msg=%s from dst=%s", s, m, d)));

        io.sendTo(a, b, newMessage("Hello 13!"));
        io.sendTo(b, a, newMessage("Hello 42!"));
    }
}
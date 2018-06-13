package com.duprasville.limiters.treefill;

import com.duprasville.limiters.treefill.domain.Message;

interface MessageDeliverator {
    void send(Message message);

}

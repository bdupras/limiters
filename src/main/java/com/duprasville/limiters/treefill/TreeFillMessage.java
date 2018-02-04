package com.duprasville.limiters.treefill;

import com.duprasville.limiters.comms.Message;

public interface TreeFillMessage extends Message {
    void deliver(TreeFillMessageSink messageSink);
}


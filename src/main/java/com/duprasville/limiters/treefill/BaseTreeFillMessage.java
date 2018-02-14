package com.duprasville.limiters.treefill;

import com.duprasville.limiters.comms.BaseMessage;

abstract class BaseTreeFillMessage extends BaseMessage implements TreeFillMessage {
    final long window;

    BaseTreeFillMessage(long src, long dst, long window) {
        super(src, dst);
        this.window = window;
    }
}

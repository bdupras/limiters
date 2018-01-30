package com.duprasville.limiters.treefill;

import com.duprasville.limiters.comms.BaseMessage;

abstract class BaseTreeFillMessage extends BaseMessage implements TreeFillMessage {
    BaseTreeFillMessage(long src, long dst) {
        super(src, dst);
    }
}

package com.duprasville.limiters.treefill;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * acquire() is used by clients to ask for one permit
 * receive(message) is used by us to enqueue a message for processing.
 */
interface Node {

    CompletableFuture<Boolean> acquire();

    CompletableFuture<Boolean> receive(Message message);
}

class GenericNode implements Node {
    private final long id;
    private final long parentId;
    private long leftChild = 0;
    private long rightChild = 0;
    private final int N;
    private final long W;
    private int round = 0;
    private int share = 0;
    private final boolean hasChildren;
    List<Boolean> childPermitsAllocated;
    private MessageDeliverator messageDeliverator;
    private long counter = 0;


    GenericNode(int id, int N, int W, boolean haschildren, MessageDeliverator m) {
        this.id = id;
        this.N = N;
        this.W = W;
        this.share = (W/N)/2;
        this.hasChildren = haschildren;
        // if we are the root, parentId is 0, which is fine since node ids begin at 1
        this.parentId = id >> 1;

        if (this.hasChildren) {
            this.childPermitsAllocated = new ArrayList<>(2);
            this.leftChild = id << 1;
            this.rightChild = this.leftChild + 1;
        }

        this.messageDeliverator = m;
    }

    @Override
    public CompletableFuture<Boolean> acquire() {
        counter++;

        if (counter <= W) {
            return CompletableFuture.completedFuture(true);
        } else {
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public CompletableFuture<Boolean> receive(Message message) {
        return null;
    }
}

package io.zksync.exception;

import io.zksync.transport.ZkSyncError;

public class ZkSyncException extends RuntimeException {
    private static final long serialVersionUID = 4907339762891790110L;

    public ZkSyncException(String message) {
        super(message);
    }

    public ZkSyncException(Throwable cause) {
        super(cause);
    }

    public ZkSyncException(String message, Throwable cause) {
        super(message, cause);
    }

    public ZkSyncException(ZkSyncError error) {
        super(error.getMessage() + " (" + error.getCode() + ")");
    }
}

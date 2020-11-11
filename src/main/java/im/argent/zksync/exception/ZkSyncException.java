package im.argent.zksync.exception;

import im.argent.zksync.transport.ZkSyncError;

public class ZkSyncException extends RuntimeException {

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

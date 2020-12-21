package io.zksync.exception;

import io.zksync.transport.ZkSyncError;

public class ZkSyncIncorrectCredentialsException extends ZkSyncException {
    private static final long serialVersionUID = 5338333199906830236L;

    public ZkSyncIncorrectCredentialsException(String message) {
        super(message);
    }

    public ZkSyncIncorrectCredentialsException(Throwable cause) {
        super(cause);
    }

    public ZkSyncIncorrectCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }

    public ZkSyncIncorrectCredentialsException(ZkSyncError error) {
        super(error.getMessage() + " (" + error.getCode() + ")");
    }

}

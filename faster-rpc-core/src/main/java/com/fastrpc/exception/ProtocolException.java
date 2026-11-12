package com.fastrpc.exception;

public class ProtocolException extends IllegalArgumentException {
    private final int requestId;

    public ProtocolException(int requestId, String message) {
        super(message);
        this.requestId = requestId;
    }

    public int getRequestId() {
        return requestId;
    }
}

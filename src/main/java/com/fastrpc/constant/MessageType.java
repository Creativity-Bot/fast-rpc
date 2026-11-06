package com.fastrpc.constant;

public enum MessageType {
    REQUEST((byte) 1),
    RESPONSE((byte) 2),
    PING((byte) 3),
    PONG((byte) 4);

    private final byte code;

    MessageType(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static MessageType from(byte code) {
        for (MessageType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}

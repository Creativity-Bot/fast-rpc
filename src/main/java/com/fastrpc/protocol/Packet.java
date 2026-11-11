package com.fastrpc.protocol;

import com.fastrpc.constant.Flag;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@Data
@ToString
public class Packet implements Serializable {
    private Frame frame;
    private int requestId;
    private byte type;
    private byte flags;

    public Packet(Frame frame, int requestId, byte type) {
        this(frame, requestId, type, Flag.NONE);
    }

    public Packet(Frame frame, int requestId, byte type, byte flags) {
        this.frame = frame;
        this.requestId = requestId;
        this.type = type;
        this.flags = flags;
    }
}

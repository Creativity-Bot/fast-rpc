package com.fastrpc.protocol;

import java.io.Serializable;

public interface Frame extends Serializable {
    String MAGIC_NUMBER = "frpc";
    Byte VERSION = 1;
}

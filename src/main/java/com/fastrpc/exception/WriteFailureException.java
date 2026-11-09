package com.fastrpc.exception;

import java.io.IOException;

public class WriteFailureException extends IOException {
    public WriteFailureException(Throwable cause) {
        super(cause);
    }
}

package com.fastrpc.codec;

public interface Codec<T> {
    byte[] encode(T target) throws Exception;
    T decode(byte[] target) throws Exception;
}

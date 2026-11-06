package com.fastrpc.codec.impl;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.fastrpc.codec.Codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class HessianCodec<T> implements Codec<T> {

    @Override
    public byte[] encode(T target) throws Exception{
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Hessian2Output hessian2Output = new Hessian2Output(outputStream);
        hessian2Output.writeObject(target);
        hessian2Output.flush();
        return outputStream.toByteArray();
    }

    @Override
    public T decode(byte[] target) throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(target);
        Hessian2Input hessian2Input = new Hessian2Input(inputStream);
        return (T) hessian2Input.readObject();
    }
}

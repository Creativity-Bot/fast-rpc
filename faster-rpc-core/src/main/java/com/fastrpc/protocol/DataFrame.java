package com.fastrpc.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DataFrame implements Frame {

    private Class<?> serviceInterface;
    private String methodName;
    private Class<?>[] parameterTypes;
    private Object[] args;
}

package com.aegisql.conveyor.persistence.converters;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.google.protobuf.MessageLite;
import java.lang.reflect.Method;

public class ProtobufToBytesConverter <O extends MessageLite> implements ObjectToByteArrayConverter<O> {

    private final Class<O> cls;
    private final Method parser;

    public ProtobufToBytesConverter(Class<O> cls) {
        this.cls = cls;
        try {
            this.parser = cls.getMethod("parseFrom",byte[].class);
        } catch (NoSuchMethodException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public byte[] toPersistence(O obj) {
        return obj.toByteArray();
    }

    @Override
    public O fromPersistence(byte[] bytes) {
        try {
            return (O) parser.invoke(null,bytes);
        } catch (Exception e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public String conversionHint() {
        return "Protobuf<"+cls.getCanonicalName()+">:byte[]";
    }
}

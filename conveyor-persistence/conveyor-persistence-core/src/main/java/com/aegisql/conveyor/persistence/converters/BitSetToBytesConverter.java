package com.aegisql.conveyor.persistence.converters;

import java.util.BitSet;

/**
 * The type Bit set to bytes converter.
 */
public class BitSetToBytesConverter implements ObjectToByteArrayConverter<BitSet> {
    @Override
    public byte[] toPersistence(BitSet bs) {
        return bs.toByteArray();
    }

    @Override
    public BitSet fromPersistence(byte[] bytes) {
        return BitSet.valueOf(bytes);
    }

    @Override
    public String conversionHint() {
        return "java.util.BitSet:byte[]";
    }
}

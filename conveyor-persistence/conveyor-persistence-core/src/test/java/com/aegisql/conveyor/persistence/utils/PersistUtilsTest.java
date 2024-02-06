package com.aegisql.conveyor.persistence.utils;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.zip.DataFormatException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PersistUtilsTest {

    @Test
    public void compressDecompressTest() throws IOException, DataFormatException {

        String test = "Test Test Test Test Test Test Test";
        byte[] compressed = PersistUtils.compress(test.getBytes());
        byte[] decompressed = PersistUtils.decompress(compressed);
        System.out.println("Test length: "+test.length());
        System.out.println("Compressed length: "+compressed.length);
        System.out.println("Compressed: "+new String(compressed));
        System.out.println("Decompressed length: "+decompressed.length);
        assertEquals(test,new String(decompressed));
        assertTrue(compressed.length < decompressed.length);

    }

}
package com.aegisql.conveyor.consumers.scrap;

import com.aegisql.conveyor.ScrapBin;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StreamScrapTest {

    ByteArrayOutputStream os = new ByteArrayOutputStream();

    @Test
    public void accept() throws IOException {
        StreamScrap<Integer> ss = new StreamScrap<>(os);
        assertNotNull(ss.getPrintStream());
        ss.close();
        assertThrows(RuntimeException.class,()->ss.accept(new ScrapBin<>(null,1,this,"TEST", null, ScrapBin.FailureType.GENERAL_FAILURE,null,null)));
    }
}
package com.aegisql.conveyor.consumers.scrap;

import com.aegisql.conveyor.ScrapBin;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;

public class StreamScrapTest {

    ByteArrayOutputStream os = new ByteArrayOutputStream();

    @Test(expected = RuntimeException.class)
    public void accept() throws IOException {
        StreamScrap<Integer> ss = new StreamScrap<>(os);
        assertNotNull(ss.getPrintStream());
        ss.close();
        ss.accept(new ScrapBin<>(1,this,"TEST", null, ScrapBin.FailureType.GENERAL_FAILURE,null,null));
    }
}
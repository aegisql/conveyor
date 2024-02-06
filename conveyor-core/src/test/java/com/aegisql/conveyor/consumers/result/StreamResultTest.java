package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.harness.Tester;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class StreamResultTest {

    @Test
    public void constructionTest() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        StreamResult<Integer,String> sr = new StreamResult<>(os);
        sr.accept(ResultConsumerTest.getProductBin(1,"test"));
        assertNotNull(sr.getPrintStream());
        sr.close();
    }

    @Test
    public void failTest() throws IOException {
        OutputStream os = new ByteArrayOutputStream();
        StreamResult<Integer,StreamResultTest> sr = new StreamResult<>(os);
        assertThrows(RuntimeException.class,()->sr.accept(ResultConsumerTest.getProductBin(1,this)));
    }

    @Test
    public void ofTest() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        StreamResult<Integer,String> sr = StreamResult.of(null,os);
        sr.accept(ResultConsumerTest.getProductBin(1,"test"));
        assertNotNull(sr.getPrintStream());
        sr.close();

        Tester.removeFile("res_out_test.dat");
        StreamResult<Integer,String> sr2 = StreamResult.of(null,new File("res_out_test.dat"));
    }

}
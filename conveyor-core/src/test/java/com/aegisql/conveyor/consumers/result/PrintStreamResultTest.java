package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.harness.Tester;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;

public class PrintStreamResultTest {

    @Test
    public void constructionTest() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStreamResult<String,Integer> sr = new PrintStreamResult<>(os);
        assertNotNull(sr.getPrintStream());
        sr.accept(ResultConsumerTest.getProductBin(1,"test"));
        sr.close();
    }

    @Test
    public void ofTest() throws IOException {
        Tester.removeFile("print_stream_out.txt");
        PrintStreamResult<Integer,String> sr =
                PrintStreamResult.of(null,new File("print_stream_out.txt"));
        sr.accept(ResultConsumerTest.getProductBin(1,"test"));
        sr.close();
    }

    @Test
    public void ofUpperTest() throws IOException {
        Tester.removeFile("print_stream_up.txt");
        PrintStreamResult<Integer,String> sr =
                PrintStreamResult.of(null,new File("print_stream_up.txt"),v->v.toString().toUpperCase());
        sr.accept(ResultConsumerTest.getProductBin(1,"test"));
        sr.close();
    }

    @Test
    public void ofStrTest() throws IOException {
        Tester.removeFile("print_stream_out_str.txt");
        PrintStreamResult<Integer,String> sr =
                PrintStreamResult.of(null,"print_stream_out_str.txt");
        sr.accept(ResultConsumerTest.getProductBin(1,"test"));
        sr.close();
    }

    @Test
    public void ofUpperStrTest() throws IOException {
        Tester.removeFile("print_stream_up_str.txt");
        PrintStreamResult<Integer,String> sr =
                PrintStreamResult.of(null,"print_stream_up_str.txt",v->v.toString().toUpperCase());
        sr.accept(ResultConsumerTest.getProductBin(1,"test"));
        sr.close();
    }

}
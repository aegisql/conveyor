package com.aegisql.conveyor.consumers.scrap;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.harness.Tester;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.Assert.assertNotNull;

public class PrintStreamScrapTest {

    @Test
    public void printStreamIntoBytesTest() throws IOException {
        OutputStream os = new ByteArrayOutputStream();
        PrintStreamScrap<Integer> pss = new PrintStreamScrap<>(os);
        assertNotNull(pss.getPrintStream());
        pss.accept((ScrapBin)ScrapConsumerTest.getScrapBin(1,"test"));
        pss.close();
        String res = new String(((ByteArrayOutputStream) os).toByteArray());
        System.out.println(pss+" "+res);
    }

    @Test
    public void ofMethodsTest() throws Exception {
        Tester.removeFile("scrap.txt");
        Conveyor<Integer,String,String> c = new AssemblingConveyor<>();
        PrintStreamScrap<Integer> pss = PrintStreamScrap.of(c,"scrap.txt");
        pss.accept((ScrapBin)ScrapConsumerTest.getScrapBin(1,"test"));
        pss.close();
        assertNotNull(pss);
    }

    @Test
    public void ofMethodsFileTest() throws Exception {
        Tester.removeFile("scrap3.txt");
        Conveyor<Integer,String,String> c = new AssemblingConveyor<>();
        PrintStreamScrap<Integer> pss = PrintStreamScrap.of(c,new File("scrap3.txt"));
        pss.accept((ScrapBin)ScrapConsumerTest.getScrapBin(1,"test"));
        pss.close();
        assertNotNull(pss);
    }

    @Test
    public void ofMethodsTest2() throws Exception {
        Tester.removeFile("scrap2.txt");
        Conveyor<Integer,String,String> c = new AssemblingConveyor<>();
        PrintStreamScrap<Integer> pss = PrintStreamScrap.of(c,"scrap2.txt",txt->txt.toString().toUpperCase());
        pss.accept((ScrapBin)ScrapConsumerTest.getScrapBin(1,"test"));
        pss.close();
        assertNotNull(pss);
    }

}
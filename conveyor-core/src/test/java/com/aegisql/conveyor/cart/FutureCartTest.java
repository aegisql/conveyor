package com.aegisql.conveyor.cart;

import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertNotNull;

public class FutureCartTest {

    @Test
    public void futureCartTest() {
        CompletableFuture<String> f1 = new CompletableFuture<>();
        CompletableFuture<String> f2 = new CompletableFuture<>();
        FutureCart<Integer,String,String> fc1 = new FutureCart<>(1,f1,1);
        FutureCart<Integer,String,String> fc2 = new FutureCart<>(1,f2,1,0,new HashMap<>(),0);
        assertNotNull(fc1.getValue());
        assertNotNull(fc1.get());
        assertNotNull(fc2.getValue(String.class));
        assertNotNull(fc1.copy());

        ScrapConsumer<Integer, Cart<Integer, CompletableFuture<String>, String>> sc1 = fc1.getScrapConsumer();
        ScrapBin<Integer,Cart<Integer,CompletableFuture<String>,String>> sb1
                = new ScrapBin<>(null,1,fc1,"test",new Exception("test"),
                ScrapBin.FailureType.CART_REJECTED,new HashMap<>(),null);
        ScrapBin<Integer,Cart<Integer,CompletableFuture<String>,String>> sb2
                = new ScrapBin<>(null,1,fc1,"test",null,
                ScrapBin.FailureType.CART_REJECTED,new HashMap<>(),null);
        sc1.accept(sb1);
        sc1.accept(sb2);
    }

}
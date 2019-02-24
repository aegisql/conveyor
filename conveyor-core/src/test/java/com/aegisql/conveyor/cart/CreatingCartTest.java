package com.aegisql.conveyor.cart;

import com.aegisql.conveyor.BuilderAndFutureSupplier;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import org.junit.Test;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertNotNull;

public class CreatingCartTest {

    @Test
    public void constructorTest() {
        CompletableFuture<String> f1 = new CompletableFuture<>();
        BuilderSupplier.BuilderFutureSupplier<String> bfs = new BuilderAndFutureSupplier<>(BuilderSupplier.of(()->"test"),f1);
        CreatingCart<Integer,String,String> cc1 = new CreatingCart<>(
                1, bfs
                ,0,0,new HashMap<>(),0);
        assertNotNull(cc1.copy());
        assertNotNull(cc1.get());
        ScrapConsumer<Integer, Cart<Integer, BuilderSupplier<String>, String>> scrapConsumer
                = cc1.getScrapConsumer();


        ScrapBin<Integer,Cart<Integer, BuilderSupplier<String>, String>> bin1 =
                new ScrapBin<>(1,cc1,"test",new Exception(), ScrapBin.FailureType.CART_REJECTED,new HashMap<>(),null);
        ScrapBin<Integer,Cart<Integer, BuilderSupplier<String>, String>> bin2 =
                new ScrapBin<>(1,cc1,"test",null, ScrapBin.FailureType.CART_REJECTED,new HashMap<>(),null);
        scrapConsumer.accept(bin1);
        scrapConsumer.accept(bin2);
    }

}
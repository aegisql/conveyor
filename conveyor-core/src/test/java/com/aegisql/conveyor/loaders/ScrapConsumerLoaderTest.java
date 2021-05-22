package com.aegisql.conveyor.loaders;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.cart.FutureCart;
import com.aegisql.conveyor.consumers.scrap.IgnoreScrap;
import com.aegisql.conveyor.consumers.scrap.LogScrap;
import com.aegisql.conveyor.consumers.scrap.ScrapMap;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ScrapConsumerLoaderTest {

    @Test
    public void scrapConsLoaderByNameTest() {
        AssemblingConveyor<Integer,String,String> ac = new AssemblingConveyor<>();
        ac.setName("test");
        ScrapConsumerLoader<Integer> scl = ScrapConsumerLoader.byConveyorName("test",new IgnoreScrap<>());
        assertNotNull(scl);
    }

    @Test
    public void creationTests() {
        ScrapMap<Integer> sm = new ScrapMap<>();
        ScrapConsumerLoader<Integer> scl = new ScrapConsumerLoader<>(sc->{
            System.out.println("place "+sc);
        },sm);
        scl.andThen(new LogScrap<>());
        scl.set();

        ScrapConsumerLoader<Integer> scl2 = new ScrapConsumerLoader<>(sc->{
            System.out.println("place "+sc);
        },null);
        scl2.andThen(new LogScrap<>());
        scl2.set();

    }
    @Test
    public void cartConsumerTest() {
        ScrapMap<Integer> sm = new ScrapMap<>();
        ScrapConsumerLoader<Integer> scl = new ScrapConsumerLoader<>(sc->{
            System.out.println("place "+sc);
        },sm);
        CompletableFuture<String> f = new CompletableFuture<>();
        FutureCart<Integer,String,String> fc = new FutureCart<>(1,f,0);
        ScrapBin sb = new ScrapBin(null,1,fc,"test",null, ScrapBin.FailureType.CART_REJECTED,null,null);
        scl.CART_CONSUMER.accept(sb);
    }

    @Test
    public void futureConsumerTest() {
        ScrapMap<Integer> sm = new ScrapMap<>();
        ScrapConsumerLoader<Integer> scl = new ScrapConsumerLoader<>(sc->{
            System.out.println("place "+sc);
        },sm);
        CompletableFuture<String> f = new CompletableFuture<>();
        FutureCart<Integer,String,String> fc = new FutureCart<>(1,f,0);
        ScrapBin sb = new ScrapBin(null,1,fc,"test",null, ScrapBin.FailureType.CART_REJECTED,null,null);
        scl.FUTURE_CONSUMER.accept(sb);
        ScrapBin sb1 = new ScrapBin(null,1,null,"test",null, ScrapBin.FailureType.CART_REJECTED,null,null);
        scl.FUTURE_CONSUMER.accept(sb1);
        ScrapBin sb2 = new ScrapBin(null,1,fc,"test",new Exception("test"), ScrapBin.FailureType.CART_REJECTED,null,null);
        scl.FUTURE_CONSUMER.accept(sb2);
        assertTrue(fc.getValue().isCompletedExceptionally());

    }

    @Test
    public void cartFutureConsumerTest() {
        ScrapMap<Integer> sm = new ScrapMap<>();
        ScrapConsumerLoader<Integer> scl = new ScrapConsumerLoader<>(sc->{
            System.out.println("place "+sc);
        },sm);
        CompletableFuture<String> f = new CompletableFuture<>();
        FutureCart<Integer,String,String> fc = new FutureCart<>(1,f,0);
        ScrapBin sb = new ScrapBin(null,1,null,"test",null, ScrapBin.FailureType.CART_REJECTED,null,null);
        ScrapBin sb1 = new ScrapBin(null,1,fc,"test",new Exception("test"), ScrapBin.FailureType.CART_REJECTED,null,null);
        scl.CART_FUTURE_CONSUMER.accept(sb);
        scl.CART_FUTURE_CONSUMER.accept(sb1);
        assertTrue(fc.getFuture().isCompletedExceptionally());

    }


}
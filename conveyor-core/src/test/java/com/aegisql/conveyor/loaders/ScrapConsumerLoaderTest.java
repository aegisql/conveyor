package com.aegisql.conveyor.loaders;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.consumers.scrap.IgnoreScrap;
import com.aegisql.conveyor.consumers.scrap.LogScrap;
import com.aegisql.conveyor.consumers.scrap.ScrapMap;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

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

    }

}
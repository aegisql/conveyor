package com.aegisql.conveyor.consumers.scrap;

import com.aegisql.conveyor.ScrapBin;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ScrapConsumerTest {

    public static ScrapBin<Integer,String> getScrapBin(int id, String scrap) {
        ScrapBin<Integer,String> bin = new ScrapBin<>(null,id,scrap,"test",new Exception("test error"),
                ScrapBin.FailureType.GENERAL_FAILURE,new HashMap<>(),null);
        return bin;
    }
    @Test
    public void scrapConsumerTest() {

        Set<String> res = new HashSet<>();

        ScrapConsumer<Integer,String> sc1 = bin->{
            System.out.println(bin);
            res.add(bin.scrap);
        };
        ScrapConsumer<Integer,String> sc2 = sc1.andThen(bin->{
            System.err.println(bin);
        });

        ScrapConsumer<Integer,String> filter = sc1.filter(bin->true);
        ScrapConsumer<Integer,String> filterKey = sc1.filterKey(key->true);
        ScrapConsumer<Integer,String> filterScrap = sc1.filterScrap(scrap->true);
        ScrapConsumer<Integer,String> filterScrapType = sc1.filterScrapType(scrap->true);
        ScrapConsumer<Integer,String> filterFailureType = sc1.filterFailureType(key->true);
        ScrapConsumer<Integer,String> filterError = sc1.filterError(err->true);
        ScrapConsumer<Integer,String> async = sc1.async();

        sc1.accept(getScrapBin(1,"s1"));
        sc2.accept(getScrapBin(2,"s2"));
        filter.accept(getScrapBin(3,"s3"));
        filterKey.accept(getScrapBin(4,"s4"));
        filterScrap.accept(getScrapBin(5,"s5"));
        filterFailureType.accept(getScrapBin(6,"s6"));
        filterError.accept(getScrapBin(7,"s7"));
        async.accept(getScrapBin(8,"s8"));
        filterScrapType.accept(getScrapBin(9,"s9"));

        assertTrue(res.size()>=8);

    }

    @Test
    public void  filterByPropertyTest() {

        AtomicBoolean res1 = new AtomicBoolean(false);

        ScrapConsumer<Integer,String> rc1 = bin->{
            res1.set(true);
        };
        rc1 = rc1.filterProperty("test",s->s.equals("TEST"));

        ScrapBin<Integer,String> bin1 = getScrapBin(1,"val");
        bin1.properties.put("test","BEST");
        rc1.accept(bin1);
        assertFalse(res1.get());
        bin1.properties.put("test","TEST");
        rc1.accept(bin1);
        assertTrue(res1.get());
    }

    @Test
    public void  filterByPropertyEqualsTest() {

        AtomicBoolean res1 = new AtomicBoolean(false);

        ScrapConsumer<Integer,String> rc1 = bin->{
            res1.set(true);
        };
        rc1 = rc1.propertyEquals("test","TEST");

        ScrapBin<Integer,String> bin1 = getScrapBin(1,"val");
        bin1.properties.put("test","BEST");
        rc1.accept(bin1);
        assertFalse(res1.get());
        bin1.properties.put("test","TEST");
        rc1.accept(bin1);
        assertTrue(res1.get());
    }

}
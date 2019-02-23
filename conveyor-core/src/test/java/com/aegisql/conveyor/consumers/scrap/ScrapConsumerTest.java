package com.aegisql.conveyor.consumers.scrap;

import com.aegisql.conveyor.ScrapBin;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class ScrapConsumerTest {

    public static ScrapBin<Integer,String> getScrapBin(int id, String scrap) {
        ScrapBin<Integer,String> bin = new ScrapBin<>(id,scrap,"test",new Exception("test error"),
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
}
package com.aegisql.conveyor.consumers.scrap;

import com.aegisql.conveyor.ScrapBin;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

public class LastScrapsTest {

    public ScrapBin<Integer,Object> getScrapBin(int id,String scrap) {
        return new ScrapBin<>(null,id,scrap,"test",null,
                ScrapBin.FailureType.GENERAL_FAILURE,new HashMap<>(),null);
    }

    @Test
    public void lastScrapsTest() {
        LastScraps<Integer> ls = new LastScraps<>(2);
        ls.accept(getScrapBin(1,"s1"));
        ls.accept(getScrapBin(2,"s2"));
        ls.accept(getScrapBin(3,"s3"));
        System.out.println(ls);
        List<?> last = ls.getLast();
        assertEquals(2,last.size());
        assertTrue(last.contains("s2"));
        assertTrue(last.contains("s3"));
        LastScraps ls2 = LastScraps.of(null,3);
        assertNotNull(ls2);
    }

}
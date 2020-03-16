package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.ProductBin;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SampleResultsTest {

    @Test
    public void sampleResultTest() {
        SampleResults<Integer,Integer> sr1 = new SampleResults<>(10,1);
        SampleResults<Integer,Integer> sr0 = new SampleResults<>(10,0);
        SampleResults<Integer,Integer> sr05 = new SampleResults<>(10,0.005);
        for(int i = 1;i<10000;i++) {
            ProductBin<Integer,Integer> b = new ProductBin<>(i,i,0,null,null,null);
            sr1.accept(b);
            sr0.accept(b);
            sr05.accept(b);
        }
        List<Integer> last1 = sr1.getLast();
        System.out.println(last1);
        assertTrue(last1.containsAll(Arrays.asList(9990,9991,9992,9993,9994,9995,9996,9997,9998,9999)));

        List<Integer> last0 = sr0.getLast();
        System.out.println(last0);
        assertEquals(0,last0.size());

        List<Integer> last05 = new ArrayList<>(sr05.getLast());
        Collections.sort(last05);
        System.out.println(last05);
        assertTrue(last05.get(9)-last05.get(0)>100);


    }

}
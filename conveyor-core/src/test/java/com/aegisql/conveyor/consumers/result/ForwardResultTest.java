package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.ProductBin;
import org.junit.Test;

import static org.junit.Assert.fail;

public class ForwardResultTest {

    @Test
    public void constructionTest() {

        ForwardResult fr = new ForwardResult(null);//for coverage

        AssemblingConveyor<Integer,String,String> ac1 = new AssemblingConveyor<>();
        AssemblingConveyor<Integer,String,String> ac2 = new AssemblingConveyor<>();

        ac1.setName("from");
        ac2.setName("to");

        ForwardResult.ForwardingConsumer<Integer,String> fc
        = new ForwardResult.ForwardingConsumer<>("LABEL",bin->(Integer)bin.key,"from","to",k->true);

        ProductBin pb = ResultConsumerTest.getProductBin(1,"TEST");
        pb.properties.put("FORWARDED","to");

        fc.accept(pb);

        ForwardResult.from(ac1).to("to").foreach(k->true).bind();
        try {
            ForwardResult.from(ac1).bind();
            fail("Must fail, underset");
        }catch (Exception e){

        }
    }

}
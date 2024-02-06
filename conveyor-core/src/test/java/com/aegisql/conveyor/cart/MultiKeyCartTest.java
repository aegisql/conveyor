package com.aegisql.conveyor.cart;

import com.aegisql.conveyor.harness.Tester;
import com.aegisql.conveyor.serial.SerializablePredicate;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class MultiKeyCartTest {

    @Test
    public void constructionTest() {
        MultiKeyCart<Integer,String,String> mc1 = new MultiKeyCart<>((SerializablePredicate)k->true,"test","label",
        0,0,LoadType.MULTI_KEY_PART,new HashMap<>(),0);
        MultiKeyCart<Integer,String,String> mc2 = new MultiKeyCart<>("test","label",
                0,0);
        MultiKeyCart<Integer,String,String> mc3 = new MultiKeyCart<>(k->true,"test","label",
                0,0,0);
        MultiKeyCart<Integer,String,String> mc4 = new MultiKeyCart<>(k->true,"test","label",
                0,0);
        assertNotNull(mc1.copy());
        assertNotNull(mc1.toShoppingCart(1));
        try {
            byte[] pickle = Tester.pickle(mc1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
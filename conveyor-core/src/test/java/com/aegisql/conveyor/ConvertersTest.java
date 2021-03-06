package com.aegisql.conveyor;

import com.aegisql.conveyor.consumers.result.LastResultReference;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilderEvents;
import com.aegisql.conveyor.user.UserBuilderSmart;
import org.junit.Test;

import java.math.BigInteger;
import java.util.function.Function;

import static com.aegisql.conveyor.user.UserBuilderEvents.*;
import static org.junit.Assert.assertNotNull;

public class ConvertersTest {

    @Test
    public void registerConvertersTest() {


        AssemblingConveyor<Integer, UserBuilderEvents, User> c = new AssemblingConveyor<>();
        c.setName("converting");
        c.setBuilderSupplier(UserBuilderSmart::new);
        c.setReadinessEvaluator(Conveyor.getTesterFor(c).accepted(SET_FIRST, SET_LAST, SET_YEAR));

        c.registerKeyConverter(String.class,id->Integer.parseInt(id));
        c.registerLabelConverter(String.class,l-> valueOf(l));

        c.registerValueConverter(byte[].class,b->new String(b));
        c.registerValueConverter("text/plain", val->val.toString());
        c.registerValueConverter(BigInteger.class, bi->bi.intValue());

        var consumer = LastResultReference.of(c);
        c.resultConsumer(consumer).set();

        var keyConverter = c.getKeyConverter(String.class);
        assertNotNull(keyConverter);

        var loader = c.part().id(keyConverter.apply("1"));

        var labelConverter = c.getLabelConverter(String.class);
        assertNotNull(labelConverter);

        var bytesConverter = c.getValueConverter(byte[].class);
        assertNotNull(bytesConverter);
        Function toStringConverter = c.getValueConverter("text/plain");
        assertNotNull(toStringConverter);
        var bigIntConverter = c.getValueConverter(BigInteger.class);
        assertNotNull(bigIntConverter);

        loader.label(labelConverter.apply("SET_FIRST")).value(bytesConverter.apply("JOHN".getBytes())).place();
        loader.label(labelConverter.apply("SET_LAST")).value(toStringConverter.apply(new StringBuilder("SILVER"))).place();
        loader.label(labelConverter.apply("SET_YEAR")).value(bigIntConverter.apply(new BigInteger("2000"))).place().join();

        assertNotNull(consumer.getCurrent());
        System.out.println(consumer);
    }


}

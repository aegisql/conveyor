package com.aegisql.conveyor.consumers.scrap;

import com.aegisql.conveyor.AssemblingConveyor;
import org.junit.Test;

import static org.junit.Assert.*;

public class IgnoreScrapTest {

    @Test
    public void accept() {

        IgnoreScrap is = new IgnoreScrap();

        IgnoreScrap is2 = IgnoreScrap.of(new AssemblingConveyor<>());

        ScrapConsumer is3 = is.andThen(is2);

        is3.accept(null);


    }
}
package com.aegisql.conveyor.consumers.result;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;

public class LogResultTest {

    Logger L = LoggerFactory.getLogger(LogResultTest.class);

    @Test
    public void logResConstructorsTest() {
        ResultConsumer lr = new LogResult();
        lr.accept(ResultConsumerTest.getProductBin(1,"test 1"));
        LogResult trace = new LogResult(L, LogResult.Level.TRACE);
        LogResult debug = new LogResult(L, LogResult.Level.DEBUG);
        LogResult warn = new LogResult(L, LogResult.Level.WARN);
        LogResult info = new LogResult(L, LogResult.Level.INFO);
        LogResult error = new LogResult(L, LogResult.Level.ERROR);
        LogResult stdout = new LogResult(L, LogResult.Level.STDOUT);
        LogResult stderr = new LogResult(L, LogResult.Level.STDERR);
        stderr.accept(ResultConsumerTest.getProductBin(2,"test 2"));
    }

    @Test
    public void logResTraceTest() {
        LogResult lr1 = LogResult.trace(null);
        LogResult lr2 = LogResult.trace(null,L);
        LogResult lr3 = LogResult.trace(null,LogResultTest.class);
        LogResult lr4 = LogResult.trace(null,LogResultTest.class.getName());
        assertNotNull(lr1);
        assertNotNull(lr2);
        assertNotNull(lr3);
        assertNotNull(lr4);
    }

    @Test
    public void logResDebugTest() {
        LogResult lr1 = LogResult.debug(null);
        LogResult lr2 = LogResult.debug(null,L);
        LogResult lr3 = LogResult.debug(null,LogResultTest.class);
        LogResult lr4 = LogResult.debug(null,LogResultTest.class.getName());
        assertNotNull(lr1);
        assertNotNull(lr2);
        assertNotNull(lr3);
        assertNotNull(lr4);
    }

    @Test
    public void logResWarnTest() {
        LogResult lr1 = LogResult.warn(null);
        LogResult lr2 = LogResult.warn(null,L);
        LogResult lr3 = LogResult.warn(null,LogResultTest.class);
        LogResult lr4 = LogResult.warn(null,LogResultTest.class.getName());
        assertNotNull(lr1);
        assertNotNull(lr2);
        assertNotNull(lr3);
        assertNotNull(lr4);
    }

    @Test
    public void logResInfoTest() {
        LogResult lr1 = LogResult.info(null);
        LogResult lr2 = LogResult.info(null,L);
        LogResult lr3 = LogResult.info(null,LogResultTest.class);
        LogResult lr4 = LogResult.info(null,LogResultTest.class.getName());
        assertNotNull(lr1);
        assertNotNull(lr2);
        assertNotNull(lr3);
        assertNotNull(lr4);
    }

    @Test
    public void logResErrTest() {
        LogResult lr1 = LogResult.error(null);
        LogResult lr2 = LogResult.error(null,L);
        LogResult lr3 = LogResult.error(null,LogResultTest.class);
        LogResult lr4 = LogResult.error(null,LogResultTest.class.getName());
        assertNotNull(lr1);
        assertNotNull(lr2);
        assertNotNull(lr3);
        assertNotNull(lr4);
    }

    @Test
    public void logResStdTest() {
        LogResult lr1 = LogResult.stdOut(null);
        LogResult lr2 = LogResult.stdErr(null);
        assertNotNull(lr1);
        assertNotNull(lr2);
    }

}
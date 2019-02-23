package com.aegisql.conveyor.consumers.scrap;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;

public class LogScrapTest {

    Logger LOG = LoggerFactory.getLogger(LogScrapTest.class);

    @Test
    public void logDefaultTest() {
        LogScrap ls = new LogScrap();
        ls.accept(ScrapConsumerTest.getScrapBin(1,"test"));
    }

    @Test
    public void logLeveledTest() {
        LogScrap lsTrace = new LogScrap(LOG, LogScrap.Level.TRACE);
        LogScrap lsDebug = new LogScrap(LOG, LogScrap.Level.DEBUG);
        LogScrap lsWarn = new LogScrap(LOG, LogScrap.Level.WARN);
        LogScrap lsInfo = new LogScrap(LOG, LogScrap.Level.INFO);
        LogScrap lsError = new LogScrap(LOG, LogScrap.Level.ERROR);
        LogScrap lsStderr = new LogScrap(LOG, LogScrap.Level.STDERR);
        LogScrap lsStdout = new LogScrap(LOG, LogScrap.Level.STDOUT);
        assertNotNull(lsTrace);
        assertNotNull(lsDebug);
        assertNotNull(lsWarn);
        assertNotNull(lsInfo);
        assertNotNull(lsError);
        assertNotNull(lsStderr);
        assertNotNull(lsStdout);
        lsStdout.accept(ScrapConsumerTest.getScrapBin(1,"test out"));
        lsStderr.accept(ScrapConsumerTest.getScrapBin(2,"test err"));
    }

    @Test
    public void logLeveledLoggerTest() {
        LogScrap lsTrace = LogScrap.trace(null,LOG);
        LogScrap lsDebug = LogScrap.debug(null, LOG);
        LogScrap lsWarn = LogScrap.warn(null, LOG);
        LogScrap lsInfo = LogScrap.info(null,LOG);
        LogScrap lsError = LogScrap.error(null,LOG);
        LogScrap lsStderr = LogScrap.stdErr(null);
        LogScrap lsStdout = LogScrap.stdOut(null);

        assertNotNull(lsTrace);
        assertNotNull(lsDebug);
        assertNotNull(lsWarn);
        assertNotNull(lsInfo);
        assertNotNull(lsError);
        assertNotNull(lsStderr);
        assertNotNull(lsStdout);
    }

    @Test
    public void logLeveledClassTest() {
        LogScrap lsTrace = LogScrap.trace(null,LogScrapTest.class);
        LogScrap lsDebug = LogScrap.debug(null, LogScrapTest.class);
        LogScrap lsWarn = LogScrap.warn(null, LogScrapTest.class);
        LogScrap lsInfo = LogScrap.info(null,LogScrapTest.class);
        LogScrap lsError = LogScrap.error(null,LogScrapTest.class);
        assertNotNull(lsTrace);
        assertNotNull(lsDebug);
        assertNotNull(lsWarn);
        assertNotNull(lsInfo);
        assertNotNull(lsError);
    }

    @Test
    public void logLeveledNameTest() {
        LogScrap lsTrace = LogScrap.trace(null,LogScrapTest.class.getName());
        LogScrap lsDebug = LogScrap.debug(null, LogScrapTest.class.getName());
        LogScrap lsWarn = LogScrap.warn(null, LogScrapTest.class.getName());
        LogScrap lsInfo = LogScrap.info(null,LogScrapTest.class.getName());
        LogScrap lsError = LogScrap.error(null,LogScrapTest.class.getName());
        assertNotNull(lsTrace);
        assertNotNull(lsDebug);
        assertNotNull(lsWarn);
        assertNotNull(lsInfo);
        assertNotNull(lsError);
    }

    @Test
    public void logLeveledDefest() {
        LogScrap lsTrace = LogScrap.trace(null);
        LogScrap lsDebug = LogScrap.debug(null);
        LogScrap lsWarn = LogScrap.warn(null);
        LogScrap lsInfo = LogScrap.info(null);
        LogScrap lsError = LogScrap.error(null);
        assertNotNull(lsTrace);
        assertNotNull(lsDebug);
        assertNotNull(lsWarn);
        assertNotNull(lsInfo);
        assertNotNull(lsError);
    }

}
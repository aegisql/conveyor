package com.aegisql.conveyor;

import com.aegisql.conveyor.exception.KeepRunningConveyorException;
import org.junit.jupiter.api.Test;

public class KeepRunningConveyorExceptionTest {

    @Test
    public void testConstructors() {
        KeepRunningConveyorException e1 = new KeepRunningConveyorException();
        KeepRunningConveyorException e2 = new KeepRunningConveyorException("test");
        KeepRunningConveyorException e3 = new KeepRunningConveyorException(e1);
        KeepRunningConveyorException e4 = new KeepRunningConveyorException("test",e1);
        KeepRunningConveyorException e5 = new KeepRunningConveyorException("test",e1,true,false);
    }

}
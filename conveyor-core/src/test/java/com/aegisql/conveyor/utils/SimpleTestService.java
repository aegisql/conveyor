package com.aegisql.conveyor.utils;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ConveyorInitiatingService;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilderEvents;

public class SimpleTestService implements ConveyorInitiatingService<Integer,UserBuilderEvents,User> {
    Conveyor<Integer,UserBuilderEvents, User> conveyor;
    @Override
    public Conveyor<Integer, UserBuilderEvents, User> getConveyor() {
        if(conveyor==null) {
            conveyor = new AssemblingConveyor<>();
            conveyor.setName("SimpleTestServiceConveyor");
        }
        return conveyor;
    }

}

package com.aegisql.conveyor.utils;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ConveyorInitiatingService;
import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.user.UserBuilder;
import com.aegisql.conveyor.user.UserBuilderEvents;

import java.util.Arrays;
import java.util.List;

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

    @Override
    public Class<Integer> getKeyType() {
        return Integer.class;
    }

    @Override
    public Class<UserBuilderEvents> getLabelType() {
        return UserBuilderEvents.class;
    }

    @Override
    public Class<User> getProductType() {
        return User.class;
    }

    @Override
    public List<Class<?>> getSupportedValueTypes(UserBuilderEvents label) {
        return null;
    }

    @Override
    public List<UserBuilderEvents> getLabels() {
        return Arrays.asList(UserBuilderEvents.values());
    }

    public BuilderSupplier<User> builderSupplierFactory() {
        return UserBuilder::new;
    }

}

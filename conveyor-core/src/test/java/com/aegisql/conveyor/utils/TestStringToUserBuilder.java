package com.aegisql.conveyor.utils;

import com.aegisql.conveyor.user.User;
import com.aegisql.conveyor.utils.scalar.ScalarConvertingBuilder;

import java.util.concurrent.TimeUnit;

public class TestStringToUserBuilder extends ScalarConvertingBuilder<String, User> {

    public TestStringToUserBuilder() {
        super();
    }

    public TestStringToUserBuilder(long ttl, TimeUnit timeUnit) {
        super(ttl, timeUnit);
    }

    public TestStringToUserBuilder(long expirationTime) {
        super(expirationTime);
    }

    @Override
    public User get() {
        String[] fields = scalar.split(",");
        return new User(fields[0], fields[1], Integer.parseInt(fields[2]));
    }
}

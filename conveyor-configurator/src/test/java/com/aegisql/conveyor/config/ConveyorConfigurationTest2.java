package com.aegisql.conveyor.config;

import com.aegisql.conveyor.config.harness.StringSupplier;
import com.aegisql.java_path.ClassRegistry;
import com.aegisql.java_path.JavaPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

public class ConveyorConfigurationTest2 {


    @Test
    @Disabled
    public void evalBuilderUsingPath() {
        ConveyorBuilder cb = new ConveyorBuilder();

        JavaPath jp = new JavaPath(ConveyorBuilder.class);

        jp.evalPath("registerPath(#,$0)",cb,"java.lang.Integer");

        Supplier<StringSupplier> s1 = (Supplier<StringSupplier>) getBuilderSupplier("StringSupplier new(s1)");
        Supplier<StringSupplier> s2 = (Supplier<StringSupplier>) getBuilderSupplier("StringSupplier new(s2)");

        StringSupplier ss1 = s1.get();
        StringSupplier ss2 = s2.get();

        String str1 = ss1.get();
        String str2 = ss2.get();

        StringSupplier ss3 = s1.get();
        StringSupplier ss4 = s2.get();

        String str3 = ss1.get();
        String str4 = ss2.get();

    }

    static JavaPath javaPath;
    static ClassRegistry classRegistry = new ClassRegistry();

    @BeforeAll
    public static void init() {
        classRegistry.registerClass(StringSupplier.class,StringSupplier.class.getSimpleName());
        javaPath = new JavaPath(ConveyorConfigurationTest2.class,classRegistry);
    }

    Supplier<?> getBuilderSupplier(String init) {
        return ()-> javaPath.initPath("("+init+").@");
    }


}
package com.aegisql.conveyor.config;

import com.aegisql.conveyor.config.harness.StringSupplier;
import com.aegisql.java_path.ClassRegistry;
import com.aegisql.java_path.JavaPath;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.function.Supplier;

public class ConveyorConfigurationTest2 {

            /*
        * 					.<String>when("javaPath", ConveyorBuilder::registerPath)
					.<String>when("supplier", ConveyorBuilder::supplier)
					.<String>when("defaultBuilderTimeout", ConveyorBuilder::defaultBuilderTimeout)
					.<String>when("idleHeartBeat", ConveyorBuilder::idleHeartBeat)
					.<String>when("rejectUnexpireableCartsOlderThan", ConveyorBuilder::rejectUnexpireableCartsOlderThan)
					.<String>when("expirationPostponeTime", ConveyorBuilder::expirationPostponeTime)
					.<String>when("staticPart", ConveyorBuilder::staticPart)
					.<String>when("firstResultConsumer", ConveyorBuilder::firstResultConsumer)
					.<String>when("nextResultConsumer", ConveyorBuilder::nextResultConsumer)
					.<String>when("firstScrapConsumer", ConveyorBuilder::firstScrapConsumer)
					.<String>when("nextScrapConsumer", ConveyorBuilder::nextScrapConsumer)
					.<String>when("onTimeoutAction", ConveyorBuilder::timeoutAction)
					.<String>when("defaultCartConsumer", ConveyorBuilder::defaultCartConsumer)
					.<String>when("readinessEvaluator", ConveyorBuilder::readinessEvaluator)
					.<String>when("builderSupplier", ConveyorBuilder::builderSupplier)
					.<String>when("addBeforeKeyEvictionAction", ConveyorBuilder::addBeforeKeyEvictionAction)
					.<String>when("addCartBeforePlacementValidator", ConveyorBuilder::addCartBeforePlacementValidator)
					.<String>when("addBeforeKeyReschedulingAction", ConveyorBuilder::addBeforeKeyReschedulingAction)
					.<String>when("acceptLabels", ConveyorBuilder::acceptLabels)
					.<String>when("enablePostponeExpiration", ConveyorBuilder::enablePostponeExpiration)
					.<String>when("enablePostponeExpirationOnTimeout",ConveyorBuilder::enablePostponeExpirationOnTimeout)
					.<String>when("autoAcknowledge", ConveyorBuilder::autoAcknowledge)
					.<String>when("acknowledgeAction", ConveyorBuilder::acknowledgeAction)
					.<String>when("autoAcknowledgeOnStatus", ConveyorBuilder::autoAcknowledgeOnStatus)
					.<String>when("cartPayloadAccessor", ConveyorBuilder::cartPayloadAccessor)
					.<String>when("forward", ConveyorBuilder::forward)
					.<String>when("completed", ConveyorBuilder::completed)
					.<String>when("dependency", ConveyorBuilder::dependency)
					.<String>when("parallel", ConveyorBuilder::parallel)
					.<String>when("maxQueueSize", ConveyorBuilder::maxQueueSize)
					.<String>when("priority", ConveyorBuilder::priority)
					.<String>when("persistence", ConveyorBuilder::persitence)
					.<String>when("readyWhenAccepted", ConveyorBuilder::readyWhen)
					.<PersistenceProperty>when("persistenceProperty", ConveyorBuilder::persistenceProperty)
					.when("complete_configuration", ConveyorBuilder::allFilesReadSuccessfully));
        * */


    @Test
    @Ignore
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

    @BeforeClass
    public static void init() {
        classRegistry.registerClass(StringSupplier.class,StringSupplier.class.getSimpleName());
        javaPath = new JavaPath(ConveyorConfigurationTest2.class,classRegistry);
    }

    Supplier<?> getBuilderSupplier(String init) {
        return ()-> javaPath.initPath("("+init+").@");
    }


}
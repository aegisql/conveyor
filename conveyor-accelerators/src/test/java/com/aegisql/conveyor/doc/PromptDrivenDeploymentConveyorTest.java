package com.aegisql.conveyor.doc;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ScrapBin;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.consumers.result.LastResultReference;
import com.aegisql.conveyor.consumers.scrap.LastScrapReference;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptDrivenDeploymentConveyorTest {

    /*
     * This test is an example of using doc/conveyor-authoring-prompt.md as an implementation guide.
     *
     * Specification:
     * - Target module: conveyor-accelerators tests
     * - Product/output class: DeploymentPlan
     * - Key type: String
     * - Expected parts:
     *   - SERVICE: String, required
     *   - VERSION: String, required
     *   - REGION: String, required
     *   - OWNER: String, required
     *   - TAG: String, repeatable
     *   - CHANNEL: String, optional static default
     *   - FINALIZE: explicit completion signal
     * - Completion criteria:
     *   - SERVICE, VERSION, REGION, OWNER, and FINALIZE must be accepted
     * - Timeout/default behavior:
     *   - incomplete builds expire and go to scrap
     * - Result handling:
     *   - capture built plan in a result consumer
     * - Scrap handling:
     *   - capture incomplete build timeout in a scrap consumer
     */

    @Test
    void shouldBuildDeploymentPlanFromPromptStyleSpecification() {
        LastResultReference<String, DeploymentPlan> resultRef = new LastResultReference<>();
        LastScrapReference<String> scrapRef = new LastScrapReference<>();

        AssemblingConveyor<String, DeploymentLabel, DeploymentPlan> conveyor = newDeploymentConveyor(resultRef, scrapRef);
        try {
            conveyor.staticPart().label(DeploymentLabel.CHANNEL).value("stable").place().join();

            CompletableFuture<DeploymentPlan> future = conveyor.build().id("deploy-101").createFuture();

            conveyor.part().id("deploy-101").label(DeploymentLabel.SERVICE).value("billing-api").place().join();
            conveyor.part().id("deploy-101").label(DeploymentLabel.VERSION).value("1.8.0").place().join();
            conveyor.part().id("deploy-101").label(DeploymentLabel.REGION).value("us-east-1").place().join();
            conveyor.part().id("deploy-101").label(DeploymentLabel.OWNER).value("payments-team").place().join();
            conveyor.part().id("deploy-101").label(DeploymentLabel.TAG).value("customer-facing").place().join();
            conveyor.part().id("deploy-101").label(DeploymentLabel.TAG).value("blue-green").place().join();
            conveyor.part().id("deploy-101").label(DeploymentLabel.FINALIZE).place().join();

            DeploymentPlan plan = future.join();
            assertNotNull(plan);
            assertEquals("billing-api", plan.service());
            assertEquals("1.8.0", plan.version());
            assertEquals("us-east-1", plan.region());
            assertEquals("payments-team", plan.owner());
            assertEquals("stable", plan.channel());
            assertEquals(List.of("customer-facing", "blue-green"), plan.tags());
            assertEquals(plan, resultRef.getCurrent());
            assertNull(scrapRef.getCurrent());
        } finally {
            conveyor.completeAndStop().join();
        }
    }

    @Test
    void shouldScrapIncompleteDeploymentPlanWhenPromptDefinedCompletionIsNotReached() throws InterruptedException {
        LastResultReference<String, DeploymentPlan> resultRef = new LastResultReference<>();
        LastScrapReference<String> scrapRef = new LastScrapReference<>();

        AssemblingConveyor<String, DeploymentLabel, DeploymentPlan> conveyor =
                newDeploymentConveyor(resultRef, scrapRef, Duration.ofMillis(100));
        try {
            conveyor.staticPart().label(DeploymentLabel.CHANNEL).value("stable").place().join();

            conveyor.part().id("deploy-102").label(DeploymentLabel.SERVICE).value("pricing-api").place().join();
            conveyor.part().id("deploy-102").label(DeploymentLabel.VERSION).value("2.0.0").place().join();
            conveyor.part().id("deploy-102").label(DeploymentLabel.REGION).value("eu-west-1").place().join();
            // OWNER and FINALIZE never arrive, so the build should expire.

            Thread.sleep(200);
            conveyor.completeAndStop().join();

            ScrapBin<String, Object> scrap = scrapRef.getCurrent();
            assertNotNull(scrap);
            assertEquals("deploy-102", scrap.key);
            assertEquals(ScrapBin.FailureType.BUILD_EXPIRED, scrap.failureType);
            assertNull(resultRef.getCurrent());
        } finally {
            conveyor.stop();
        }
    }

    private AssemblingConveyor<String, DeploymentLabel, DeploymentPlan> newDeploymentConveyor(
            LastResultReference<String, DeploymentPlan> resultRef,
            LastScrapReference<String> scrapRef
    ) {
        return newDeploymentConveyor(resultRef, scrapRef, Duration.ofSeconds(5));
    }

    private AssemblingConveyor<String, DeploymentLabel, DeploymentPlan> newDeploymentConveyor(
            LastResultReference<String, DeploymentPlan> resultRef,
            LastScrapReference<String> scrapRef,
            Duration timeout
    ) {
        AssemblingConveyor<String, DeploymentLabel, DeploymentPlan> conveyor = new AssemblingConveyor<>();
        conveyor.setName("promptDrivenDeployment");
        conveyor.setBuilderSupplier(DeploymentPlanBuilder::new);
        conveyor.setDefaultBuilderTimeout(timeout);
        conveyor.setIdleHeartBeat(Duration.ofMillis(25));
        conveyor.resultConsumer(resultRef).set();
        conveyor.scrapConsumer(scrapRef).set();
        conveyor.setReadinessEvaluator(
                Conveyor.getTesterFor(conveyor).accepted(
                        DeploymentLabel.SERVICE,
                        DeploymentLabel.VERSION,
                        DeploymentLabel.REGION,
                        DeploymentLabel.OWNER,
                        DeploymentLabel.FINALIZE
                )
        );
        return conveyor;
    }

    private record DeploymentPlan(
            String service,
            String version,
            String region,
            String owner,
            String channel,
            List<String> tags
    ) {
    }

    private static final class DeploymentPlanBuilder implements Supplier<DeploymentPlan> {
        private String service;
        private String version;
        private String region;
        private String owner;
        private String channel = "standard";
        private final List<String> tags = new ArrayList<>();

        void setService(String service) {
            this.service = service;
        }

        void setVersion(String version) {
            this.version = version;
        }

        void setRegion(String region) {
            this.region = region;
        }

        void setOwner(String owner) {
            this.owner = owner;
        }

        void setChannel(String channel) {
            this.channel = channel;
        }

        void addTag(String tag) {
            this.tags.add(tag);
        }

        @Override
        public DeploymentPlan get() {
            return new DeploymentPlan(service, version, region, owner, channel, List.copyOf(tags));
        }
    }

    private enum DeploymentLabel implements SmartLabel<DeploymentPlanBuilder> {
        SERVICE(DeploymentPlanBuilder::setService),
        VERSION(DeploymentPlanBuilder::setVersion),
        REGION(DeploymentPlanBuilder::setRegion),
        OWNER(DeploymentPlanBuilder::setOwner),
        CHANNEL(DeploymentPlanBuilder::setChannel),
        TAG(DeploymentPlanBuilder::addTag),
        FINALIZE((builder, value) -> {
        });

        private final BiConsumer<DeploymentPlanBuilder, Object> setter;

        <T> DeploymentLabel(BiConsumer<DeploymentPlanBuilder, T> setter) {
            this.setter = (builder, value) -> setter.accept(builder, (T) value);
        }

        @Override
        public BiConsumer<DeploymentPlanBuilder, Object> get() {
            return setter;
        }
    }
}

# Spring Users Guide

If you are building a Spring application, treat Conveyor as part of your application code, not as a separate configuration system.

A comfortable working model is:

- Spring owns infrastructure
- Spring creates the persistence bean
- Spring creates the named conveyor bean
- application services inject that conveyor and use it

In practice, that means:

- JDBC URL, credentials, and pool settings live in Spring's `DataSource` configuration
- Redis URI, credentials, SSL, and pool settings live in Spring's Redis/client configuration
- Conveyor-specific behavior lives in a Spring `@Configuration` class

## What Goes Where

A simple rule helps keep the setup readable.

Put these in Spring configuration:

- `DataSource`
- connection-pool settings
- Redis client settings
- credentials and secrets management
- profiles and environment overrides
- bean lifecycle and dependency wiring

Put these in Conveyor builder code:

- conveyor name
- builder supplier
- readiness rules
- timeouts
- persistence wrapping
- Conveyor-specific persistence options such as encryption or custom table names

## A Good Default Pattern

The most natural Spring integration looks like this:

1. configure JDBC in `application.yml`
2. bind only Conveyor-specific settings with `@ConfigurationProperties`
3. create a persistence bean from Spring's `DataSource`
4. create a named conveyor bean
5. inject that conveyor into application services

That keeps the boundaries clean. Spring handles infrastructure. Conveyor handles conveyor behavior.

## Example: Spring-Owned JDBC With A Named Conveyor

### 1. Keep JDBC in Spring config

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orders
    username: app_user
    password: change-this-password

app:
  conveyor:
    orders:
      name: ordersConveyor
      idle-heart-beat: 100ms
      default-builder-timeout: 30s
      persistence:
        auto-init: true
        encryption-secret: change-this-secret
```

The important part here is what is *not* duplicated.

You do not need to repeat in Conveyor config:

- JDBC URL
- host and port
- username and password
- pool settings
- database-selection details already handled by Spring

### 2. Bind only the Conveyor-specific settings

```java
package com.example.orders;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.conveyor.orders")
public class OrdersConveyorProperties {

    private String name = "ordersConveyor";
    private Duration idleHeartBeat = Duration.ofMillis(100);
    private Duration defaultBuilderTimeout = Duration.ofSeconds(30);
    private final Persistence persistence = new Persistence();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Duration getIdleHeartBeat() {
        return idleHeartBeat;
    }

    public void setIdleHeartBeat(Duration idleHeartBeat) {
        this.idleHeartBeat = idleHeartBeat;
    }

    public Duration getDefaultBuilderTimeout() {
        return defaultBuilderTimeout;
    }

    public void setDefaultBuilderTimeout(Duration defaultBuilderTimeout) {
        this.defaultBuilderTimeout = defaultBuilderTimeout;
    }

    public Persistence getPersistence() {
        return persistence;
    }

    public static class Persistence {
        private boolean autoInit = true;
        private String encryptionSecret;

        public boolean isAutoInit() {
            return autoInit;
        }

        public void setAutoInit(boolean autoInit) {
            this.autoInit = autoInit;
        }

        public String getEncryptionSecret() {
            return encryptionSecret;
        }

        public void setEncryptionSecret(String encryptionSecret) {
            this.encryptionSecret = encryptionSecret;
        }
    }
}
```

This class stays small on purpose. It carries only the settings that are really about the conveyor.

### 3. Build the persistence bean from Spring's `DataSource`

```java
package com.example.orders;

import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(OrdersConveyorProperties.class)
public class OrdersPersistenceConfiguration {

    @Bean
    public Persistence<Long> ordersPersistence(
            DataSource dataSource,
            OrdersConveyorProperties properties) {

        return JdbcPersistenceBuilder.presetInitializer("postgres", Long.class)
                .dbcpConnection(dataSource)
                .autoInit(properties.getPersistence().isAutoInit())
                .encryptionSecret(properties.getPersistence().getEncryptionSecret())
                .build();
    }
}
```

This is the central Spring integration point:

- Spring owns the `DataSource`
- Conveyor persistence uses that `DataSource`
- connection details remain in Spring where Spring users expect them to be

If you need Conveyor-specific storage naming, add only those overrides:

```java
return JdbcPersistenceBuilder.presetInitializer("postgres", Long.class)
        .dbcpConnection(dataSource)
        .partTable("ORDER_PART")
        .completedLogTable("ORDER_COMPLETED")
        .autoInit(properties.getPersistence().isAutoInit())
        .build();
```

### 4. Create the named conveyor bean

```java
package com.example.orders;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.persistence.core.Persistence;
import com.aegisql.conveyor.persistence.core.PersistentConveyor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrdersConveyorConfiguration {

    @Bean("ordersConveyor")
    public PersistentConveyor<Long, OrderLabel, OrderSummary> ordersConveyor(
            Persistence<Long> ordersPersistence,
            OrdersConveyorProperties properties) {

        AssemblingConveyor<Long, OrderLabel, OrderSummary> conveyor = new AssemblingConveyor<>();
        conveyor.setName(properties.getName());
        conveyor.setBuilderSupplier(OrderSummaryBuilder::new);
        conveyor.setIdleHeartBeat(properties.getIdleHeartBeat());
        conveyor.setDefaultBuilderTimeout(properties.getDefaultBuilderTimeout());
        conveyor.setReadinessEvaluator(Conveyor.getTesterFor(conveyor).accepted(
                OrderLabel.CUSTOMER_ID,
                OrderLabel.ORDER_TOTAL));

        return ordersPersistence.wrapConveyor(conveyor);
    }
}
```

A small but useful habit:

- keep the Spring bean name and `conveyor.setName(...)` aligned

That makes logs, diagnostics, `Conveyor.byName(...)`, and service integration easier to follow.

### 5. Inject the named conveyor

```java
package com.example.orders;

import com.aegisql.conveyor.Conveyor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class OrdersPlacementService {

    private final Conveyor<Long, OrderLabel, OrderSummary> ordersConveyor;

    public OrdersPlacementService(
            @Qualifier("ordersConveyor")
            Conveyor<Long, OrderLabel, OrderSummary> ordersConveyor) {
        this.ordersConveyor = ordersConveyor;
    }

    public void placeCustomerId(long orderId, long customerId) {
        ordersConveyor.part()
                .id(orderId)
                .label(OrderLabel.CUSTOMER_ID)
                .value(customerId)
                .place();
    }
}
```

At that point the conveyor behaves like any other Spring-managed application component.

## Logging In A Spring App

Spring applications usually want Conveyor logs to appear in the same logging configuration as the rest of the application.

A practical `application.yml` example:

```yaml
logging:
  level:
    root: INFO
    com.example.orders: DEBUG
    com.aegisql.conveyor: INFO
    com.aegisql.conveyor.persistence: INFO
```

If you want a `.properties` version:

```properties
logging.level.root=INFO
logging.level.com.example.orders=DEBUG
logging.level.com.aegisql.conveyor=INFO
logging.level.com.aegisql.conveyor.persistence=INFO
```

A common tuning pattern is:

- keep `com.aegisql.conveyor` at `INFO` in normal environments
- raise it to `DEBUG` temporarily when diagnosing placement, readiness, timeout, or recovery behavior
- keep your own application package at a level that matches how much placement detail you want to see

## Applying Spring Configuration To A Conveyor

The usual workflow is:

1. define infrastructure in Spring config
2. bind Conveyor-specific settings with `@ConfigurationProperties`
3. create persistence beans from Spring-managed infrastructure
4. build and name conveyors in `@Configuration`
5. inject conveyors with `@Qualifier`

This keeps responsibilities clear:

- Spring owns infrastructure and environment handling
- Conveyor code owns conveyor behavior
- application code sees a normal typed Spring bean

## Optional: External Conveyor Config Files

Spring applications can also call `ConveyorConfiguration.build(...)` and load conveyor semantics from a YAML or properties file.

That is possible, but for most Spring applications it is unnecessary because Spring already gives you:

- externalized configuration
- typed property binding
- profile-specific overrides
- environment-specific secrets and infrastructure wiring

If you still choose to use `conveyor-configurator` inside a Spring app, keep it focused on conveyor semantics only. JDBC and Redis infrastructure should still remain in Spring.

## Redis Follows The Same Rule

For Redis, the same style applies:

- let Spring create and own the Redis client bean
- inject that client into `RedisPersistenceBuilder`
- keep Redis URI, credentials, SSL, and pool tuning in Spring configuration
- keep Conveyor-specific behavior in your Spring `@Configuration` code

## Related Docs

- `doc/project-context.md`
- `conveyor-persistence/README.md`
- `conveyor-configurator/doc/project-context.md`
- `conveyor-service/README.md`

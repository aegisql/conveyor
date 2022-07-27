package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

public interface ConnectionDefaults {
    default String defaultHostname() {
        return "localhost";
    }

    default int defaultPort() {
        return -1;
    }

    default String defaultDriverClassName() {
        return "";
    }

    default ConnectionFactory<?> defaultConnectionFactory() {
        return ConnectionFactory.driverManagerFactoryInstance();
    }

    default ConnectionFactory<?> defaultConnectionPoolFactory() {
        return ConnectionFactory.DBCP2FactoryInstance();
    }
}

package com.aegisql.conveyor.persistence.redis;

import com.aegisql.conveyor.persistence.core.Persistence;

import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.util.function.Supplier;

public interface RedisPersistenceMBean<K> extends Supplier<Persistence<K>> {

    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    String getBackend();

    String getRedisUri();

    String getName();

    String getNamespace();

    String getArchiveStrategy();

    boolean isEncrypted();

    String getRestoreOrder();

    String getPriorityRestoreStrategy();

    int getMaxBatchSize();

    long getMaxBatchTime();

    Integer getMaxTotal();

    Integer getMaxIdle();

    Integer getMinIdle();

    Integer getConnectionTimeoutMillis();

    Integer getSocketTimeoutMillis();

    Integer getBlockingSocketTimeoutMillis();

    Integer getDatabase();

    String getClientName();

    String getUsername();

    boolean isPasswordConfigured();

    Boolean getSsl();

    int minCompactSize();

    boolean isAutoInit();

    boolean isExternalClient();

    int getAdditionalFieldCount();

    int getConverterCount();
}

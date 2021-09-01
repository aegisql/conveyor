package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import javax.sql.DataSource;
import java.sql.ShardingKey;
import java.util.function.Function;

public abstract class AbstractDataSourceConnectionFactory<T extends DataSource> extends AbstractConnectionFactory {

    protected T dataSource;
    protected final Function<AbstractDataSourceConnectionFactory<T>,T> dataSourceInitializer;
    protected boolean dataSourceInitialized = false;

    @Override
    public void resetConnection() {
        super.resetConnection();
        dataSource = null;
    }

    public T getDataSource() {
        return dataSource;
    }

    public ShardingKey getSuperShardingKey() {
        return superShardingKey;
    }

    public void setSuperShardingKey(ShardingKey superShardingKey) {
        this.superShardingKey = superShardingKey;
    }

    public ShardingKey getShardingKey() {
        return shardingKey;
    }

    public void setShardingKey(ShardingKey shardingKey) {
        this.shardingKey = shardingKey;
    }

    protected ShardingKey superShardingKey;
    protected ShardingKey shardingKey;

    public AbstractDataSourceConnectionFactory(Function<AbstractDataSourceConnectionFactory<T>,T> dataSourceInitializer) {
        this.dataSourceInitializer = dataSourceInitializer;
    }

    protected void initDataSource() {
        if(dataSource==null) {
            dataSource = dataSourceInitializer.apply(this);
        }
    }

}

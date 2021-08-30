package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.ShardingKey;

public abstract class AbstractDataSourceConnectionFactory<T extends DataSource> extends AbstractConnectionFactory {

    protected final T dataSource;
    protected boolean dataSourceInitialized = false;

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

    public AbstractDataSourceConnectionFactory(T dataSource) {
        this.dataSource = dataSource;
    }

    protected void initDataSource() {}

    @Override
    public Connection getConnection() {
        try {
            if(connection == null || connection.isClosed()) {
                ConnectionBuilder connectionBuilder = dataSource.createConnectionBuilder();
                if(notBlank(user)) {
                    connectionBuilder.user(user);
                }
                if(notBlank(password)) {
                    connectionBuilder.password(password);
                }
                if(superShardingKey != null) {
                    connectionBuilder.superShardingKey(superShardingKey);
                }
                if(shardingKey != null) {
                    connectionBuilder.shardingKey(shardingKey);
                }
                connection = connectionBuilder.build();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return connection;
    }

}

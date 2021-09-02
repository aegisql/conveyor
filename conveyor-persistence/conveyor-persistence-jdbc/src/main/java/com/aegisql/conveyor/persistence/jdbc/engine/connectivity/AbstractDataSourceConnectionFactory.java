package com.aegisql.conveyor.persistence.jdbc.engine.connectivity;

import javax.sql.DataSource;
import java.util.function.Function;

public abstract class AbstractDataSourceConnectionFactory<T extends DataSource> extends AbstractConnectionFactory {

    protected T dataSource;
    protected final Function<AbstractDataSourceConnectionFactory<T>,T> dataSourceInitializer;

    @Override
    public void resetConnection() {
        super.resetConnection();
        dataSource = null;
    }

    public AbstractDataSourceConnectionFactory(Function<AbstractDataSourceConnectionFactory<T>,T> dataSourceInitializer) {
        this.dataSourceInitializer = dataSourceInitializer;
    }

    protected void initDataSource() {
        if(dataSource==null) {
            dataSource = dataSourceInitializer.apply(this);
        }
    }

}

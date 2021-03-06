package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.ProductBin;

public class RunnableConsumer<T> implements ResultConsumer<T,Runnable>{
    @Override
    public void accept(ProductBin<T, Runnable> bin) {
        bin.product.run();
    }
}

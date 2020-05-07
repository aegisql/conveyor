package com.aegisql.conveyor.consumers.result;

import com.aegisql.conveyor.ProductBin;

public class RunnableConsumer implements ResultConsumer<Object,Runnable>{
    @Override
    public void accept(ProductBin<Object, Runnable> bin) {
        bin.product.run();
    }
}

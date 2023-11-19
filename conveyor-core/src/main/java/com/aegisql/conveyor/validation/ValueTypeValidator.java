package com.aegisql.conveyor.validation;

import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.exception.ConveyorRuntimeException;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class ValueTypeValidator<K,L extends SmartLabel> implements Consumer<Cart<K,?,L>> {
    @Override
    public void accept(Cart<K, ?, L> cart) {
        Object value = cart.getLabel().getPayload(cart);
        if(value != null) {
            List<Class<?>> acceptableTypes = cart.getLabel().getAcceptableTypes();
            if(acceptableTypes.size() == 0) {
                throw new ConveyorRuntimeException("ValueTypeValidator expected explicit list of supported types in label "+cart.getLabel());
            } else {
                final Class<?> valueClass = value.getClass();
                boolean foundMatch = false;
                for(int i = 0; i < acceptableTypes.size(); i++) {
                    Class<?> type = acceptableTypes.get(i);
                    Objects.requireNonNull(type,"ValueTypeValidator expected non-null type in label "+cart.getLabel());
                    if(type.isAssignableFrom(valueClass)) {
                        foundMatch = true;
                        break;
                    }
                }
                if( !foundMatch ) {
                    throw new ConveyorRuntimeException("Value type is unsupported for label "+cart.getLabel()+". Expected classes:"+acceptableTypes+". Value class:"+valueClass);
                }
            }
        }
    }

}

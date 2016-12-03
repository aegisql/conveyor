package com.aegisql.conveyor.utils.caching;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.LabeledValueConsumer;

public class MutableValueConsumer<T> implements LabeledValueConsumer<Object, Object, MutableReference<T>>{

	protected final static Logger LOG = LoggerFactory.getLogger(MutableValueConsumer.class);
	
	public MutableValueConsumer() {
		
	}
	
	@Override
	public void accept(Object label, Object value, MutableReference<T> builder) {
		if(label == null) {
			LOG.warn("Concuring create event for mutable cache {} {} {}",label,value,builder.get());
			builder.accept(((BuilderSupplier<T>)value).get().get());
		} else {
			LOG.debug("Update received for mutable cache {} {} {}",label,value,builder.get());
			builder.accept((T)value);
		}
	}

}

package com.aegisql.conveyor.utils.caching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.LabeledValueConsumer;

public class ImmutableValueConsumer<T> implements LabeledValueConsumer<Object, Object, ImmutableReference<T>>{

	protected final static Logger LOG = LoggerFactory.getLogger(ImmutableValueConsumer.class);
	
	public ImmutableValueConsumer() {
		
	}
	
	@Override
	public void accept(Object label, Object value, ImmutableReference<T> builder) {
		LOG.warn("Unexpected event for immutable cache {} {} {}",label,value,builder);
	}

}

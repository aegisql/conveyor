package com.aegisql.conveyor.utils.caching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.LabeledValueConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Class ImmutableValueConsumer.
 *
 * @param <T> the generic type
 */
public class ImmutableValueConsumer<T> implements LabeledValueConsumer<Object, Object, ImmutableReference<T>>{

	/** The Constant LOG. */
	protected final static Logger LOG = LoggerFactory.getLogger(ImmutableValueConsumer.class);
	
	/**
	 * Instantiates a new immutable value consumer.
	 */
	public ImmutableValueConsumer() {
		
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.LabeledValueConsumer#accept(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void accept(Object label, Object value, ImmutableReference<T> builder) {
		LOG.warn("Unexpected event for immutable cache {} {} {}",label,value,builder);
	}

}

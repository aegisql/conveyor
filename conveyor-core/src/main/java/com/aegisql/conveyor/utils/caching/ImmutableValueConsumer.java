package com.aegisql.conveyor.utils.caching;

import com.aegisql.conveyor.LabeledValueConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;

// TODO: Auto-generated Javadoc
/**
 * The Class ImmutableValueConsumer.
 *
 * @param <T> the generic type
 */
public class ImmutableValueConsumer<T> implements LabeledValueConsumer<Object, Object, ImmutableReference<T>>{

	@Serial
    private static final long serialVersionUID = 1L;
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

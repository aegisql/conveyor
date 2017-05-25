package com.aegisql.conveyor.utils.caching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.LabeledValueConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Class MutableValueConsumer.
 *
 * @param <T> the generic type
 */
public class MutableValueConsumer<T> implements LabeledValueConsumer<Object, Object, MutableReference<T>>{

	/** The Constant LOG. */
	protected final static Logger LOG = LoggerFactory.getLogger(MutableValueConsumer.class);
	
	/**
	 * Instantiates a new mutable value consumer.
	 */
	public MutableValueConsumer() {
		
	}
	
	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.LabeledValueConsumer#accept(java.lang.Object, java.lang.Object, java.lang.Object)
	 */
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

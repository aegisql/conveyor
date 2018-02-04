package com.aegisql.conveyor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.consumers.result.ResultConsumer;

// TODO: Auto-generated Javadoc
/**
 * The Class ConveyorNameSetter.
 */
@SuppressWarnings("rawtypes")
public class ConveyorNameSetter implements ResultConsumer<String, Conveyor> {

	/** The log. */
	Logger LOG = LoggerFactory.getLogger(ConveyorNameSetter.class);
	
	/** The conv. */
	private final Conveyor conv;
	
	/**
	 * Instantiates a new conveyor name setter.
	 *
	 * @param conv the conv
	 */
	public ConveyorNameSetter(Conveyor conv) {
		this.conv = conv;
	}
	
	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ProductBin<String, Conveyor> bin) {
		if(bin.product != null && bin.key != null) {
			bin.product.setName(bin.key);
			LOG.info("Complete setup for {}",bin.product);
		}
		conv.part().foreach().label("completed").value(bin.key).place();
	}

}

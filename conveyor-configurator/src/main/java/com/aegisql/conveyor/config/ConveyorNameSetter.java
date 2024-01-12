package com.aegisql.conveyor.config;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;

// TODO: Auto-generated Javadoc
/**
 * The Class ConveyorNameSetter.
 */
@SuppressWarnings("rawtypes")
public class ConveyorNameSetter implements ResultConsumer<String, Conveyor> {

	/** The log. */
	private final static Logger LOG = LoggerFactory.getLogger(ConveyorNameSetter.class);
	
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
	@Serial
    private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	public void accept(ProductBin<String, Conveyor> bin) {
		if(ConveyorConfiguration.DEFAULT_PERSISTENCE_NAME.equals(bin.key)) {
			if(bin.product != null) {
				bin.product.stop();
				try {
					Conveyor.unRegister(bin.product.getName());
				} catch (Exception e){

				}
			}
		} else if(bin.product != null && bin.key != null) {
			bin.product.setName(bin.key);
			LOG.info("Complete setup for {}",bin.product);
		}
		conv.part().foreach().label("completed").value(bin.key).place();
	}

}

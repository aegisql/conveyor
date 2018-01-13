package com.aegisql.conveyor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.consumers.result.ResultConsumer;

@SuppressWarnings("rawtypes")
public class ConveyorNameSetter implements ResultConsumer<String, Conveyor> {

	Logger LOG = LoggerFactory.getLogger(ConveyorNameSetter.class);
	
	private final Conveyor conv;
	
	public ConveyorNameSetter(Conveyor conv) {
		this.conv = conv;
	}
	
	private static final long serialVersionUID = 1L;

	@Override
	public void accept(ProductBin<String, Conveyor> bin) {
		if(bin.product != null && bin.key != null) {
			bin.product.setName(bin.key);
			conv.part().foreach().label("completed").value(bin.key).place();
			LOG.info("Complete setup for {}",bin.product);
		}
	}

}

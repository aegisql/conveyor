package com.aegisql.conveyor.persistence.core.harness;

import java.io.Serializable;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.aegisql.conveyor.SmartLabel;

public class SummBuilder implements Supplier<Long>,Serializable {

	public enum SummStep implements SmartLabel<SummBuilder> {
		ADD {
			@Override
			public BiConsumer<SummBuilder, Object> get() {
				return SummBuilder::value;
			}
		},
		DONE {
			@Override
			public BiConsumer<SummBuilder, Object> get() {
				return (a,b)->{};
			}
		}
		;
	}
	
	private static final long serialVersionUID = 1L;

	private long summ = 0;
	
	@Override
	public Long get() {
		return summ;
	}

	public static void value(SummBuilder b, Object val) {
		b.summ += (Long)val;
	}
	
}

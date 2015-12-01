package com.aegisql.conveyor.utils.scalar;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;

public class ScalarConvertingConveyor <K,IN,OUT> extends AssemblingConveyor<K, SmartLabel<ScalarConvertingBuilder<IN,?>>, ScalarCart<K,IN>, OUT> {

	public ScalarConvertingConveyor() {
		super();
		this.setName("ScalarConvertingConveyor");
	}

}

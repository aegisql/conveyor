package com.aegisql.conveyor.utils.scalar;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.SmartLabel;
import com.aegisql.conveyor.cart.Cart;
import com.aegisql.conveyor.cart.ShoppingCart;

public class ScalarConvertingConveyor <K,IN,OUT> extends AssemblingConveyor<K, SmartLabel<ScalarConvertingBuilder<IN,?>>, Cart<K, IN, SmartLabel<ScalarConvertingBuilder<IN, ?>>>, OUT> {

	public ScalarConvertingConveyor() {
		super();
		this.setName("ScalarConvertingConveyor");
	}

}

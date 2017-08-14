package com.aegisql.conveyor.cart;

public enum LoadType {
	PART,
	MULTI_KEY_PART,
	STATIC_PART,
	RESULT_CONSUMER, //15 char max
	FUTURE,
	BUILDER,
	COMMAND
}

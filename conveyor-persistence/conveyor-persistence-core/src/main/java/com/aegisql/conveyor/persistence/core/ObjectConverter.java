package com.aegisql.conveyor.persistence.core;

public interface ObjectConverter <O,P> {
	P toPersistence(O obj);
	O fromPersistence(P p);
}

package com.aegisql.conveyor;

import java.io.Serializable;

public interface Wrapped<T> extends Serializable {
    T unwrap();
}

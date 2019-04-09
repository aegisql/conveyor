package com.aegisql.conveyor.utils;

import java.io.Serializable;

public interface Wrapped<T> extends Serializable {
    T unwrap();
}

package com.aegisql.conveyor.config.harness;

public class TestBean {

    public int timeout = 1000;

    public String type = "value";

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TestBean{");
        sb.append("timeout=").append(timeout);
        sb.append(", type='").append(type).append('\'');
        sb.append('}');
        return sb.toString();
    }
}

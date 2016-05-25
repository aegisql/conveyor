package com.aegisql.conveyor;

public class LabeledValue<L> {
	public final L label;
	public final Object value;
	
	public LabeledValue(L label, Object value) {
		this.label = label;
		this.value = value;
	}

	@Override
	public String toString() {
		return "LabeledValue [label=" + label + ", value=" + value + "]";
	}
	
}

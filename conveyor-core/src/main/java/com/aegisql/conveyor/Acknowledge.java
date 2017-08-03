package com.aegisql.conveyor;

public interface Acknowledge {
	public void ack();
	public boolean isAcknowledged();
}

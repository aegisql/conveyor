package com.aegisql.conveyor;

public interface ParallelConveyorMBean {
	public String getName();
	public String getType();
	public int getInnerConveyorsCount();
	public boolean isRunning();

}

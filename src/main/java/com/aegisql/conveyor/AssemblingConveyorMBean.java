package com.aegisql.conveyor;

public interface AssemblingConveyorMBean {
	public String getName();
	public String getType();
	public long getThreadId();
	public int getInputQueueSize();
	public int getCollectorSize();
	public int getCommandQueueSize();
	public boolean isRunning();
	public boolean isLBalanced();
	public long getDefaultBuilderTimeoutMsec();
	public long getIdleHeartBeatMsec();
	public String getExpirationPosponeTimeMsec();
	public String getForwardingResultsTo();
	public String getAcceptedLabels();
	public long getCartCounter();
	public long getCommandCounter();
}

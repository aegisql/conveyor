package com.aegisql.conveyor;

public class ScrapBin<K,O> {
	
	public enum FailureType {
		 CART_REJECTED
		,COMMAND_REJECTED
		,DATA_REJECTED
		,BUILD_FAILED
		,BUILD_INITIALIZATION_FAILED
		,READY_FAILED
		,BUILD_EXPIRED
		,ON_TIMEOUT_FAILED
		,BEFORE_EVICTION_FAILED
		,RESULT_CONSUMER_FAILED
		,CONVEYOR_STOPPED
		,GENERAL_FAILURE
	}
	
	public final K key;
	public final String comment;
	public final O scrap;
	public final Throwable error;
	public final FailureType failureType;
	
	public ScrapBin(K key, O scrap, String comment, FailureType type) {
		this.key         = key;
		this.comment     = comment;
		this.scrap       = scrap;
		this.error       = null;
		this.failureType = type;
	}

	public ScrapBin(K key, O scrap, String comment, Throwable error, FailureType type) {
		this.key         = key;
		this.comment     = comment;
		this.scrap       = scrap;
		this.error       = error;
		this.failureType = type;
	}

	@Override
	public String toString() {
		return "ScrapBin ["+failureType+" key=" + key + ": " + comment + "; " + scrap + (error == null ? "": " error="+error.getMessage() ) +"]";
	}
	
}

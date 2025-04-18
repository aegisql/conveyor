/*
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor;

import java.util.Map;
import java.util.Optional;

// TODO: Auto-generated Javadoc
// TODO: Add full State from the BuildingSite, if available, to the scrap 
/**
 * The Class ScrapBin.
 *
 * @param <K> the key type
 * @param <O> the generic type
 */
public final class ScrapBin<K,O>  extends AbstractBin<K,Object,Object> {
	
	/**
	 * The Enum FailureType.
	 */
	public enum FailureType {
		 
/** The cart rejected. */
CART_REJECTED
		,
/** The command rejected. */
COMMAND_REJECTED
		,
/** The data rejected. */
DATA_REJECTED
		,
/** The build failed. */
BUILD_FAILED
		,
/** The build initialization failed. */
BUILD_INITIALIZATION_FAILED
		,
/** The ready failed. */
READY_FAILED
		,
/** The build expired. */
BUILD_EXPIRED
		,
/** The on timeout failed. */
ON_TIMEOUT_FAILED
		,
/** The before eviction failed. */
BEFORE_EVICTION_FAILED
		,
/** The result consumer failed. */
RESULT_CONSUMER_FAILED
		,
/** The conveyor stopped. */
CONVEYOR_STOPPED
		,
/** The general failure. */
GENERAL_FAILURE
		,

/** The keep running exception. */
KEEP_RUNNING_EXCEPTION
		,
/** External failure failure type. */
EXTERNAL_FAILURE,

		COLLECTOR_MAX_SIZE_REACHED
	}
	
	/** The comment. */
	public final String comment;
	
	/** The scrap. */
	public final O scrap;
	
	/** The error. */
	public final Throwable error;
	
	/** The failure type. */
	public final FailureType failureType;
	
	/** The properties. */
	public final Map<String,Object> properties;
	
	/** The acknowledge. */
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public final Optional<Acknowledge> acknowledge;

	/**
	 * Instantiates a new scrap bin.
	 *
	 * @param key the key
	 * @param scrap the scrap
	 * @param comment the comment
	 * @param error the error
	 * @param type the type
	 * @param properties the properties
	 * @param acknowledge the acknowledge
	 */
	public ScrapBin(Conveyor<K, Object, Object> conveyor, K key, O scrap, String comment, Throwable error, FailureType type, Map<String, Object> properties, Acknowledge acknowledge) {
		super(conveyor,key);
		this.comment     = comment;
		this.scrap       = scrap;
		this.error       = error;
		this.failureType = type;
		this.properties  = properties;
		this.acknowledge = Optional.ofNullable(acknowledge);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "ScrapBin ["+failureType+" conveyor="+(conveyor==null?"N/A":conveyor.getName())+" key=" + key + ": " + comment + "; " + scrap
				+ (error == null ? "": " error="+error.getMessage() )
				+ "; properties=" + properties
				+"]";
	}
	
}

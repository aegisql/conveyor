/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.user;

import com.aegisql.conveyor.SmartLabel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Class UserBuilderSmart.
 * 
 * @author Mikhail Teplitskiy
 * @version 1.0.0
 */
public class UserBuilderSmart implements Supplier<User> {

	public enum UserBuilderSmartLabel implements SmartLabel<UserBuilderSmart> {
		FIRST((b,v)->{UserBuilderSmart.setFirst(b, (String)v);},String.class)
		,LAST((b,v)->{UserBuilderSmart.setLast(b, (String)v);},String.class)
		,YOB((b,v)->{UserBuilderSmart.setYearOfBirth(b, (Integer)v);},Integer.class)
		;

		final BiConsumer<UserBuilderSmart, Object> consumer;
		final List<Class<?>> supportedTypes = new ArrayList<>();

		UserBuilderSmartLabel(BiConsumer<UserBuilderSmart, ?> consumer, Class<?> type) {
			this.consumer = (BiConsumer<UserBuilderSmart, Object>) consumer;
			this.supportedTypes.add(type);
		}

		@Override
		public BiConsumer<UserBuilderSmart, Object> get() {
			return consumer;
		}

		@Override
		public List<Class<?>> getAcceptableTypes() {
			return supportedTypes;
		}

	}

	/** The first. */
	String first;
	
	/** The last. */
	String last;
	
	/** The year of birth. */
	Integer yearOfBirth;

	/** The ready. */
	private boolean ready = false;

	/**
	 * Gets the year of birth.
	 *
	 * @return the year of birth
	 */
	public Integer getYearOfBirth() {
		return yearOfBirth;
	}

	/**
	 * Sets the year of birth.
	 *
	 * @param builder the builder
	 * @param yob the yob
	 */
	public static void setYearOfBirth(UserBuilderSmart builder, Integer yob) {
		builder.yearOfBirth = yob;
	}

	/**
	 * Gets the first.
	 *
	 * @return the first
	 */
	public String getFirst() {
		return first;
	}

	/**
	 * Sets the first.
	 *
	 * @param builder the builder
	 * @param first the first
	 */
	public static void setFirst(UserBuilderSmart builder, String first) {
		builder.first = first;
	}

	/**
	 * Gets the last.
	 *
	 * @return the last
	 */
	public String getLast() {
		return last;
	}

	/**
	 * Sets the last.
	 *
	 * @param builder the builder
	 * @param last the last
	 */
	public static void setLast(UserBuilderSmart builder, String last) {
		builder.last = last;
	}

	/* (non-Javadoc)
	 * @see com.aegisql.conveyor.Builder#build()
	 */
	@Override
	public User get() {
		return new User(first, last, yearOfBirth==null? 0:yearOfBirth.intValue());
	}

	/**
	 * Ready.
	 *
	 * @return true, if successful
	 */
	public boolean ready() {
		return ready;
	}

	/**
	 * Sets the ready.
	 *
	 * @param ready the new ready
	 */
	public void setReady(boolean ready) {
		this.ready = ready;
	}

}
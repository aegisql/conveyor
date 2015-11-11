package com.aegisql.conveyor.user;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import com.aegisql.conveyor.Builder;
import com.aegisql.conveyor.BuildingSite;
import com.aegisql.conveyor.Lot;

public class UserBuilderDelayed implements Builder<User>, Delayed {

	String first;
	String last;
	Integer yearOfBirth;

	private long builderCreated = System.currentTimeMillis();
	private long builderExpiration;

	
	private boolean ready = false;

	public UserBuilderDelayed(long delay) {
		builderExpiration = builderCreated + delay;
	}
	
	public Integer getYearOfBirth() {
		return yearOfBirth;
	}

	public void setYearOfBirth(Integer yob) {
		this.yearOfBirth = yob;
	}

	public String getFirst() {
		return first;
	}

	public void setFirst(String first) {
		this.first = first;
	}

	public String getLast() {
		return last;
	}

	public void setLast(String last) {
		this.last = last;
	}

	@Override
	public User build() {
		return new User(first, last, yearOfBirth==null? 0:yearOfBirth.intValue());
	}

	public boolean ready() {
		return ready;
	}

	public void setReady(boolean ready) {
		this.ready = ready;
	}

	public int compareTo(Delayed o) {
		return (int) (builderCreated - ((UserBuilderDelayed)o).builderCreated);
	}
	@Override
	public long getDelay(TimeUnit unit) {
        long delta;
		if( builderExpiration == 0 ) {
			delta = Long.MAX_VALUE;
		} else {
			delta = builderExpiration - System.currentTimeMillis();
		}
        return unit.convert(delta, TimeUnit.MILLISECONDS);
	}

	
}
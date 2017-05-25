/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.conveyor_smart_builder;

import java.util.Date;
import java.util.function.Supplier;

import com.aegisql.conveyor.Expireable;
import com.aegisql.conveyor.Testing;
import com.aegisql.conveyor.TimeoutAction;

public class PersonBuilder implements Supplier<Person>, Testing, Expireable, TimeoutAction {
	
	private String firstName;
	private String lastName;
	private Date dateOfBirth;
	private boolean forceReady = false;
	
	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public static void setFirstName(PersonBuilder builder, String firstName) {
		builder.firstName = firstName;
	}

	public static void setLastName(PersonBuilder builder, String lastName) {
		builder.lastName = lastName;
	}

	public static void setDateOfBirth(PersonBuilder builder, Date dateOfBirth) {
		builder.dateOfBirth = dateOfBirth;
	}

	@Override
	public Person get() {
		return new Person(firstName,lastName,dateOfBirth);
	}

	@Override
	public boolean test() {
		return (firstName!=null && lastName != null && dateOfBirth != null) || forceReady;
	}
	
	public void forceReady() {
		forceReady = true;
	}

	@Override
	public void onTimeout() {
		if((firstName != null) && (lastName != null)) {
			forceReady();
		}
	}

	@Override
	public long getExpirationTime() {
		return System.currentTimeMillis()+100;
	}
}
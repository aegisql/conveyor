/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.smart_conveyor_labels;

import java.util.Date;
import java.util.function.Supplier;

import com.aegisql.conveyor.Testing;

public class PersonBuilder implements Supplier<Person>, Testing {
	
	private String firstName;
	private String lastName;
	private Date dateOfBirth;
	
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
		return firstName!=null && lastName != null && dateOfBirth != null;
	}
}
/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.reflection;

import java.util.Date;
import java.util.function.Supplier;

public class PersonBuilder implements Supplier<Person> {
	
	public String firstName;
	public String lastName;
	public Date dateOfBirth;

	@Override
	public Person get() {
		return new Person(firstName,lastName,dateOfBirth);
	}	
}
package com.aegisql.conveyor.demo;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Demo2 {
	
	public static void main(String[] args) throws ParseException, InterruptedException {
		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		
		final PersonBuilder1 builder = new PersonBuilder1();
		
		Thread t1 = new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(10);
					builder.setFirstName("John");					
				} catch (InterruptedException e) {}
			}
		});

		Thread t2 = new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(100);
					builder.setLastName("Silver");					
				} catch (InterruptedException e) {}
			}
		});

		Thread t3 = new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(1000);
					builder.setDateOfBirth( format.parse("1695-11-10") );
				} catch (InterruptedException e) {} catch (ParseException e) {}
			}
		});

		t1.start();
		t2.start();
		t3.start();
		
		Thread.sleep(1100);
		
		Person person = builder.get();
		
		System.out.println( person );
		
		
		
	}

}

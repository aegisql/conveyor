package com.aegisql.conveyor;

import static org.junit.Assert.*;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AssemblingConveyorTest {

	Queue<User> outQueue = new ConcurrentLinkedQueue<>();
	
	public static class User {
		final String first;
		final String last;
		final int yearOfBirth;

		public User(String first, String last, int yob) {
			super();
			this.first = first;
			this.last = last;
			this.yearOfBirth = yob;
		}

		public String getFirst() {
			return first;
		}

		public String getLast() {
			return last;
		}
		
		public int getYearOfBirth() {
			return yearOfBirth;
		}

		@Override
		public String toString() {
			return "Name [first=" + first + ", last=" + last + ", born in " + yearOfBirth + "]";
		}

	}

	public static class UserBuilder implements Builder<User> {

		String first;
		String last;
		Integer yearOfBirth;

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
			return new User(first, last, yearOfBirth);
		}

		@Override
		public boolean ready() {
			return first != null && last != null && yearOfBirth != null;
		}

	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws InterruptedException {
		AssemblingConveyor<Integer, String, Cart<Integer, ?, String>, User> c 
		= new AssemblingConveyor<>(
				    UserBuilder::new,
				    (cart, b) -> {
					UserBuilder nb = (UserBuilder) b;
					try {
					} catch (Exception e) {

					}
					switch (cart.getLabel()) {
					case "setFirst":
						nb.setFirst((String) cart.getValue());
						break;
					case "setLast":
						nb.setLast((String) cart.getValue());
						break;
					case "setYob":
						nb.setYearOfBirth((Integer) cart.getValue());
						break;
					default:
						throw new RuntimeException("Unknown cart " + cart);
					}
				},
				    res->{
				    	outQueue.add(res);
				    });

		Cart<Integer, String, String> c1 = new Cart<>(1, "John", "setFirst");
		Cart<Integer, String, String> c2 = new Cart<>(1, "Doe", "setLast");
		Cart<Integer, String, String> c3 = new Cart<>(2, "Mike", "setFirst");
		Cart<Integer, Integer, String> c4 = new Cart<>(1, 1999, "setYob");

		c.offer(c1);
		System.out.println(outQueue.poll());
		c.offer(c2);
		c.offer(c3);
		c.offer(c4);
		Thread.sleep(100);

		System.out.println(outQueue.poll());
		System.out.println(outQueue.poll());

		c.stop();
	}

}

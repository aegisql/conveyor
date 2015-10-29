package com.aegisql.conveyor;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AssemblingConveyorTest {

	public static class Name {
		final String first;
		final String last;
		final int yob;

		public Name(String first, String last, int yob) {
			super();
			this.first = first;
			this.last = last;
			this.yob = yob;
		}

		public String getFirst() {
			return first;
		}

		public String getLast() {
			return last;
		}

		@Override
		public String toString() {
			return "Name [first=" + first + ", last=" + last + ", yob=" + yob + "]";
		}

	}

	public static class NameBuilder implements Builder<Name> {

		String first;
		String last;
		Integer yob;

		public Integer getYob() {
			return yob;
		}

		public void setYob(Integer yob) {
			this.yob = yob;
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
		public Name build() {
			return new Name(first, last, yob);
		}

		@Override
		public boolean ready() {
			return first != null && last != null && yob != null;
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
		AssemblingConveyor<Integer, Cart<Integer, ?>, Name> c = new AssemblingConveyor<Integer, Cart<Integer, ?>, Name>(
				(Class<? extends Builder<Name>>) NameBuilder.class, (cart, b) -> {
					NameBuilder nb = (NameBuilder) b;
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
						nb.setYob((Integer) cart.getValue());
						break;
					default:
						throw new RuntimeException("Unknown cart " + cart);
					}
				});

		Cart<Integer, String> c1 = new Cart<Integer, String>(1, "Mike", "setFirst");
		Cart<Integer, String> c2 = new Cart<Integer, String>(1, "Teplitskiy", "setLast");
		Cart<Integer, String> c3 = new Cart<Integer, String>(2, "Mike", "setFirst");
		Cart<Integer, Integer> c4 = new Cart<Integer, Integer>(1, 1970, "setYob");

		c.offer(c1);
		System.out.println(c.poll());
		c.offer(c2);
		c.offer(c3);
		c.offer(c4);
		Thread.sleep(100);

		System.out.println(c.poll());
		System.out.println(c.poll());

		c.stop();
	}

}

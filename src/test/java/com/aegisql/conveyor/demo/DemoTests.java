/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(Suite.class)
@SuiteClasses({
	com.aegisql.conveyor.demo.caching_conveyor.Demo.class,
	com.aegisql.conveyor.demo.conveyor_smart_builder.Demo.class,
	com.aegisql.conveyor.demo.conveyor_timeout.Demo.class,
	com.aegisql.conveyor.demo.conveyor_timeout_action.Demo.class,
	com.aegisql.conveyor.demo.scalar_conveyor.Demo.class,
	com.aegisql.conveyor.demo.simple_builder.Demo.class,
	com.aegisql.conveyor.demo.simple_builder_asynch.Demo.class,
	com.aegisql.conveyor.demo.simple_conveyor.Demo.class,
	com.aegisql.conveyor.demo.smart_conveyor.Demo.class,
	com.aegisql.conveyor.demo.smart_conveyor_labels.Demo.class,
})
public class DemoTests {

}

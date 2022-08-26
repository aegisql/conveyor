/* 
 * COPYRIGHT (C) AEGIS DATA SOLUTIONS, LLC, 2015
 */
package com.aegisql.conveyor.demo.weather;

import com.aegisql.conveyor.AssemblingConveyor;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.loaders.PartLoader;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import static com.aegisql.conveyor.demo.weather.MonthSummaryLabels.WEATHER_RECORD;
import static com.aegisql.conveyor.demo.weather.WeatherRecordFields.DONE;

public class Demo {
	final static String PATTERN = "MM/dd/yy";

	public static void main(String[] args) throws InterruptedException, ExecutionException, FileNotFoundException, ParseException {

		if(args == null || args.length == 0) {
			System.err.println("Usage: Demo path_to_CSV_file");
			return;
		}

		// we'll put results here
		Queue<WeatherRecord> weatherRecords = new ConcurrentLinkedQueue<>();
		Queue<MonthSummary> summary = new ConcurrentLinkedQueue<>();

		// Create and configure conveyor for monthly summary
		Conveyor<String, MonthSummaryLabels, MonthSummary> summaryCollector = new AssemblingConveyor<>();
		summaryCollector.setName("month_summary");
		summaryCollector.setBuilderSupplier(MonthSummaryCollector::new);
		summaryCollector.resultConsumer(new ResultConsumer<String, MonthSummary>() {
			@Override
			public void accept(ProductBin<String, MonthSummary> bin) {
				MonthSummary product = bin.product;
				product.setStation((String) bin.properties.get("STATION"));
				summary.add(product);
			}
		}).set();
		summaryCollector.setDefaultBuilderTimeout(Duration.ofMillis(1000));
		summaryCollector.enablePostponeExpiration(true);
		summaryCollector.setExpirationPostponeTime(Duration.ofMillis(1000));


		// Create field collector conveyor
		Conveyor<String, WeatherRecordFields, WeatherRecord> fieldCollector = new AssemblingConveyor<>();
		fieldCollector.setName("weather_records");
		fieldCollector.setBuilderSupplier(WeatherRecordCollector::new);
		fieldCollector.resultConsumer(new ResultConsumer<String, WeatherRecord>() {
			@Override
			public void accept(ProductBin<String, WeatherRecord> bin) {
				String station = (String) bin.properties.get("STATION");
				Date date = (Date) bin.properties.get("DATE");
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				int month = calendar.get(Calendar.MONTH);
				WeatherRecord product = bin.product;
				product.setStation(station);
				product.setDate(date);
				weatherRecords.add(product);
				summaryCollector.part()
						.id(station+"."+month)
						.label(WEATHER_RECORD)
						.value(product)
						.addProperty("STATION",station)
						.place();
			}
		}).set();
		fieldCollector.setReadinessEvaluator(Conveyor.getTesterFor(fieldCollector).accepted(DONE));

		PartLoader<String, WeatherRecordFields> recordDoneLoader = fieldCollector.part().foreach().label(DONE);


		// read a file line by line
		File file = FileUtils.getFile(args[0]);
		Scanner input = new Scanner(file);
		while (input.hasNextLine()) {
			String line = input.nextLine();
			String[] fields = line.split(",", 4);
			String station = fields[0];
			String date = fields[1];
			String labelName = fields[2];
			String value = fields[3];

			// place fields in the field collector conveyor
			fieldCollector
					.part().id(station+"."+date)
					.label(WeatherRecordFields.valueOf(labelName))
					.value(value)
					.addProperty("STATION",station)
					.addProperty("DATE",new SimpleDateFormat(PATTERN).parse(date))
					.place();
		}

		// File is read
		CompletableFuture<Boolean> recordDonePlaced = recordDoneLoader.place();
		// wait until all records processed
		recordDonePlaced.join();

		// stop conveyors
		fieldCollector.stop();
		// summary may contain data for incomplete months
		summaryCollector.completeAndStop().join();

		System.out.println("Weather Records Example");
		weatherRecords.stream().limit(10).forEach(System.out::println);
		System.out.println();

		List<MonthSummary> summarySorted = new ArrayList<>(summary);
		Collections.sort(summarySorted);

		System.out.println("Month Summary Example");
		summarySorted.subList(summarySorted.size()-16, summarySorted.size()).forEach(System.out::println);
		System.out.println();
	}

	@Test
	public void test() throws Exception {
		// Load weather stations data provided by the National Centers for Environmental Information
		main(new String[]{"src/test/resources/weather.csv"});
	}
}

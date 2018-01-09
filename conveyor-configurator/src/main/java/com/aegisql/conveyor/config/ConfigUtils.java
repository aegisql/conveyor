package com.aegisql.conveyor.config;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Status;

class ConfigUtils {

	public final static Function<String,Object> timeToMillsConverter = val -> {
		String[] parts = val.trim().split("\\s+");
		TimeUnit u;
		if(parts.length == 1) {
			u = TimeUnit.MILLISECONDS;
		} else {
			u = TimeUnit.valueOf(parts[1]);
		}
		Double d = Double.valueOf(parts[0]);
		
		long big = d.longValue();
		long one = u.toMillis(1);
		
		return u.toMillis(big)+(long)(one*(d.doubleValue()-big));
	};

	public final static Function<String,Object> stringToStatusConverter = val -> {
		String[] parts = val.trim().split(",");
		Status[] res = new Status[parts.length];

		for(int i = 0; i < parts.length; i++) {
			res[i] = Status.valueOf(parts[i]);
		}
		
		return res;
	};

	private final static ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");

	private final static String getBuilderSupplierJs = 
			  "var getBuilderSupplier = function() {\n" 
			+ "		var BuilderSupplier = Java.type('com.aegisql.conveyor.BuilderSupplier');\n"
			+ "		var SupplierImpl = Java.extend(BuilderSupplier, {\n"
			+ "			get: function() {\n"
	        + "				return %s\n;"
	    		+ "			}});\n"
			+ "    return new SupplierImpl();\n" 
			+ "};\n";
	
	public final static Function<String,Object> stringToBuilderSupplier = js-> {
		try {
			engine.eval(String.format(getBuilderSupplierJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getBuilderSupplier");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToBuilderSupplier error",e);
		}
	};

	private final static String getResultConsumerJs = 
			  "var getResultConsumer = function() {\n" 
			+ "		var ResultConsumer = Java.type('com.aegisql.conveyor.consumers.result.ResultConsumer');\n"
			+ "     var rc = %s;"
			+ "		var SupplierImpl = Java.extend(ResultConsumer, {\n"
			+ "			accept: function(bin) {\n"
	        + "				rc.accept(bin)\n;"
	    		+ "			}});\n"
			+ "    return new SupplierImpl();\n" 
			+ "};\n";

	public final static Function<String,Object> stringToResultConsumerSupplier = js -> {
		try {
			engine.eval(String.format(getResultConsumerJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getResultConsumer");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToResultConsumerSupplier error",e);
		}
	};

	private final static String getScrapConsumerJs = 
			  "var getScrapConsumer = function() {\n" 
			+ "		var ScrapConsumer = Java.type('com.aegisql.conveyor.consumers.scrap.ScrapConsumer');\n"
			+ "     var sc = %s;"
			+ "		var SupplierImpl = Java.extend(ScrapConsumer, {\n"
			+ "			accept: function(bin) {\n"
	        + "				sc.accept(bin)\n;"
	    		+ "			}});\n"
			+ "    return new SupplierImpl();\n" 
			+ "};\n";

	public final static Function<String,Object> stringToScrapConsumerSupplier = js -> {
		try {
			engine.eval(String.format(getScrapConsumerJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getScrapConsumer");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToScrapConsumerSupplier error",e);
		}
	};

	private final static String getLabelValuePairJs = 
			  "var getLabelValuePair = function() {\n" 
			+ "		var Pair = Java.type('com.aegisql.conveyor.config.Pair');\n"
			+ "     %s;"
			+ "    return new Pair(label,value);\n" 
			+ "};\n";

	public final static Function<String,Object> stringToLabelValuePairSupplier = js -> {
		try {
			engine.eval(String.format(getLabelValuePairJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getLabelValuePair");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToLabelValuePairSupplier error",e);
		}
	};

	
}

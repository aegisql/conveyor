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

	private final static String sbjs = 
			  "var getBuilderSupplier = function() {\n" 
			+ "		var BuilderSupplier = Java.type('com.aegisql.conveyor.BuilderSupplier');\n"
			+ "		var SupplierImpl = Java.extend(BuilderSupplier, {\n"
			+ "			get: function() {\n"
	        + "				return %s\n;"
	    		+ "			}});\n"
			+ "    return new SupplierImpl();\n" 
			+ "};\n";
	
	public final static Function<String,Object> stringToBuilderSupplier = js-> {
		
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
		try {
			engine.eval(String.format(sbjs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getBuilderSupplier");
			return result;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	};
	
	
}

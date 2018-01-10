package com.aegisql.conveyor.config;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.cart.Cart;

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

	private final static String consumerJs = 
			  "var getConsumer = function() {\n" 
			+ "		var Consumer = Java.type('java.util.function.Consumer');\n"
			+ "     var consumer = %s;"
			+ "		var SupplierImpl = Java.extend(Consumer, {\n"
			+ "			accept: function(builder) {\n"
	        + "				consumer.accept(builder)\n;"
	    		+ "			}});\n"
			+ "    return new SupplierImpl();\n" 
			+ "};\n";

	public final static Function<String,Object> stringToConsumerSupplier = js -> {
		try {
			engine.eval(String.format(consumerJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getConsumer");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToConsumerSupplier error",e);
		}
	};

	private final static String getLabeledValueConsumerJs = 
			  "var getLabeledValueConsumer = function() {\n" 
			+ "		var LabeledValueConsumer = Java.type('com.aegisql.conveyor.LabeledValueConsumer');\n"
			+ "     var consumer = %s;"
			+ "		var SupplierImpl = Java.extend(LabeledValueConsumer, {\n"
			+ "			accept: function(l,v,b) {\n"
	        + "				consumer.accept(l,v,b)\n;"
	    		+ "			}});\n"
			+ "    return new SupplierImpl();\n" 
			+ "};\n";

	public static final Function<String,Object> stringToLabeledValueConsumerSupplier = js -> {
		try {
			engine.eval(String.format(getLabeledValueConsumerJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getLabeledValueConsumer");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToLabeledValueConsumerSupplier error",e);
		}		
	};

	private final static String getReadinessEvaluatorJs = 
			  "var getReadinessEvaluator = function() {\n" 
			+ "		var Predicate = Java.type('java.util.function.Predicate');\n"
			+ "		var BiPredicate = Java.type('java.util.function.BiPredicate');\n"
			+ "     var re = %s;"
			+ "		var REImpl;\n"
			+ "		if(BiPredicate.class.isAssignableFrom(re.getClass())) {\n"
			+ "			REImpl = Java.extend(BiPredicate, {\n"
			+ "				test: function(a,b) {\n"
	        + "					return re.test(a,b)\n;"
	    		+ "				}});}\n"
			+ "		if(Predicate.class.isAssignableFrom(re.getClass())) {\n"
			+ "			REImpl = Java.extend(Predicate, {\n"
			+ "				test: function(a) {\n"
	        + "					return re.test(a)\n;"
	    		+ "				}});}\n"
			+ "    return new REImpl();\n" 
			+ "};\n";

	public static final Function<String, Object> stringToReadinessEvaluatorSupplier = js -> {
		try {
			engine.eval(String.format(getReadinessEvaluatorJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getReadinessEvaluator");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToReadinessEvaluatorSupplier error",e);
		}		
	};

	private final static String biConsumerJs = 
			  "var getBiConsumer = function() {\n" 
			+ "		var BiConsumer = Java.type('java.util.function.BiConsumer');\n"
			+ "     var consumer = %s;"
			+ "		var SupplierImpl = Java.extend(BiConsumer, {\n"
			+ "			accept: function(a,b) {\n"
	        + "				consumer.accept(a,b)\n;"
	    		+ "			}});\n"
			+ "    return new SupplierImpl();\n" 
			+ "};\n";

	public final static Function<String,Object> stringToBiConsumerSupplier = js -> {
		try {
			engine.eval(String.format(biConsumerJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getBiConsumer");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToBiConsumerSupplier error",e);
		}
	};

	private final static String getLabelArrayConsumerJs = 
			  "var getLabelArrayConsumer = function() {\n" 
			+ "		var ObjectArray = Java.type('java.lang.Object[]');\n"
			+ "     var array = [%s];"
			+ "     var res = new ObjectArray(array.length);\n" 
			+ "     for(i = 0; i < array.length; i++) { res[i] = array[i];};\n" 
			+ "     return res;\n" 
			+ "};\n";

	public static final Function<String, Object> stringToLabelArraySupplier = js -> {
		try {
			engine.eval(String.format(getLabelArrayConsumerJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getLabelArrayConsumer");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToLabelArraySupplier error",e);
		}
	};

	private final static String functionJs = 
			  "var getFunction = function() {\n" 
			+ "		var Function = Java.type('java.util.function.Function');\n"
			+ "     var f = %s;"
			+ "		var SupplierImpl = Java.extend(Function, {\n"
			+ "			apply: function(x) {\n"
	        + "				return f.apply(x)\n;"
	    		+ "			}});\n"
			+ "    return new SupplierImpl();\n" 
			+ "};\n";

	public static final Function<String, Object> stringToCartPayloadFunctionSupplier = js -> {
		try {
			engine.eval(String.format(functionJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getFunction");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToCartPayloadFunctionSupplier error",e);
		}
	};

}

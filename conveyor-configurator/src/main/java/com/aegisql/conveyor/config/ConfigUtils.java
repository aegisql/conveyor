package com.aegisql.conveyor.config;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.core.ObjectConverter;
import com.aegisql.id_builder.IdSource;

// TODO: Auto-generated Javadoc
/**
 * The Class ConfigUtils.
 */
class ConfigUtils {

	/** The Constant timeToMillsConverter. */
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

	/** The Constant stringToStatusConverter. */
	public final static Function<String,Object> stringToStatusConverter = val -> {
		String[] parts = val.trim().split(",");
		Status[] res = new Status[parts.length];

		for(int i = 0; i < parts.length; i++) {
			res[i] = Status.valueOf(parts[i]);
		}
		
		return res;
	};


	/** The Constant getBuilderSupplierJs. */
	private final static String getBuilderSupplierJs = 
			  "var getBuilderSupplier = function() {\n" 
			+ "		var BuilderSupplier = Java.type('com.aegisql.conveyor.BuilderSupplier');\n"
			+ "		var SupplierImpl = Java.extend(BuilderSupplier, {\n"
			+ "			get: function() {\n"
	        + "				return %s;\n"
	    		+ "			}});\n"
			+ "    return new SupplierImpl();\n" 
			+ "};\n";
	
	/** The Constant stringToBuilderSupplier. */
	public final static Function<String,Object> stringToBuilderSupplier = js-> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
			engine.eval(String.format(getBuilderSupplierJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getBuilderSupplier");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToBuilderSupplier error",e);
		}
	};
	
	/** The Constant getResultConsumerJs. */
	private final static String getResultConsumerJs = 
			  "var getResultConsumer = function() {\n" 
			+ "		var ResultConsumer = Java.type('com.aegisql.conveyor.consumers.result.ResultConsumer');\n"
			+ "     var rc = %s;\n"
			+ "		var SupplierImpl = Java.extend(ResultConsumer, {\n"
			+ "			accept: function(bin) {\n"
	        + "				rc(bin)\n;"
	    		+ "			}});\n"
			+ "    return new SupplierImpl();\n" 
			+ "};\n";

	/** The Constant stringToResultConsumerSupplier. */
	public final static Function<String,Object> stringToResultConsumerSupplier = js -> {
		try {			
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
			engine.eval(String.format(getResultConsumerJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getResultConsumer");
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new ConveyorConfigurationException("stringToResultConsumerSupplier error",e);
		}
	};

	/** The Constant getScrapConsumerJs. */
	private final static String getScrapConsumerJs = 
			  "var getScrapConsumer = function() {\n" 
			+ "		var ScrapConsumer = Java.type('com.aegisql.conveyor.consumers.scrap.ScrapConsumer');\n"
			+ "     var sc = %s;\n"
			+ "		var SupplierImpl = Java.extend(ScrapConsumer, {\n"
			+ "			accept: function(bin) {\n"
	        + "				sc(bin)\n;"
	    		+ "			}});\n"
			+ "    return new SupplierImpl();\n" 
			+ "};\n";

	/** The Constant stringToScrapConsumerSupplier. */
	public final static Function<String,Object> stringToScrapConsumerSupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
			engine.eval(String.format(getScrapConsumerJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getScrapConsumer");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToScrapConsumerSupplier error",e);
		}
	};

	/** The Constant getLabelValuePairJs. */
	private final static String getLabelValuePairJs = 
			  "var getLabelValuePair = function() {\n" 
			+ "		var Pair = Java.type('com.aegisql.conveyor.config.Pair');\n"
			+ "     %s;"
			+ "    return new Pair(label,value);\n" 
			+ "};\n";

	/** The Constant getLabelForwardTrioJs. */
	private final static String getLabelForwardTrioJs = 
			  "var getLabelValueTrio = function() {\n" 
			+ "%s;\n"
			+ "var Trio = Java.type('com.aegisql.conveyor.config.Trio');\n"
			+ "if(typeof keyTransformer === \"undefined\") {return new Trio(label,name,null);}\n"
			+ "		var Function = Java.type('java.util.function.Function');\n"
			+ "		var FunctionImpl = Java.extend(Function, {\n"
			+ "			apply: function(x) {\n"
	        + "				return keyTransformer(x)\n;"
	    		+ "			}});\n"
//			+ "		var keyTransformer = function(k){return k}; var x =  keyTransformer('a');\n"
			+ "    return new Trio(label,name,new FunctionImpl());\n" 
			+ "};\n";

	/** The Constant stringToLabelValuePairSupplier. */
	public final static Function<String,Object> stringToLabelValuePairSupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
			engine.eval(String.format(getLabelValuePairJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getLabelValuePair");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToLabelValuePairSupplier error",e);
		}
	};

	/** The Constant stringToForwardTrioSupplier. */
	public final static Function<String,Object> stringToForwardTrioSupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
			engine.eval(String.format(getLabelForwardTrioJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getLabelValueTrio");
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new ConveyorConfigurationException("stringToForwardTrioSupplier error",e);
		}
	};

	/** The Constant consumerJs. */
	private final static String consumerJs = 
			  "var getConsumer = function() {\n" 
			+ "		var Consumer = Java.type('java.util.function.Consumer');\n"
			+ "     var consumer = %s;"
			+ "		var SupplierImpl = Java.extend(Consumer, {\n"
			+ "			accept: function(builder) {\n"
	        + "				consumer(builder)\n;"
	    		+ "			}});\n"
			+ "    return new SupplierImpl();\n" 
			+ "};\n";

	/** The Constant stringToConsumerSupplier. */
	public final static Function<String,Object> stringToConsumerSupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
			engine.eval(String.format(consumerJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getConsumer");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToConsumerSupplier error",e);
		}
	};

	/** The Constant getLabeledValueConsumerJs. */
	private final static String getLabeledValueConsumerJs = 
			  "var getLabeledValueConsumer = function() {\n" 
			+ "		var LabeledValueConsumer = Java.type('com.aegisql.conveyor.LabeledValueConsumer');\n"
			+ "     var consumer = %s;"
			+ "		var SupplierImpl = Java.extend(LabeledValueConsumer, {\n"
			+ "			accept: function(l,v,b) {\n"
	        + "				consumer(l,v,b)\n;"
	    		+ "			}});\n"
			+ "    return new SupplierImpl();\n" 
			+ "};\n";

	/** The Constant stringToLabeledValueConsumerSupplier. */
	public static final Function<String,Object> stringToLabeledValueConsumerSupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
			engine.eval(String.format(getLabeledValueConsumerJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getLabeledValueConsumer");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToLabeledValueConsumerSupplier error",e);
		}		
	};

	/** The Constant getReadinessEvaluatorJs. */
	private final static String getReadinessEvaluatorJs = 
			  "var getReadinessEvaluator = function() {\n" 
			+ "		var Predicate = Java.type('java.util.function.Predicate');\n"
			+ "		var BiPredicate = Java.type('java.util.function.BiPredicate');\n"
			+ "     var re = %s;\n"
			+ "		var REImpl;\n"
			+ "		if(re.length == 2) {\n"
			+ "			REImpl = Java.extend(BiPredicate, {\n"
			+ "				test: function(a,b) {\n"
	        + "					return re(a,b)\n;"
	    		+ "				}});}\n"
			+ "		 else if(re.length == 1) {\n"
			+ "			REImpl = Java.extend(Predicate, {\n"
			+ "				test: function(a) {\n"
	        + "					return re(a)\n;"
	    		+ "				}});}\n"
			+ "		else if(BiPredicate.class.isAssignableFrom(re.getClass())) {\n"
			+ "			REImpl = Java.extend(BiPredicate, {\n"
			+ "				test: function(a,b) {\n"
	        + "					return re(a,b)\n;"
	    		+ "				}});}\n"
			+ "		 else if(Predicate.class.isAssignableFrom(re.getClass())) {\n"
			+ "			REImpl = Java.extend(Predicate, {\n"
			+ "				test: function(a) {\n"
	        + "					return re(a)\n;"
	    		+ "				}});}\n"
			+ "    return new REImpl();\n" 
			+ "};\n";

	/** The Constant stringToReadinessEvaluatorSupplier. */
	public static final Function<String, Object> stringToReadinessEvaluatorSupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
			engine.eval(String.format(getReadinessEvaluatorJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getReadinessEvaluator");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToReadinessEvaluatorSupplier error",e);
		}		
	};

	/** The Constant biConsumerJs. */
	private final static String biConsumerJs = 
			  "var getBiConsumer = function() {\n" 
			+ "		var BiConsumer = Java.type('java.util.function.BiConsumer');\n"
			+ "     var consumer = %s;"
			+ "		var SupplierImpl = Java.extend(BiConsumer, {\n"
			+ "			accept: function(a,b) {\n"
	        + "				consumer(a,b)\n;"
	    		+ "			}});\n"
			+ "    return new SupplierImpl();\n" 
			+ "};\n";

	/** The Constant stringToBiConsumerSupplier. */
	public final static Function<String,Object> stringToBiConsumerSupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
			engine.eval(String.format(biConsumerJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getBiConsumer");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToBiConsumerSupplier error",e);
		}
	};

	/** The Constant getLabelArrayConsumerJs. */
	private final static String getLabelArrayConsumerJs = 
			  "var getLabelArrayConsumer = function() {\n" 
			+ "		var ObjectArray = Java.type('java.lang.Object[]');\n"
			+ "     var array = [%s];"
			+ "     var res = new ObjectArray(array.length);\n" 
			+ "     for(i = 0; i < array.length; i++) { res[i] = array[i];};\n" 
			+ "     return res;\n" 
			+ "};\n";

	/** The Constant stringToLabelArraySupplier. */
	public static final Function<String, Object> stringToLabelArraySupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
			engine.eval(String.format(getLabelArrayConsumerJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getLabelArrayConsumer");
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			throw new ConveyorConfigurationException("stringToLabelArraySupplier error",e);
		}
	};

	/** The Constant functionJs. */
	private final static String functionJs = 
			  "var getFunction = function() {\n" 
			+ "		var Function = Java.type('java.util.function.Function');\n"
			+ "     var f = %s;"
			+ "		var FunctionImpl = Java.extend(Function, {\n"
			+ "			apply: function(x) {\n"
	        + "				return f(x);\n"
	    		+ "			}});\n"
			+ "    return new FunctionImpl();\n" 
			+ "};\n";

	/** The Constant stringToFunctionSupplier. */
	public static final Function<String, Object> stringToFunctionSupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
			engine.eval(String.format(functionJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getFunction");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToFunctionSupplier error",e);
		}
	};

	/** The Constant getSupplierJs. */
	private final static String getSupplierJs = 
			  "var getSupplier = function() {\n" 
			+ "		var Supplier = Java.type('java.util.function.Supplier');\n"
			+ "		var SupplierImpl = Java.extend(Supplier, {\n"
			+ "			get: function() {\n"
	        + "				return %s\n;"
	    		+ "			}});\n"
			+ "    return new SupplierImpl();\n" 
			+ "};\n";
	
	/** The Constant stringToConveyorSupplier. */
	public final static Function<String,Supplier<Conveyor>> stringToConveyorSupplier = js-> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
			engine.eval(String.format(getSupplierJs, js));
			Invocable invocable = (Invocable) engine;
			return (Supplier<Conveyor>) invocable.invokeFunction("getSupplier");
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToConveyorSupplier error",e);
		}
	};
	
	
	/** The Constant getLabeledValueConsumerJs. */
	private final static String getObjectConverterJs = 
			  "var getObjectConverter = function() {\n" 
			+ "    return %s;\n" 
			+ "};\n";

	/** The Constant stringToObjectConverter. */
	public static final Function<String,ObjectConverter> stringToObjectConverter = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
			engine.eval(String.format(getObjectConverterJs, js));
			Invocable invocable = (Invocable) engine;
			ObjectConverter result = (ObjectConverter) invocable.invokeFunction("getObjectConverter");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToObjectConverter error",e);
		}		
	};

	/** The Constant stringToArchiverConverter. */
	public static final Function<String,Archiver> stringToArchiverConverter = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
			engine.eval(String.format(getObjectConverterJs, js));
			Invocable invocable = (Invocable) engine;
			Archiver result = (Archiver) invocable.invokeFunction("getObjectConverter");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToArchiverConverter error",e);
		}		
	};

	/** The Constant stringToRefConverter. */
	public static final Function<String,Object> stringToRefConverter = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
			engine.eval(String.format(getObjectConverterJs, js));
			Invocable invocable = (Invocable) engine;
			Object result = invocable.invokeFunction("getObjectConverter");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToRefConverter error",e);
		}		
	};

	/** The Constant getLongSupplierJs. */
	private final static String getLongSupplierJs = 
			  "var getLongSupplier = function() {\n" 
			+ "		var LongSupplier = Java.type('java.util.function.LongSupplier');\n"
			+ "		var SupplierImpl = Java.extend(LongSupplier, {\n"
			+ "			getAsLong: function() {\n"
	        + "				return %s;\n"
	    		+ "			}});\n"
			+ "    return new SupplierImpl();\n" 
			+ "};\n";

	/** The Constant stringToIdSupplier. */
	public static final Function<String,LongSupplier> stringToIdSupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
			engine.eval(String.format(getLongSupplierJs, js));
			Invocable invocable = (Invocable) engine;
			LongSupplier result = (LongSupplier) invocable.invokeFunction("getLongSupplier");
			return result;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToIdSupplier error",e);
		}		
	};
	
	
}

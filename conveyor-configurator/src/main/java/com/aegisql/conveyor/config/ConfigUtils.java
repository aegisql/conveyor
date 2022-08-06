package com.aegisql.conveyor.config;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.core.ObjectConverter;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Class ConfigUtils.
 */
class ConfigUtils {

	public final static String JAVASCRIPT_ENGINE = "graal.js";

	static {
		System.setProperty("polyglot.js.nashorn-compat", "true");
	}

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
		
		return u.toMillis(big)+(long)(one*(d -big));
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
			"""
					var getBuilderSupplier = function() {
							var BuilderSupplier = Java.type('com.aegisql.conveyor.BuilderSupplier');
							var SupplierImpl = Java.extend(BuilderSupplier, {
								get: function() {
									return %s;
								}});
					    return new SupplierImpl();
					};
					""";
	
	/** The Constant stringToBuilderSupplier. */
	public final static Function<String,Object> stringToBuilderSupplier = js-> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
			engine.eval(String.format(getBuilderSupplierJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getBuilderSupplier");
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToBuilderSupplier error",e);
		}
	};
	
	/** The Constant getResultConsumerJs. */
	private final static String getResultConsumerJs =
			"""
					var getResultConsumer = function() {
						var ResultConsumer = Java.type('com.aegisql.conveyor.consumers.result.ResultConsumer');
					    var rc = %s;
						var SupplierImpl = Java.extend(ResultConsumer, {
							accept: function(bin) {
								rc(bin)
					;		}});
					    return new SupplierImpl();
					};
					""";

	/** The Constant stringToResultConsumerSupplier. */
	public final static Function<String,Object> stringToResultConsumerSupplier = js -> {
		try {			
			ScriptEngine engine = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
			engine.eval(String.format(getResultConsumerJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getResultConsumer");
		} catch (Exception e) {
			e.printStackTrace();
			throw new ConveyorConfigurationException("stringToResultConsumerSupplier error",e);
		}
	};

	/** The Constant getScrapConsumerJs. */
	private final static String getScrapConsumerJs =
			"""
					var getScrapConsumer = function() {
						var ScrapConsumer = Java.type('com.aegisql.conveyor.consumers.scrap.ScrapConsumer');
					    var sc = %s;
						var SupplierImpl = Java.extend(ScrapConsumer, {
							accept: function(bin) {
								sc(bin)
					;		}});
					    return new SupplierImpl();
					};
					""";

	/** The Constant stringToScrapConsumerSupplier. */
	public final static Function<String,Object> stringToScrapConsumerSupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
			engine.eval(String.format(getScrapConsumerJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getScrapConsumer");
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToScrapConsumerSupplier error",e);
		}
	};

	/** The Constant getLabelValuePairJs. */
	private final static String getLabelValuePairJs =
			"""
					var getLabelValuePair = function() {
						var Pair = Java.type('com.aegisql.conveyor.config.Pair');
					    %s;
					    return new Pair(label,value);
					};
					""";

	//			+ "		var keyTransformer = function(k){return k}; var x =  keyTransformer('a');\n"
	/** The Constant getLabelForwardTrioJs. */
	private final static String getLabelForwardTrioJs =
			"""
					var getLabelValueTrio = function() {
						%s;
						var Trio = Java.type('com.aegisql.conveyor.config.Trio');
						if(typeof keyTransformer === "undefined") {return new Trio(label,name,null);}
						var Function = Java.type('java.util.function.Function');
						var FunctionImpl = Java.extend(Function, {
							apply: function(x) {
								return keyTransformer(x);
							}});
						return new Trio(label,name,new FunctionImpl());
					};
					""";

	/** The Constant stringToLabelValuePairSupplier. */
	public final static Function<String,Object> stringToLabelValuePairSupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
			engine.eval(String.format(getLabelValuePairJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getLabelValuePair");
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToLabelValuePairSupplier error",e);
		}
	};

	/** The Constant stringToForwardTrioSupplier. */
	public final static Function<String,Object> stringToForwardTrioSupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
			engine.eval(String.format(getLabelForwardTrioJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getLabelValueTrio");
		} catch (Exception e) {
			e.printStackTrace();
			throw new ConveyorConfigurationException("stringToForwardTrioSupplier error",e);
		}
	};

	/** The Constant consumerJs. */
	private final static String consumerJs =
			"""
					var getConsumer = function() {
						var Consumer = Java.type('java.util.function.Consumer');
					    var consumer = %s;
					    var SupplierImpl = Java.extend(Consumer, {
							accept: function(builder) {
								consumer(builder)
					;		}});
					    return new SupplierImpl();
					};
					""";

	/** The Constant stringToConsumerSupplier. */
	public final static Function<String,Object> stringToConsumerSupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
			engine.eval(String.format(consumerJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getConsumer");
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToConsumerSupplier error",e);
		}
	};

	/** The Constant getLabeledValueConsumerJs. */
	private final static String getLabeledValueConsumerJs =
			"""
					var getLabeledValueConsumer = function() {
						var LabeledValueConsumer = Java.type('com.aegisql.conveyor.LabeledValueConsumer');
					    var consumer = %s;
					    var SupplierImpl = Java.extend(LabeledValueConsumer, {
							accept: function(l,v,b) {
								consumer(l,v,b)
					;		}});
					    return new SupplierImpl();
					};
					""";

	/** The Constant stringToLabeledValueConsumerSupplier. */
	public static final Function<String,Object> stringToLabeledValueConsumerSupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
			engine.eval(String.format(getLabeledValueConsumerJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getLabeledValueConsumer");
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToLabeledValueConsumerSupplier error",e);
		}		
	};

	/** The Constant getReadinessEvaluatorJs. */
	private final static String getReadinessEvaluatorJs =
			"""
					var getReadinessEvaluator = function() {
						var Predicate = Java.type('java.util.function.Predicate');
						var BiPredicate = Java.type('java.util.function.BiPredicate');
					    var re = %s;
						var REImpl;
							if(re.length == 2) {
								REImpl = Java.extend(BiPredicate, {
									test: function(a,b) {
										return re(a,b)
					;				}});}
							else if(re.length == 1) {
								REImpl = Java.extend(Predicate, {
									test: function(a) {
										return re(a)
					;				}});}
							else if(BiPredicate.class.isAssignableFrom(re.getClass())) {
								REImpl = Java.extend(BiPredicate, {
									test: function(a,b) {
										return re(a,b)
					;				}});}
							else if(Predicate.class.isAssignableFrom(re.getClass())) {
								REImpl = Java.extend(Predicate, {
									test: function(a) {
										return re(a)
					;				}});}
					    return new REImpl();
					};
					""";

	/** The Constant stringToReadinessEvaluatorSupplier. */
	public static final Function<String, Object> stringToReadinessEvaluatorSupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
			engine.eval(String.format(getReadinessEvaluatorJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getReadinessEvaluator");
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToReadinessEvaluatorSupplier error",e);
		}		
	};

	/** The Constant biConsumerJs. */
	private final static String biConsumerJs =
			"""
					var getBiConsumer = function() {
						var BiConsumer = Java.type('java.util.function.BiConsumer');
					    var consumer = %s;
					    var SupplierImpl = Java.extend(BiConsumer, {
							accept: function(a,b) {
								consumer(a,b)
					;		}});
					    return new SupplierImpl();
					};
					""";

	/** The Constant stringToBiConsumerSupplier. */
	public final static Function<String,Object> stringToBiConsumerSupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
			engine.eval(String.format(biConsumerJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getBiConsumer");
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToBiConsumerSupplier error",e);
		}
	};

	/** The Constant getLabelArrayConsumerJs. */
	private final static String getLabelArrayConsumerJs =
			"""
					var getLabelArrayConsumer = function() {
						var ObjectArray = Java.type('java.lang.Object[]');
					    var array = [%s];
					    var res = new ObjectArray(array.length);
					    for(i = 0; i < array.length; i++) { res[i] = array[i];};
					    return res;
					};
					""";

	/** The Constant stringToLabelArraySupplier. */
	public static final Function<String, Object> stringToLabelArraySupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
			engine.eval(String.format(getLabelArrayConsumerJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getLabelArrayConsumer");
		} catch (Exception e) {
			e.printStackTrace();
			throw new ConveyorConfigurationException("stringToLabelArraySupplier error",e);
		}
	};

	/** The Constant functionJs. */
	private final static String functionJs =
			"""
					var getFunction = function() {
						var Function = Java.type('java.util.function.Function');
					    var f = %s;
					    var FunctionImpl = Java.extend(Function, {
							apply: function(x) {
								return f(x);
							}});
					    return new FunctionImpl();
					};
					""";

	/** The Constant stringToFunctionSupplier. */
	public static final Function<String, Object> stringToFunctionSupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
			engine.eval(String.format(functionJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getFunction");
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToFunctionSupplier error",e);
		}
	};

	/** The Constant getSupplierJs. */
	private final static String getSupplierJs =
			"""
					var getSupplier = function() {
						var Supplier = Java.type('java.util.function.Supplier');
						var SupplierImpl = Java.extend(Supplier, {
							get: function() {
								return %s
					;		}});
					    return new SupplierImpl();
					};
					""";
	
	/** The Constant stringToConveyorSupplier. */
	public final static Function<String,Supplier<Conveyor>> stringToConveyorSupplier = js-> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
			engine.eval(String.format(getSupplierJs, js));
			Invocable invocable = (Invocable) engine;
			return (Supplier<Conveyor>) invocable.invokeFunction("getSupplier");
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToConveyorSupplier error",e);
		}
	};
	
	
	/** The Constant getLabeledValueConsumerJs. */
	private final static String getObjectConverterJs =
			"""
					var getObjectConverter = function() {
					    return %s;
					};
					""";

	/** The Constant stringToObjectConverter. */
	public static final Function<String,ObjectConverter> stringToObjectConverter = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
			engine.eval(String.format(getObjectConverterJs, js));
			Invocable invocable = (Invocable) engine;
			return (ObjectConverter) invocable.invokeFunction("getObjectConverter");
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToObjectConverter error",e);
		}		
	};

	/** The Constant stringToArchiverConverter. */
	public static final Function<String,Archiver> stringToArchiverConverter = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
			engine.eval(String.format(getObjectConverterJs, js));
			Invocable invocable = (Invocable) engine;
			return (Archiver) invocable.invokeFunction("getObjectConverter");
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToArchiverConverter error",e);
		}		
	};

	/** The Constant stringToRefConverter. */
	public static final Function<String,Object> stringToRefConverter = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
			engine.eval(String.format(getObjectConverterJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getObjectConverter");
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToRefConverter error",e);
		}		
	};

	/** The Constant getLongSupplierJs. */
	private final static String getLongSupplierJs =
			"""
					var getLongSupplier = function() {
						var LongSupplier = Java.type('java.util.function.LongSupplier');
						var SupplierImpl = Java.extend(LongSupplier, {
							getAsLong: function() {
								return %s;
							}});
					    return new SupplierImpl();
					};
					""";

	/** The Constant stringToIdSupplier. */
	public static final Function<String,LongSupplier> stringToIdSupplier = js -> {
		try {
			ScriptEngine engine = new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
			engine.eval(String.format(getLongSupplierJs, js));
			Invocable invocable = (Invocable) engine;
			return (LongSupplier) invocable.invokeFunction("getLongSupplier");
		} catch (Exception e) {
			throw new ConveyorConfigurationException("stringToIdSupplier error",e);
		}		
	};
	
	
}

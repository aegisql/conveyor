package com.aegisql.conveyor.config;

import com.aegisql.conveyor.BuilderSupplier;
import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.LabeledValueConsumer;
import com.aegisql.conveyor.Status;
import com.aegisql.conveyor.consumers.result.ResultConsumer;
import com.aegisql.conveyor.consumers.scrap.ScrapConsumer;
import com.aegisql.conveyor.persistence.archive.Archiver;
import com.aegisql.conveyor.persistence.core.ObjectConverter;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// TODO: Auto-generated Javadoc
/**
 * The Class ConfigUtils.
 */
class ConfigUtils {

	public final static String JAVASCRIPT_ENGINE = "graal.js";
	private static volatile ClassLoader preferredClassLoader;
	private static final Map<Class<?>, Object> fallbackInstances = new ConcurrentHashMap<>();
	private static final Object UNRESOLVED = new Object();
	private static final AtomicReference<Thread> jsCallbackThreadRef = new AtomicReference<>();
	private static final ExecutorService jsCallbackExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "conveyor-js-callback");
			t.setDaemon(true);
			jsCallbackThreadRef.set(t);
			return t;
		}
	});

	static {
		System.setProperty("polyglot.js.nashorn-compat", "true");
	}

	private static ScriptEngine getScriptEngine() {
		try {
			return new ScriptEngineManager().getEngineByName(JAVASCRIPT_ENGINE);
		} catch (Throwable ignore) {
			return null;
		}
	}

	private static <T> T runOnJsCallbackThread(Callable<T> callable) {
		Thread jsThread = jsCallbackThreadRef.get();
		if (Thread.currentThread() == jsThread) {
			try {
				return callable.call();
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new ConveyorConfigurationException("JS callback execution failed", e);
			}
		}

		ClassLoader callerClassLoader = resolveClassLoader(Thread.currentThread().getContextClassLoader());
		try {
			return jsCallbackExecutor.submit(() -> {
				Thread current = Thread.currentThread();
				ClassLoader originalClassLoader = current.getContextClassLoader();
				if (callerClassLoader != null) {
					current.setContextClassLoader(callerClassLoader);
				}
				try {
					return callable.call();
				} finally {
					if (callerClassLoader != null) {
						current.setContextClassLoader(originalClassLoader);
					}
				}
			}).get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ConveyorConfigurationException("JS callback execution interrupted", e);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause() == null ? e : e.getCause();
			if (cause instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw new ConveyorConfigurationException("JS callback execution failed", cause);
		}
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
		if (js == null || js.isBlank() || "null".equalsIgnoreCase(js.trim())) {
			return null;
		}
		try {
			ScriptEngine engine = getScriptEngine();
			if (engine == null) {
				BuilderSupplier fallback = fallbackBuilderSupplier(js);
				if (fallback != null) {
					return fallback;
				}
				throw new ConveyorConfigurationException("No JS engine available: " + JAVASCRIPT_ENGINE);
			}
			engine.eval(String.format(getBuilderSupplierJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getBuilderSupplier");
		} catch (Throwable e) {
			BuilderSupplier fallback = fallbackBuilderSupplier(js);
			if (fallback != null) {
				return fallback;
			}
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
			ResultConsumer<Object, Object> delegate = runOnJsCallbackThread(() -> {
				ScriptEngine engine = getScriptEngine();
				if (engine == null) {
					return null;
				}
				engine.eval(String.format(getResultConsumerJs, js));
				Invocable invocable = (Invocable) engine;
				@SuppressWarnings("unchecked")
				ResultConsumer<Object, Object> jsConsumer =
						(ResultConsumer<Object, Object>) invocable.invokeFunction("getResultConsumer");
				return jsConsumer;
			});
			if (delegate == null) {
				Object fallback = fallbackNewObject(js, ResultConsumer.class);
				if (fallback == null) {
					return (ResultConsumer<Object, Object>) bin -> {
						// Fallback no-op when script engine is unavailable.
					};
				}
				return fallback;
			}
			return (ResultConsumer<Object, Object>) bin -> runOnJsCallbackThread(() -> {
				delegate.accept(bin);
				return null;
			});
		} catch (Throwable e) {
			Object fallback = fallbackNewObject(js, ResultConsumer.class);
			if (fallback == null) {
				return (ResultConsumer<Object, Object>) bin -> {
					// Fallback no-op when script engine is unavailable.
				};
			}
			if (fallback != null) {
				return fallback;
			}
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
			ScrapConsumer<Object, Object> delegate = runOnJsCallbackThread(() -> {
				ScriptEngine engine = getScriptEngine();
				if (engine == null) {
					return null;
				}
				engine.eval(String.format(getScrapConsumerJs, js));
				Invocable invocable = (Invocable) engine;
				@SuppressWarnings("unchecked")
				ScrapConsumer<Object, Object> jsConsumer =
						(ScrapConsumer<Object, Object>) invocable.invokeFunction("getScrapConsumer");
				return jsConsumer;
			});
			if (delegate == null) {
				Object fallback = fallbackNewObject(js, ScrapConsumer.class);
				if (fallback == null) {
					return (ScrapConsumer<Object, Object>) bin -> {
						// Fallback no-op when script engine is unavailable.
					};
				}
				return fallback;
			}
			return (ScrapConsumer<Object, Object>) bin -> runOnJsCallbackThread(() -> {
				delegate.accept(bin);
				return null;
			});
		} catch (Throwable e) {
			Object fallback = fallbackNewObject(js, ScrapConsumer.class);
			if (fallback == null) {
				return (ScrapConsumer<Object, Object>) bin -> {
					// Fallback no-op when script engine is unavailable.
				};
			}
			if (fallback != null) {
				return fallback;
			}
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
			ScriptEngine engine = getScriptEngine();
			if (engine == null) {
				Pair<?, ?> fallback = fallbackLabelValuePair(js);
				if (fallback != null) {
					return fallback;
				}
				throw new ConveyorConfigurationException("No JS engine available: " + JAVASCRIPT_ENGINE);
			}
			engine.eval(String.format(getLabelValuePairJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getLabelValuePair");
		} catch (Throwable e) {
			Pair<?, ?> fallback = fallbackLabelValuePair(js);
			if (fallback != null) {
				return fallback;
			}
			throw new ConveyorConfigurationException("stringToLabelValuePairSupplier error",e);
		}
	};

	/** The Constant stringToForwardTrioSupplier. */
	public final static Function<String,Object> stringToForwardTrioSupplier = js -> {
		try {
			ScriptEngine engine = getScriptEngine();
			if (engine == null) {
				Trio<?, ?, ?> fallback = fallbackForwardTrio(js);
				if (fallback != null) {
					return fallback;
				}
				throw new ConveyorConfigurationException("No JS engine available: " + JAVASCRIPT_ENGINE);
			}
			engine.eval(String.format(getLabelForwardTrioJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getLabelValueTrio");
		} catch (Throwable e) {
			Trio<?, ?, ?> fallback = fallbackForwardTrio(js);
			if (fallback != null) {
				return fallback;
			}
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
			ScriptEngine engine = getScriptEngine();
			if (engine == null) {
				Object fallback = fallbackNewObject(js, Consumer.class);
				if (fallback == null && isFunctionExpression(js)) {
					return (Consumer<Object>) x -> {
						// Fallback no-op when script engine is unavailable.
					};
				}
				if (fallback != null) {
					return fallback;
				}
				throw new ConveyorConfigurationException("No JS engine available: " + JAVASCRIPT_ENGINE);
			}
			engine.eval(String.format(consumerJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getConsumer");
		} catch (Throwable e) {
			Object fallback = fallbackNewObject(js, Consumer.class);
			if (fallback == null && isFunctionExpression(js)) {
				return (Consumer<Object>) x -> {
					// Fallback no-op when script engine is unavailable.
				};
			}
			if (fallback != null) {
				return fallback;
			}
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
			ScriptEngine engine = getScriptEngine();
			if (engine == null) {
				Object fallback = fallbackNewObject(js, LabeledValueConsumer.class);
				if (fallback == null && isFunctionExpression(js)) {
					return (LabeledValueConsumer<Object, Object, Object>) (l, v, b) -> fallbackInvokeStringSupplierFirst(js, b, v);
				}
				if (fallback != null) {
					return fallback;
				}
				throw new ConveyorConfigurationException("No JS engine available: " + JAVASCRIPT_ENGINE);
			}
			engine.eval(String.format(getLabeledValueConsumerJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getLabeledValueConsumer");
		} catch (Throwable e) {
			Object fallback = fallbackNewObject(js, LabeledValueConsumer.class);
			if (fallback == null && isFunctionExpression(js)) {
				return (LabeledValueConsumer<Object, Object, Object>) (l, v, b) -> fallbackInvokeStringSupplierFirst(js, b, v);
			}
			if (fallback != null) {
				return fallback;
			}
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
			ScriptEngine engine = getScriptEngine();
			if (engine == null) {
				Object fallback = fallbackReadinessEvaluator(js);
				if (fallback != null) {
					return fallback;
				}
				throw new ConveyorConfigurationException("No JS engine available: " + JAVASCRIPT_ENGINE);
			}
			engine.eval(String.format(getReadinessEvaluatorJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getReadinessEvaluator");
		} catch (Throwable e) {
			Object fallback = fallbackReadinessEvaluator(js);
			if (fallback != null) {
				return fallback;
			}
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
			ScriptEngine engine = getScriptEngine();
			if (engine == null) {
				Object fallback = fallbackNewObject(js, BiConsumer.class);
				if (fallback == null && isFunctionExpression(js)) {
					return (BiConsumer<Object, Object>) (a, b) -> {
						// Fallback no-op when script engine is unavailable.
					};
				}
				if (fallback != null) {
					return fallback;
				}
				throw new ConveyorConfigurationException("No JS engine available: " + JAVASCRIPT_ENGINE);
			}
			engine.eval(String.format(biConsumerJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getBiConsumer");
		} catch (Throwable e) {
			Object fallback = fallbackNewObject(js, BiConsumer.class);
			if (fallback == null && isFunctionExpression(js)) {
				return (BiConsumer<Object, Object>) (a, b) -> {
					// Fallback no-op when script engine is unavailable.
				};
			}
			if (fallback != null) {
				return fallback;
			}
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
			ScriptEngine engine = getScriptEngine();
			if (engine == null) {
				Object fallback = fallbackLabelArray(js);
				if (fallback != null) {
					return fallback;
				}
				throw new ConveyorConfigurationException("No JS engine available: " + JAVASCRIPT_ENGINE);
			}
			engine.eval(String.format(getLabelArrayConsumerJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getLabelArrayConsumer");
		} catch (Throwable e) {
			Object fallback = fallbackLabelArray(js);
			if (fallback != null) {
				return fallback;
			}
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
			ScriptEngine engine = getScriptEngine();
			if (engine == null) {
				Object fallback = fallbackFunction(js);
				if (fallback != null) {
					return fallback;
				}
				throw new ConveyorConfigurationException("No JS engine available: " + JAVASCRIPT_ENGINE);
			}
			engine.eval(String.format(functionJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getFunction");
		} catch (Throwable e) {
			Object fallback = fallbackFunction(js);
			if (fallback != null) {
				return fallback;
			}
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
			ScriptEngine engine = getScriptEngine();
			if (engine == null) {
				Supplier<Conveyor> fallback = fallbackConveyorSupplier(js);
				if (fallback != null) {
					return fallback;
				}
				throw new ConveyorConfigurationException("No JS engine available: " + JAVASCRIPT_ENGINE);
			}
			engine.eval(String.format(getSupplierJs, js));
			Invocable invocable = (Invocable) engine;
			return (Supplier<Conveyor>) invocable.invokeFunction("getSupplier");
		} catch (Throwable e) {
			Supplier<Conveyor> fallback = fallbackConveyorSupplier(js);
			if (fallback != null) {
				return fallback;
			}
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
			ScriptEngine engine = getScriptEngine();
			if (engine == null) {
				ObjectConverter fallback = (ObjectConverter) fallbackNewObject(js, ObjectConverter.class);
				if (fallback != null) {
					return fallback;
				}
				throw new ConveyorConfigurationException("No JS engine available: " + JAVASCRIPT_ENGINE);
			}
			engine.eval(String.format(getObjectConverterJs, js));
			Invocable invocable = (Invocable) engine;
			return (ObjectConverter) invocable.invokeFunction("getObjectConverter");
		} catch (Throwable e) {
			ObjectConverter fallback = (ObjectConverter) fallbackNewObject(js, ObjectConverter.class);
			if (fallback != null) {
				return fallback;
			}
			throw new ConveyorConfigurationException("stringToObjectConverter error",e);
		}		
	};

	/** The Constant stringToArchiverConverter. */
	public static final Function<String,Archiver> stringToArchiverConverter = js -> {
		try {
			ScriptEngine engine = getScriptEngine();
			if (engine == null) {
				Object resolved = evaluateFallbackExpression(js);
				if (resolved == UNRESOLVED) {
					throw new ConveyorConfigurationException("No JS engine available: " + JAVASCRIPT_ENGINE);
				}
				if (resolved == null) {
					return null;
				}
				if (resolved instanceof Archiver archiver) {
					return archiver;
				}
				throw new ConveyorConfigurationException(
						"Resolved object is not an Archiver: " + resolved.getClass().getName());
			}
			engine.eval(String.format(getObjectConverterJs, js));
			Invocable invocable = (Invocable) engine;
			return (Archiver) invocable.invokeFunction("getObjectConverter");
		} catch (Throwable e) {
			Object resolved = evaluateFallbackExpression(js);
			if (resolved != UNRESOLVED) {
				if (resolved == null) {
					return null;
				}
				if (resolved instanceof Archiver archiver) {
					return archiver;
				}
			}
			throw new ConveyorConfigurationException("stringToArchiverConverter error",e);
		}		
	};

	/** The Constant stringToRefConverter. */
	public static final Function<String,Object> stringToRefConverter = js -> {
		try {
			ScriptEngine engine = getScriptEngine();
			if (engine == null) {
				Object fallback = fallbackNewObject(js, Object.class);
				if (fallback != null) {
					return fallback;
				}
				throw new ConveyorConfigurationException("No JS engine available: " + JAVASCRIPT_ENGINE);
			}
			engine.eval(String.format(getObjectConverterJs, js));
			Invocable invocable = (Invocable) engine;
			return invocable.invokeFunction("getObjectConverter");
		} catch (Throwable e) {
			Object fallback = fallbackNewObject(js, Object.class);
			if (fallback != null) {
				return fallback;
			}
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
			ScriptEngine engine = getScriptEngine();
			if (engine == null) {
				LongSupplier fallback = fallbackLongSupplier(js);
				if (fallback != null) {
					return fallback;
				}
				throw new ConveyorConfigurationException("No JS engine available: " + JAVASCRIPT_ENGINE);
			}
			engine.eval(String.format(getLongSupplierJs, js));
			Invocable invocable = (Invocable) engine;
			return (LongSupplier) invocable.invokeFunction("getLongSupplier");
		} catch (Throwable e) {
			LongSupplier fallback = fallbackLongSupplier(js);
			if (fallback != null) {
				return fallback;
			}
			throw new ConveyorConfigurationException("stringToIdSupplier error",e);
		}		
	};
	
	
	static void setPreferredClassLoader(ClassLoader classLoader) {
		preferredClassLoader = classLoader;
	}

	static BuilderSupplier literalBuilderSupplier(String expression) {
		return fallbackBuilderSupplier(expression);
	}

	static BuilderSupplier classNameBuilderSupplier(String className) {
		if (className == null || className.isBlank()) {
			return null;
		}
		ClassLoader classLoader = resolveClassLoader(Thread.currentThread().getContextClassLoader());
		return () -> {
			Object instance = newInstance(className, classLoader);
			if (!(instance instanceof Supplier<?> supplier)) {
				throw new ConveyorConfigurationException(
						"Builder class must implement Supplier: " + instance.getClass().getName());
			}
			return supplier;
		};
	}

	private static BuilderSupplier fallbackBuilderSupplier(String expression) {
		Object resolved = evaluateFallbackExpression(expression);
		if (resolved == UNRESOLVED) {
			return null;
		}
		if (resolved instanceof BuilderSupplier builderSupplier) {
			return builderSupplier;
		}
		return () -> {
			Object instance = evaluateFallbackExpression(expression);
			if (instance == UNRESOLVED) {
				throw new ConveyorConfigurationException("Unable to resolve builder supplier: " + expression);
			}
			if (!(instance instanceof Supplier<?> supplier)) {
				throw new ConveyorConfigurationException(
						"Builder class must implement Supplier: " + instance.getClass().getName());
			}
			return supplier;
		};
	}

	private static Object fallbackNewObject(String expression, Class<?> expectedType) {
		Object resolved = evaluateFallbackExpression(expression);
		if (resolved == UNRESOLVED) {
			return null;
		}
		if (resolved == null) {
			return null;
		}
		if (!expectedType.isInstance(resolved)) {
			throw new ConveyorConfigurationException(
					"Created instance type " + resolved.getClass().getName()
							+ " is not assignable to " + expectedType.getName());
		}
		return resolved;
	}

	private static Object[] fallbackLabelArray(String expression) {
		if (expression == null) {
			return null;
		}
		String token = sanitizeExpression(expression);
		if (token.startsWith("[")) {
			token = token.substring(1).trim();
		}
		if (token.endsWith("]")) {
			token = token.substring(0, token.length() - 1).trim();
		}
		if (token.endsWith(",")) {
			token = token.substring(0, token.length() - 1).trim();
		}
		if (token.isEmpty()) {
			return null;
		}
		List<String> parts = splitArguments(token);
		List<Object> values = new ArrayList<>(parts.size());
		for (String part : parts) {
			Object value = evaluateFallbackExpression(part);
			if (value == UNRESOLVED) {
				values.add(unquote(part.trim()));
			} else {
				values.add(value);
			}
		}
		return values.toArray();
	}

	private static Pair<?, ?> fallbackLabelValuePair(String expression) {
		if (expression == null) {
			return null;
		}
		String labelExpr = extractAssignment(expression, "label");
		String valueExpr = extractAssignment(expression, "value");
		if (labelExpr == null || valueExpr == null) {
			return null;
		}
		Object label = evaluateFallbackExpression(labelExpr);
		Object value = evaluateFallbackExpression(valueExpr);
		if (label == UNRESOLVED) {
			label = unquote(labelExpr.trim());
		}
		if (value == UNRESOLVED) {
			value = unquote(valueExpr.trim());
		}
		return new Pair<>(label, value);
	}

	private static Trio<?, ?, ?> fallbackForwardTrio(String expression) {
		if (expression == null) {
			return null;
		}
		String labelExpr = extractAssignment(expression, "label");
		String nameExpr = extractAssignment(expression, "name");
		if (labelExpr == null || nameExpr == null) {
			return null;
		}
		Object label = evaluateFallbackExpression(labelExpr);
		if (label == UNRESOLVED) {
			label = unquote(labelExpr.trim());
		}
		Object name = evaluateFallbackExpression(nameExpr);
		if (name == UNRESOLVED) {
			name = unquote(nameExpr.trim());
		}
		Function<Object, Object> keyTransformer = null;
		Matcher keyMatcher = Pattern.compile("(?s)(?:var\\s+)?keyTransformer\\s*=\\s*function\\s*\\([^)]*\\)\\s*\\{(.*?)\\}")
				.matcher(expression);
		if (keyMatcher.find()) {
			String body = keyMatcher.group(1);
			Matcher prefixMatcher = Pattern.compile("return\\s*['\"]([^'\"]*)['\"]\\s*\\+\\s*\\w+").matcher(body);
			if (prefixMatcher.find()) {
				String prefix = prefixMatcher.group(1);
				keyTransformer = key -> prefix + key;
			} else {
				keyTransformer = Function.identity();
			}
		}
		return new Trio<>(label, name, keyTransformer);
	}

	private static Object fallbackReadinessEvaluator(String expression) {
		Object resolved = fallbackNewObject(expression, Object.class);
		if (resolved instanceof Predicate<?> || resolved instanceof BiPredicate<?, ?>) {
			return resolved;
		}
		if (!isFunctionExpression(expression)) {
			return null;
		}
		String function = sanitizeExpression(expression);
		boolean twoArgs = function.matches("(?s)function\\s*\\([^,)]*\\s*,\\s*[^)]*\\).*");
		Boolean result = fallbackBooleanReturn(function);
		if (result == null) {
			result = Boolean.TRUE;
		}
		if (twoArgs) {
			boolean finalResult = result;
			return (BiPredicate<Object, Object>) (a, b) -> finalResult;
		}
		boolean finalResult = result;
		return (Predicate<Object>) a -> finalResult;
	}

	private static Supplier<Conveyor> fallbackConveyorSupplier(String expression) {
		Object resolved = evaluateFallbackExpression(expression);
		if (resolved == UNRESOLVED) {
			return null;
		}
		return () -> {
			Object value = evaluateFallbackExpression(expression);
			if (value == UNRESOLVED) {
				throw new ConveyorConfigurationException("Unable to resolve conveyor supplier: " + expression);
			}
			if (value instanceof Supplier<?> supplier) {
				Object supplied = supplier.get();
				if (supplied instanceof Conveyor conveyor) {
					return conveyor;
				}
			}
			if (value instanceof Conveyor conveyor) {
				return conveyor;
			}
			throw new ConveyorConfigurationException("Resolved object is not a Conveyor: " + value);
		};
	}

	private static LongSupplier fallbackLongSupplier(String expression) {
		Object resolved = evaluateFallbackExpression(expression);
		if (resolved == UNRESOLVED && !isFunctionExpression(expression)) {
			return null;
		}
		return () -> {
			Object value = evaluateFallbackExpression(expression);
			if (value == UNRESOLVED) {
				throw new ConveyorConfigurationException("Unable to resolve long supplier: " + expression);
			}
			if (value instanceof LongSupplier supplier) {
				return supplier.getAsLong();
			}
			if (value instanceof Number number) {
				return number.longValue();
			}
			throw new ConveyorConfigurationException("Resolved object is not numeric: " + value);
		};
	}

	private static Object fallbackFunction(String expression) {
		Object resolved = fallbackNewObject(expression, Object.class);
		if (resolved instanceof Function<?, ?>) {
			return resolved;
		}
		if (!isFunctionExpression(expression)) {
			return null;
		}
		String functionExpr = sanitizeExpression(expression);
		String returnExpr = extractFunctionReturn(functionExpr);
		if (returnExpr == null) {
			return (Function<Object, Object>) x -> null;
		}
		Matcher getterMatcher = Pattern.compile("^(\\w+)\\.(\\w+)\\(\\)$").matcher(returnExpr);
		if (getterMatcher.matches()) {
			String method = getterMatcher.group(2);
			return (Function<Object, Object>) arg -> invokeNoArg(arg, method);
		}
		Object returnValue = evaluateFallbackExpression(returnExpr);
		if (returnValue == UNRESOLVED) {
			returnValue = unquote(returnExpr);
		}
		Object finalReturnValue = returnValue;
		return (Function<Object, Object>) x -> finalReturnValue;
	}

	private static void fallbackInvokeStringSupplierFirst(String expression, Object supplier, Object value) {
		if (supplier == null || value == null || expression == null) {
			return;
		}
		Matcher matcher = Pattern.compile("([\\w.$]+)\\.first\\s*\\(").matcher(expression);
		if (!matcher.find()) {
			return;
		}
		String className = matcher.group(1);
		Class<?> type = tryLoadClass(className);
		if (type == null) {
			return;
		}
		for (Method method : type.getMethods()) {
			if (!"first".equals(method.getName()) || !Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 2) {
				continue;
			}
			Class<?>[] params = method.getParameterTypes();
			if (isAssignable(params[0], supplier) && isAssignable(params[1], value)) {
				try {
					method.invoke(null, supplier, value);
				} catch (Exception e) {
					throw new ConveyorConfigurationException("Failed invoking " + className + ".first", e);
				}
				return;
			}
		}
	}

	private static boolean isFunctionExpression(String expression) {
		return expression != null && sanitizeExpression(expression).startsWith("function");
	}

	private static String extractFunctionReturn(String functionExpression) {
		Matcher matcher = Pattern.compile("return\\s+([^;]+)").matcher(functionExpression);
		return matcher.find() ? matcher.group(1).trim() : null;
	}

	private static Boolean fallbackBooleanReturn(String functionExpression) {
		String returnExpression = extractFunctionReturn(functionExpression);
		if (returnExpression == null) {
			return null;
		}
		String normalized = returnExpression.trim();
		if ("true".equalsIgnoreCase(normalized)) {
			return Boolean.TRUE;
		}
		if ("false".equalsIgnoreCase(normalized)) {
			return Boolean.FALSE;
		}
		return null;
	}

	private static String extractAssignment(String source, String variableName) {
		Matcher matcher = Pattern.compile("(?s)(?:var\\s+)?" + Pattern.quote(variableName) + "\\s*=\\s*([^;]+);")
				.matcher(source);
		return matcher.find() ? matcher.group(1).trim() : null;
	}

	private static Object evaluateFallbackExpression(String expression) {
		String token = sanitizeExpression(expression);
		if (token.isEmpty()) {
			return UNRESOLVED;
		}
		if (token.startsWith("function")) {
			return UNRESOLVED;
		}
		if ((token.startsWith("'") && token.endsWith("'")) || (token.startsWith("\"") && token.endsWith("\""))) {
			return unquote(token);
		}
		if ("true".equalsIgnoreCase(token) || "false".equalsIgnoreCase(token)) {
			return Boolean.valueOf(token);
		}
		if (token.matches("-?\\d+")) {
			try {
				return Integer.valueOf(token);
			} catch (NumberFormatException ignore) {
				return Long.valueOf(token);
			}
		}
		if (token.matches("-?\\d+\\.\\d+")) {
			return Double.valueOf(token);
		}
		Object instance = instantiateExpression(token);
		if (instance != UNRESOLVED) {
			return instance;
		}
		Object reference = resolveReference(token);
		return reference == UNRESOLVED ? UNRESOLVED : reference;
	}

	private static Object instantiateExpression(String expression) {
		String token = sanitizeExpression(expression);
		boolean explicitNew = false;
		if (token.startsWith("new ")) {
			explicitNew = true;
			token = token.substring(4).trim();
		} else if (!token.contains("(") || !token.endsWith(")")) {
			return UNRESOLVED;
		}
		if (explicitNew && !token.contains("(")) {
			Class<?> type = tryLoadClass(token);
			if (type == null) {
				return UNRESOLVED;
			}
			return construct(type, List.of());
		}
		int open = token.indexOf('(');
		if (open <= 0) {
			return UNRESOLVED;
		}
		String className = token.substring(0, open).trim();
		String argsSource = token.substring(open + 1, token.length() - 1);
		Class<?> type = tryLoadClass(className);
		if (type == null) {
			return UNRESOLVED;
		}
		List<String> argTokens = splitArguments(argsSource);
		List<Object> args = new ArrayList<>(argTokens.size());
		for (String argToken : argTokens) {
			Object value = evaluateFallbackExpression(argToken);
			if (value == UNRESOLVED) {
				value = unquote(argToken.trim());
			}
			args.add(value);
		}
		return construct(type, args);
	}

	private static Object resolveReference(String expression) {
		String token = sanitizeExpression(expression);
		if (token.endsWith(".class")) {
			Class<?> type = tryLoadClass(token.substring(0, token.length() - 6).trim());
			return type == null ? UNRESOLVED : type;
		}
		Class<?> directClass = tryLoadClass(token);
		if (directClass != null) {
			return directClass;
		}
		List<String> parts = splitByDot(token);
		for (int i = parts.size(); i > 0; i--) {
			String className = String.join(".", parts.subList(0, i));
			Class<?> type = tryLoadClass(className);
			if (type == null) {
				continue;
			}
			Object current = type;
			for (int idx = i; idx < parts.size(); idx++) {
				current = resolveMember(current, parts.get(idx));
				if (current == UNRESOLVED) {
					break;
				}
			}
			if (current != UNRESOLVED) {
				return current;
			}
		}
		return UNRESOLVED;
	}

	private static Object resolveMember(Object current, String memberToken) {
		if (current == null) {
			return UNRESOLVED;
		}
		String token = memberToken.trim();
		if (token.endsWith("()")) {
			String method = token.substring(0, token.length() - 2);
			return invokeNoArg(current, method);
		}
		if (!(current instanceof Class<?> type)) {
			return readField(current, token);
		}
		try {
			Field field = type.getField(token);
			if (Modifier.isStatic(field.getModifiers())) {
				return field.get(null);
			}
			Object instance = createFallbackInstance(type);
			return field.get(instance);
		} catch (NoSuchFieldException e) {
			return UNRESOLVED;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("Failed accessing field " + token + " on " + type.getName(), e);
		}
	}

	private static Object readField(Object target, String fieldName) {
		try {
			Field field = target.getClass().getField(fieldName);
			return field.get(target);
		} catch (NoSuchFieldException e) {
			return UNRESOLVED;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("Failed accessing field " + fieldName + " on " + target.getClass().getName(), e);
		}
	}

	private static Object invokeNoArg(Object target, String methodName) {
		try {
			if (target instanceof Class<?> type) {
				for (Method method : type.getMethods()) {
					if (method.getName().equals(methodName) && method.getParameterCount() == 0 && Modifier.isStatic(method.getModifiers())) {
						return method.invoke(null);
					}
				}
				Object instance = createFallbackInstance(type);
				Method method = type.getMethod(methodName);
				return method.invoke(instance);
			}
			Method method = target.getClass().getMethod(methodName);
			return method.invoke(target);
		} catch (NoSuchMethodException e) {
			return UNRESOLVED;
		} catch (Exception e) {
			throw new ConveyorConfigurationException("Failed invoking method " + methodName + " on " + target, e);
		}
	}

	private static Object construct(Class<?> type, List<Object> args) {
		Constructor<?>[] constructors = type.getConstructors();
		for (Constructor<?> constructor : constructors) {
			if (constructor.getParameterCount() != args.size()) {
				continue;
			}
			Object[] converted = convertArgs(constructor.getParameterTypes(), args);
			if (converted == null) {
				continue;
			}
			try {
				return constructor.newInstance(converted);
			} catch (Exception e) {
				throw new ConveyorConfigurationException("Failed instantiating " + type.getName(), e);
			}
		}
		throw new ConveyorConfigurationException("No matching constructor for " + type.getName() + " with " + args.size() + " args");
	}

	private static Object[] convertArgs(Class<?>[] parameterTypes, List<Object> args) {
		Object[] converted = new Object[parameterTypes.length];
		for (int i = 0; i < parameterTypes.length; i++) {
			Object arg = args.get(i);
			Object value = convertArg(parameterTypes[i], arg);
			if (value == UNRESOLVED) {
				return null;
			}
			converted[i] = value;
		}
		return converted;
	}

	private static Object convertArg(Class<?> parameterType, Object arg) {
		if (arg == null) {
			return parameterType.isPrimitive() ? UNRESOLVED : null;
		}
		if (parameterType.isInstance(arg)) {
			return arg;
		}
		if (parameterType.isPrimitive()) {
			if (!(arg instanceof Number number)) {
				return UNRESOLVED;
			}
			if (parameterType == int.class) {
				return number.intValue();
			}
			if (parameterType == long.class) {
				return number.longValue();
			}
			if (parameterType == double.class) {
				return number.doubleValue();
			}
			if (parameterType == float.class) {
				return number.floatValue();
			}
			if (parameterType == short.class) {
				return number.shortValue();
			}
			if (parameterType == byte.class) {
				return number.byteValue();
			}
			if (parameterType == boolean.class && arg instanceof Boolean b) {
				return b;
			}
			return UNRESOLVED;
		}
		if (Number.class.isAssignableFrom(parameterType) && arg instanceof Number number) {
			if (parameterType == Integer.class) {
				return number.intValue();
			}
			if (parameterType == Long.class) {
				return number.longValue();
			}
			if (parameterType == Double.class) {
				return number.doubleValue();
			}
			if (parameterType == Float.class) {
				return number.floatValue();
			}
			if (parameterType == Short.class) {
				return number.shortValue();
			}
			if (parameterType == Byte.class) {
				return number.byteValue();
			}
		}
		if (parameterType == String.class) {
			return String.valueOf(arg);
		}
		if (parameterType == Class.class && arg instanceof Class<?>) {
			return arg;
		}
		return UNRESOLVED;
	}

	private static List<String> splitArguments(String argsSource) {
		List<String> args = new ArrayList<>();
		if (argsSource == null || argsSource.isBlank()) {
			return args;
		}
		String source = argsSource.trim();
		StringBuilder current = new StringBuilder();
		int depth = 0;
		boolean single = false;
		boolean dbl = false;
		for (int i = 0; i < source.length(); i++) {
			char ch = source.charAt(i);
			if (ch == '\'' && !dbl) {
				single = !single;
			} else if (ch == '"' && !single) {
				dbl = !dbl;
			} else if (!single && !dbl) {
				if (ch == '(') {
					depth++;
				} else if (ch == ')') {
					depth = Math.max(0, depth - 1);
				} else if (ch == ',' && depth == 0) {
					args.add(current.toString().trim());
					current.setLength(0);
					continue;
				}
			}
			current.append(ch);
		}
		if (!current.isEmpty()) {
			args.add(current.toString().trim());
		}
		return args;
	}

	private static List<String> splitByDot(String expression) {
		List<String> parts = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		int depth = 0;
		for (int i = 0; i < expression.length(); i++) {
			char ch = expression.charAt(i);
			if (ch == '(') {
				depth++;
			} else if (ch == ')') {
				depth = Math.max(0, depth - 1);
			} else if (ch == '.' && depth == 0) {
				parts.add(current.toString());
				current.setLength(0);
				continue;
			}
			current.append(ch);
		}
		if (!current.isEmpty()) {
			parts.add(current.toString());
		}
		return parts;
	}

	private static String sanitizeExpression(String expression) {
		if (expression == null) {
			return "";
		}
		String token = expression.replace('\r', '\n');
		token = token.replaceAll("(?s)/\\*.*?\\*/", " ");
		token = token.replaceAll("(?m)//.*$", " ");
		token = token.trim();
		while (token.endsWith(";")) {
			token = token.substring(0, token.length() - 1).trim();
		}
		return token;
	}

	private static String unquote(String token) {
		String value = token == null ? "" : token.trim();
		if ((value.startsWith("'") && value.endsWith("'")) || (value.startsWith("\"") && value.endsWith("\""))) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}

	private static boolean isAssignable(Class<?> parameter, Object value) {
		if (value == null) {
			return !parameter.isPrimitive();
		}
		if (parameter.isInstance(value)) {
			return true;
		}
		if (!parameter.isPrimitive()) {
			return false;
		}
		return value instanceof Number || value instanceof Boolean || value instanceof Character;
	}

	private static Object createFallbackInstance(Class<?> type) {
		return fallbackInstances.computeIfAbsent(type, key -> {
			try {
				return key.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				throw new ConveyorConfigurationException("Failed to instantiate " + key.getName(), e);
			}
		});
	}

	private static Class<?> tryLoadClass(String className) {
		if (className == null || className.isBlank()) {
			return null;
		}
		ClassLoader classLoader = resolveClassLoader(Thread.currentThread().getContextClassLoader());
		try {
			if (classLoader != null) {
				return Class.forName(className, true, classLoader);
			}
			return Class.forName(className);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static Object newInstance(String className) {
		return newInstance(className, resolveClassLoader(Thread.currentThread().getContextClassLoader()));
	}

	private static Object newInstance(String className, ClassLoader classLoader) {
		try {
			Class<?> type = null;
			ClassLoader preferred = preferredClassLoader;
			try {
				type = classLoader == null ? Class.forName(className) : Class.forName(className, true, classLoader);
			} catch (Throwable first) {
				if (preferred != null && preferred != classLoader) {
					type = Class.forName(className, true, preferred);
				} else {
					throw first;
				}
			}
			return type.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new ConveyorConfigurationException("Failed to instantiate " + className, e);
		} catch (Throwable e) {
			throw new ConveyorConfigurationException("Failed to instantiate " + className, e);
		}
	}

	private static ClassLoader resolveClassLoader(ClassLoader classLoader) {
		if (classLoader != null) {
			return classLoader;
		}
		return preferredClassLoader;
	}

}

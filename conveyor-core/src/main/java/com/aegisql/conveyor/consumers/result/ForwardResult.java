package com.aegisql.conveyor.consumers.result;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aegisql.conveyor.Conveyor;
import com.aegisql.conveyor.ProductBin;
import com.aegisql.conveyor.serial.SerializablePredicate;

// TODO: Auto-generated Javadoc
/**
 * The Class ForwardResult.
 *
 * @param <K2> the generic type
 * @param <L2> the generic type
 */
public class ForwardResult<K2, L2> {

	/** The Constant LOG. */
	private final static Logger LOG = LoggerFactory.getLogger(ForwardResult.class);

	/**
	 * The Class ForwardingConsumer.
	 *
	 * @param <K2> the generic type
	 * @param <L> the generic type
	 */
	public static class ForwardingConsumer<K2, L> implements ResultConsumer<Object, Object> {

		/** The Constant serialVersionUID. */
		private static final long serialVersionUID = 1L;

		/** The from name. */
		private String fromName;
		
		/** The to conv name. */
		private String toConvName;
		
		/** The to conv. */
		private Conveyor<K2, L, ?> toConv;
		
		/** The label. */
		private L label;
		
		/** The key transformer. */
		private Function<ProductBin, K2> keyTransformer;
		
		/** The filter. */
		private SerializablePredicate<K2> filter;

		/**
		 * Instantiates a new forwarding consumer.
		 *
		 * @param label the label
		 * @param keyTransformer the key transformer
		 * @param toName the to name
		 * @param fromName the from name
		 * @param filter the filter
		 */
		public ForwardingConsumer(L label, Function<ProductBin, K2> keyTransformer, String toName, String fromName,
				SerializablePredicate<K2> filter) {
			this.fromName = fromName;
			this.toConvName = toName;
			this.label = label;
			this.keyTransformer = keyTransformer;
			this.filter = filter;
		}

		/**
		 * Instantiates a new forwarding consumer.
		 *
		 * @param label the label
		 * @param keyTransformer the key transformer
		 * @param toConv the to conv
		 * @param fromName the from name
		 * @param filter the filter
		 */
		public ForwardingConsumer(L label, Function<ProductBin, K2> keyTransformer, Conveyor<K2, L, ?> toConv,
				String fromName, SerializablePredicate<K2> filter) {
			this.fromName = fromName;
			this.toConv = toConv;
			this.toConvName = toConv.getName();
			this.label = label;
			this.keyTransformer = keyTransformer;
			this.filter = filter;
		}

		/**
		 * Gets the to conv.
		 *
		 * @return the to conv
		 */
		private Conveyor<K2, L, ?> getToConv() {
			if (toConv == null) {
				toConv = Conveyor.byName(toConvName);
			}
			return toConv;
		}

		/* (non-Javadoc)
		 * @see java.util.function.Consumer#accept(java.lang.Object)
		 */
		@Override
		public void accept(ProductBin bin) {
			LOG.debug("Forward {} from {} to {} {}", label, fromName, getToConv().getName(), bin);
			String forwardedProperty;
			if (!bin.properties.containsKey("FORWARDED")) {
				forwardedProperty = fromName;
			} else {
				forwardedProperty = (String) bin.properties.get("FORWARDED");
			}
			if (filter == null) {
				getToConv().part().label(label).id(keyTransformer.apply(bin)).value(bin.product)
						.ttl(bin.remainingDelayMsec, TimeUnit.MILLISECONDS).addProperty("FORWARDED", forwardedProperty)
						.place();
			} else {
				getToConv().part().label(label).foreach(filter).value(bin.product)
				.ttl(bin.remainingDelayMsec, TimeUnit.MILLISECONDS).addProperty("FORWARDED", forwardedProperty)
				.place();
			}
		}

		/**
		 * Gets the to conv name.
		 *
		 * @return the to conv name
		 */
		public String getToConvName() {
			return toConvName;
		}

	}

	/** The from conv. */
	private Conveyor fromConv;
	
	/** The to conv name. */
	private String toConvName;
	
	/** The to conv. */
	private Conveyor<K2, L2, ?> toConv;
	
	/** The label. */
	private L2 label;
	
	/** The key transformer. */
	private Function<ProductBin, K2> keyTransformer = bin -> (K2) bin.key;
	
	/** The filter. */
	private SerializablePredicate<K2> filter = null;

	/**
	 * Instantiates a new forward result.
	 *
	 * @param fromConv the from conv
	 */
	private ForwardResult(Conveyor fromConv) {
		this.fromConv = fromConv;
	}

	/**
	 * Instantiates a new forward result.
	 *
	 * @param fromConv the from conv
	 * @param toConv the to conv
	 * @param label the label
	 * @param keyTransformer the key transformer
	 * @param toName the to name
	 * @param filter the filter
	 */
	private ForwardResult(Conveyor fromConv, Conveyor<K2, L2, ?> toConv, L2 label,
			Function<ProductBin, K2> keyTransformer, String toName, SerializablePredicate<K2> filter) {
		this.fromConv = fromConv;
		this.toConv = toConv;
		this.label = label;
		this.keyTransformer = keyTransformer;
		this.toConvName = toName;
		this.filter = filter;
	}

	/**
	 * From.
	 *
	 * @param fromConv the from conv
	 * @return the forward result
	 */
	public static ForwardResult from(Conveyor fromConv) {
		Objects.requireNonNull(fromConv, "From Conveyor must not be null");
		return new ForwardResult<>(fromConv);
	}

	/**
	 * To.
	 *
	 * @param tc the tc
	 * @return the forward result
	 */
	public ForwardResult<K2, L2> to(Conveyor<K2, L2, ?> tc) {
		return new ForwardResult<>(fromConv, tc, label, keyTransformer, null, filter);
	}

	/**
	 * To.
	 *
	 * @param toConv the to conv
	 * @return the forward result
	 */
	public ForwardResult<K2, L2> to(String toConv) {
		return new ForwardResult<>(fromConv, null, label, keyTransformer, toConv, filter);
	}

	/**
	 * Label.
	 *
	 * @param l the l
	 * @return the forward result
	 */
	public ForwardResult<K2, L2> label(L2 l) {
		return new ForwardResult<>(fromConv, toConv, l, keyTransformer, toConvName, filter);
	}

	/**
	 * Transform key.
	 *
	 * @param t the t
	 * @return the forward result
	 */
	public ForwardResult<K2, L2> transformKey(Function<ProductBin, K2> t) {
		return new ForwardResult<>(fromConv, toConv, label, t, toConvName, filter);
	}

	/**
	 * Foreach.
	 *
	 * @return the forward result
	 */
	public ForwardResult<K2, L2> foreach() {
		return new ForwardResult<>(fromConv, toConv, label, keyTransformer, toConvName, k -> true);
	}

	/**
	 * Foreach.
	 *
	 * @param f the f
	 * @return the forward result
	 */
	public ForwardResult<K2, L2> foreach(SerializablePredicate<K2> f) {
		return new ForwardResult<>(fromConv, toConv, label, keyTransformer, toConvName, f);
	}

	/**
	 * Bind.
	 */
	public void bind() {
		if (toConv != null) {
			fromConv.resultConsumer()
					.andThen(new ForwardingConsumer<K2, L2>(label, keyTransformer, toConv, fromConv.getName(), filter))
					.set();
		} else if (toConvName != null) {
			fromConv.resultConsumer().andThen(
					new ForwardingConsumer<K2, L2>(label, keyTransformer, toConvName, fromConv.getName(), filter))
					.set();
		} else {
			throw new RuntimeException("Either toConveyor or toConveyor name must not be null");
		}
	}
}

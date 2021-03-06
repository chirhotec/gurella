package com.gurella.engine.scene.action;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Date;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IdentityMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.gurella.engine.metatype.MetaType;
import com.gurella.engine.metatype.MetaTypes;
import com.gurella.engine.metatype.Property;
import com.gurella.engine.metatype.PropertyChangeListener;
import com.gurella.engine.scene.transform.TransformComponent;
import com.gurella.engine.utils.ArrayExt;
import com.gurella.engine.utils.ImmutableArray;
import com.gurella.engine.utils.Values;

public class MetaTypeTween<T> implements Tween, Poolable {
	private static final ObjectSet<Class<?>> tweenableTypes = new ObjectSet<Class<?>>();
	private static final IdentityMap<Class<?>, Interpolator<?>> interpolatorsByType = new IdentityMap<Class<?>, Interpolator<?>>();

	static {
		registerInterpolator(byte.class, ByteInterpolator.instance);
		registerInterpolator(Byte.class, ByteInterpolator.instance);
		registerInterpolator(char.class, CharInterpolator.instance);
		registerInterpolator(Character.class, CharInterpolator.instance);
		registerInterpolator(short.class, ShortInterpolator.instance);
		registerInterpolator(Short.class, ShortInterpolator.instance);
		registerInterpolator(int.class, IntInterpolator.instance);
		registerInterpolator(Integer.class, IntInterpolator.instance);
		registerInterpolator(long.class, LongInterpolator.instance);
		registerInterpolator(Long.class, LongInterpolator.instance);
		registerInterpolator(float.class, FloatInterpolator.instance);
		registerInterpolator(Float.class, FloatInterpolator.instance);
		registerInterpolator(double.class, DoubleInterpolator.instance);
		registerInterpolator(Double.class, DoubleInterpolator.instance);
		registerInterpolator(Date.class, DateInterpolator.instance);
		registerInterpolator(boolean.class, BooleanInterpolator.instance);
		registerInterpolator(Boolean.class, BooleanInterpolator.instance);
		registerInterpolator(BigInteger.class, BigIntegerInterpolator.instance);
		registerInterpolator(BigDecimal.class, BigDecimalInterpolator.instance);
	}

	private static <T> void registerInterpolator(Class<T> type, Interpolator<T> interpolator) {
		tweenableTypes.add(type);
		interpolatorsByType.put(type, interpolator);
	}

	private T target;

	private final ArrayExt<Property<?>> properties = new ArrayExt<Property<?>>();
	private final ArrayExt<Interpolator<?>> interpolators = new ArrayExt<Interpolator<?>>();
	private final ArrayExt<Property<?>> compositeProperties = new ArrayExt<Property<?>>();
	private final ArrayExt<MetaTypeTween<?>> children = new ArrayExt<MetaTypeTween<?>>();

	private final ArrayExt<Object> startValues = new ArrayExt<Object>();
	private final ArrayExt<Object> endValues = new ArrayExt<Object>();

	public MetaTypeTween(T target, T end) {
		this(target, target, end, MetaTypes.<T> getCommonMetaType(target, end));
	}

	public MetaTypeTween(T target, T start, T end) {
		this(target, start, end, MetaTypes.<T> getCommonMetaType(target, start, end));
	}

	private MetaTypeTween(T target, T start, T end, MetaType<T> metaType) {
		this.target = target;

		ImmutableArray<Property<?>> allProperties = metaType.getProperties();
		for (int i = 0, n = allProperties.size(); i < n; i++) {
			@SuppressWarnings("unchecked")
			Property<Object> property = (Property<Object>) allProperties.get(i);
			Object startValue = property.getValue(start);
			Object endValue = property.getValue(end);
			if (startValue != null && endValue != null) {
				appendProperty(property, startValue, endValue);
			}
		}
	}

	protected void appendProperty(Property<Object> property, Object startValue, Object endValue) {
		Class<?> type = property.getType();
		if (tweenableTypes.contains(type)) {
			if (Values.isNotEqual(startValue, endValue)) {
				properties.add(property);
				interpolators.add(interpolatorsByType.get(type));
				startValues.add(startValue);
				endValues.add(endValue);
			}
		} else {
			initChild(property, startValue, endValue);
		}
	}

	private <P> void initChild(Property<P> property, P start, P end) {
		P childTarget = property.getValue(target);
		if (childTarget == null || start == null || end == null) {
			return;
		}

		MetaType<P> metaType = MetaTypes.<P> getCommonMetaType(childTarget, start, end);
		if (metaType.getProperties().size() == 0) {
			return;
		}

		compositeProperties.add(property);
		children.add(new MetaTypeTween<P>(childTarget, start, end));
	}

	@Override
	public void update(float percent) {
		for (int i = 0, n = children.size; i < n; i++) {
			MetaTypeTween<?> child = children.get(i);
			child.update(percent);
			if (target instanceof PropertyChangeListener) {
				Property<?> childProperty = compositeProperties.get(i);
				PropertyChangeListener listener = (PropertyChangeListener) target;
				Object value = childProperty.getValue(target);
				listener.propertyChanged(childProperty.getName(), value, value);
			}
		}

		for (int i = 0, n = properties.size; i < n; i++) {
			updateProperty(percent, i);
		}
	}

	private <V> void updateProperty(float percent, int index) {
		@SuppressWarnings("unchecked")
		Property<V> property = (Property<V>) properties.get(index);
		@SuppressWarnings("unchecked")
		Interpolator<V> interpolator = (Interpolator<V>) interpolators.get(index);

		@SuppressWarnings("unchecked")
		V startValue = (V) startValues.get(index);
		@SuppressWarnings("unchecked")
		V endValue = (V) endValues.get(index);
		V interpolatedValue = interpolator.interpolate(startValue, endValue, percent);

		if (target instanceof PropertyChangeListener) {
			PropertyChangeListener listener = (PropertyChangeListener) target;
			listener.propertyChanged(property.getName(), property.getValue(target), interpolatedValue);
		} else {
			property.setValue(target, interpolatedValue);
		}
	}

	public String getDiagnostics() {
		return getDiagnostics(0);
	}

	public String getDiagnostics(int level) {
		StringBuilder builder = new StringBuilder();
		for (int j = 0; j < level; j++) {
			builder.append(" ");
		}

		for (int i = 0, n = properties.size; i < n; i++) {
			Property<?> property = properties.get(i);
			builder.append(property.getName());
			builder.append(": ");
			builder.append(property.getValue(target));
			if (i < n - 1) {
				builder.append(", ");
			}
		}

		for (int i = 0, n = children.size; i < n; i++) {
			builder.append(children.get(i).getDiagnostics(level + 1));
		}

		return builder.toString();
	}

	private interface Interpolator<T> {
		T interpolate(T startValue, T endValue, float percent);
	}

	private static class ByteInterpolator implements Interpolator<Byte> {
		private static final ByteInterpolator instance = new ByteInterpolator();

		@Override
		public Byte interpolate(Byte startValue, Byte endValue, float percent) {
			byte start = startValue.byteValue();
			byte end = endValue.byteValue();
			return Byte.valueOf((byte) Math.round(start + (end - start) * percent));
		}
	}

	private static class CharInterpolator implements Interpolator<Character> {
		private static final CharInterpolator instance = new CharInterpolator();

		@Override
		public Character interpolate(Character startValue, Character endValue, float percent) {
			char start = startValue.charValue();
			char end = endValue.charValue();
			return Character.valueOf((char) Math.round(start + (end - start) * percent));
		}
	}

	private static class ShortInterpolator implements Interpolator<Short> {
		private static final ShortInterpolator instance = new ShortInterpolator();

		@Override
		public Short interpolate(Short startValue, Short endValue, float percent) {
			short start = startValue.shortValue();
			short end = endValue.shortValue();
			return Short.valueOf((short) Math.round(start + (end - start) * percent));
		}
	}

	private static class IntInterpolator implements Interpolator<Integer> {
		private static final IntInterpolator instance = new IntInterpolator();

		@Override
		public Integer interpolate(Integer startValue, Integer endValue, float percent) {
			int start = startValue.intValue();
			int end = endValue.intValue();
			return Integer.valueOf(Math.round(start + (end - start) * percent));
		}
	}

	private static class LongInterpolator implements Interpolator<Long> {
		private static final LongInterpolator instance = new LongInterpolator();

		@Override
		public Long interpolate(Long startValue, Long endValue, float percent) {
			long start = startValue.longValue();
			long end = endValue.longValue();
			return Long.valueOf(Math.round(start + (end - start) * percent));
		}
	}

	private static class FloatInterpolator implements Interpolator<Float> {
		private static final FloatInterpolator instance = new FloatInterpolator();

		@Override
		public Float interpolate(Float startValue, Float endValue, float percent) {
			float start = startValue.floatValue();
			float end = endValue.floatValue();
			return Float.valueOf((start + (end - start) * percent));
		}
	}

	private static class DoubleInterpolator implements Interpolator<Double> {
		private static final DoubleInterpolator instance = new DoubleInterpolator();

		@Override
		public Double interpolate(Double startValue, Double endValue, float percent) {
			double start = startValue.doubleValue();
			double end = endValue.doubleValue();
			return Double.valueOf((start + (end - start) * percent));
		}
	}

	private static class DateInterpolator implements Interpolator<Date> {
		private static final DateInterpolator instance = new DateInterpolator();

		@Override
		public Date interpolate(Date startValue, Date endValue, float percent) {
			long start = startValue.getTime();
			long end = endValue.getTime();
			return new Date(Math.round(start + (end - start) * percent));
		}
	}

	private static class BooleanInterpolator implements Interpolator<Boolean> {
		private static final BooleanInterpolator instance = new BooleanInterpolator();

		@Override
		public Boolean interpolate(Boolean startValue, Boolean endValue, float percent) {
			return percent > 0.5f ? endValue : startValue;
		}
	}

	private static class BigIntegerInterpolator implements Interpolator<BigInteger> {
		private static final BigIntegerInterpolator instance = new BigIntegerInterpolator();

		@Override
		public BigInteger interpolate(BigInteger startValue, BigInteger endValue, float percent) {
			return new BigDecimal(startValue)
					.add(new BigDecimal(endValue.subtract(startValue)).multiply(new BigDecimal(percent)))
					.round(MathContext.UNLIMITED).toBigInteger();
		}
	}

	private static class BigDecimalInterpolator implements Interpolator<BigDecimal> {
		private static final BigDecimalInterpolator instance = new BigDecimalInterpolator();

		@Override
		public BigDecimal interpolate(BigDecimal startValue, BigDecimal endValue, float percent) {
			return endValue.subtract(startValue).multiply(new BigDecimal(percent)).add(startValue);
		}
	}

	@Override
	public void reset() {
		target = null;
		properties.reset();
		interpolators.reset();
		startValues.reset();
		endValues.reset();
	}

	public static void main(String[] args) {
		TransformComponent tc1 = new TransformComponent();
		TransformComponent tc2 = new TransformComponent();
		tc2.setEulerRotation(100, 100, 100);
		MetaTypeTween<TransformComponent> tween = new MetaTypeTween<TransformComponent>(tc1, tc2);
		Vector3 outRotation = new Vector3();
		for (int i = 0; i < 100; i++) {
			tween.update((float) (i * 0.01 + 0.01));
			System.out.println(tc1.getEulerRotation(outRotation));
		}
	}
}

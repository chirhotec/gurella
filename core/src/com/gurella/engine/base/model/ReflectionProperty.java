package com.gurella.engine.base.model;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.reflect.ArrayReflection;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.Method;
import com.gurella.engine.base.model.ValueRange.ByteRange;
import com.gurella.engine.base.model.ValueRange.CharRange;
import com.gurella.engine.base.model.ValueRange.DoubleRange;
import com.gurella.engine.base.model.ValueRange.FloatRange;
import com.gurella.engine.base.model.ValueRange.IntegerRange;
import com.gurella.engine.base.model.ValueRange.LongRange;
import com.gurella.engine.base.model.ValueRange.ShortRange;
import com.gurella.engine.base.registry.InitializationContext;
import com.gurella.engine.base.registry.Objects;
import com.gurella.engine.base.serialization.AssetReference;
import com.gurella.engine.base.serialization.ObjectArchive;
import com.gurella.engine.base.serialization.ObjectReference;
import com.gurella.engine.base.serialization.Serialization;
import com.gurella.engine.utils.Range;
import com.gurella.engine.utils.ReflectionUtils;
import com.gurella.engine.utils.ValueUtils;

public class ReflectionProperty<T> implements Property<T> {
	private String name;
	private String descriptiveName;
	private String description;
	private String group;
	private Class<T> type;
	private Range<?> range;
	private boolean nullable;
	private T defaultValue;

	private Field field;
	private Method getter;
	private Method setter;
	private Model<?> model;

	public ReflectionProperty(Field field, Model<?> model) {
		this(field, null, null, model);
	}

	public ReflectionProperty(Field field, Method getter, Method setter, Model<?> model) {
		this.name = field.getName();
		this.field = field;
		this.model = model;
		this.field.setAccessible(true);
		@SuppressWarnings("unchecked")
		Class<T> castedType = field.getType();
		type = castedType;

		this.getter = getter;
		if (this.getter != null) {
			this.getter.setAccessible(true);
		}

		this.setter = setter;
		if (this.setter != null) {
			this.setter.setAccessible(true);
		}

		PropertyDescriptor propertyDescriptor = ReflectionUtils.getDeclaredAnnotation(field, PropertyDescriptor.class);
		if (propertyDescriptor == null) {
			descriptiveName = name;
			description = "";
			group = "";
			nullable = isDefaultNullable();
		} else {
			descriptiveName = propertyDescriptor.descriptiveName();
			if (ValueUtils.isEmpty(descriptiveName)) {
				descriptiveName = name;
			}
			description = propertyDescriptor.description();
			group = propertyDescriptor.group();
			nullable = isDefaultNullable() ? propertyDescriptor.nullable() : false;
		}
		
		range = extractRange(ReflectionUtils.getDeclaredAnnotation(field, ValueRange.class));
		defaultValue = getValue(model.getDefaultValue());
	}

	private boolean isDefaultNullable() {
		return !(type.isPrimitive() || (field != null && field.isFinal()));
	}

	private Range<?> extractRange(ValueRange valueRange) {
		if (valueRange == null) {
			return null;
		}

		if (Integer.class == type || int.class == type) {
			IntegerRange integerRange = valueRange.integerRange();
			return integerRange == null ? null
					: new Range<Integer>(Integer.valueOf(integerRange.min()), Integer.valueOf(integerRange.max()));
		} else if (Float.class == type || float.class == type) {
			FloatRange floatRange = valueRange.floatRange();
			return floatRange == null ? null
					: new Range<Float>(Float.valueOf(floatRange.min()), Float.valueOf(floatRange.max()));
		} else if (Long.class == type || long.class == type) {
			LongRange longRange = valueRange.longRange();
			return longRange == null ? null
					: new Range<Long>(Long.valueOf(longRange.min()), Long.valueOf(longRange.max()));
		} else if (Double.class == type || double.class == type) {
			DoubleRange doubleRange = valueRange.doubleRange();
			return doubleRange == null ? null
					: new Range<Double>(Double.valueOf(doubleRange.min()), Double.valueOf(doubleRange.max()));
		} else if (Short.class == type || short.class == type) {
			ShortRange shortRange = valueRange.shortRange();
			return shortRange == null ? null
					: new Range<Short>(Short.valueOf(shortRange.min()), Short.valueOf(shortRange.max()));
		} else if (Byte.class == type || byte.class == type) {
			ByteRange byteRange = valueRange.byteRange();
			return byteRange == null ? null
					: new Range<Byte>(Byte.valueOf(byteRange.min()), Byte.valueOf(byteRange.max()));
		} else if (Character.class == type || char.class == type) {
			CharRange charRange = valueRange.charRange();
			return charRange == null ? null
					: new Range<Character>(Character.valueOf(charRange.min()), Character.valueOf(charRange.max()));
		} else {
			return null;
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<T> getType() {
		return type;
	}

	@Override
	public Model<?> getModel() {
		return model;
	}

	@Override
	public Range<?> getRange() {
		return range;
	}

	@Override
	public boolean isNullable() {
		return nullable;
	}

	@Override
	public String getDescriptiveName() {
		return descriptiveName;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getGroup() {
		return group;
	}
	
	@Override
	public T getDefaultValue() {
		return defaultValue;
	}
	
	@Override
	public Property<T> copy(Model<?> newModel) {
		return new ReflectionProperty<T>(field, getter, setter, newModel);
	}

	@Override
	public void init(InitializationContext<?> context) {
		Object initializingObject = context.initializingObject;
		JsonValue serializedValue = context.serializedValue == null ? null : context.serializedValue.get(name);

		if (serializedValue == null) {
			Object template = context.template;
			if (template == null) {
				return;
			}

			T value = getValue(template);
			if (ValueUtils.isEqual(value, defaultValue)) {
				return;
			}

			if (value == null) {
				setValue(initializingObject, null);
			} else if (value.getClass().isArray()) {
				int length = ArrayReflection.getLength(template);
				@SuppressWarnings("unchecked")
				T array = (T) ArrayReflection.newInstance(type, length);
				for (int i = 0; i < length; i++) {
					Object item = ArrayReflection.get(template, i);
					ArrayReflection.set(array, i, Objects.copyValue(item, context));
				}
				setValue(context.initializingObject, array);
			} else {
				setValue(initializingObject, field.isFinal() ? value : Objects.copyValue(value, context));
			}
		} else {
			if (serializedValue.isNull()) {
				setValue(initializingObject, null);
				return;
			}

			Class<T> resolvedType = Serialization.resolveObjectType(type, serializedValue);
			if (resolvedType.isArray()) {
				@SuppressWarnings("unchecked")
				T array = (T) ArrayReflection.newInstance(type, serializedValue.size);
				Class<?> componentType = resolvedType.getComponentType();

				int i = 0;
				for (JsonValue item = serializedValue.child; item != null; item = item.next) {
					if (serializedValue.isNull()) {
						ArrayReflection.set(array, i++, null);
					} else {
						Class<?> resolvedItemType = Serialization.resolveObjectType(componentType, item);
						if (Serialization.isSimpleTypeOrPrimitive(resolvedItemType)) {
							ArrayReflection.set(array, i++, context.json.readValue(resolvedItemType, null, item));
						} else if (ClassReflection.isAssignableFrom(AssetReference.class, resolvedItemType)) {
							AssetReference assetReference = context.json.readValue(AssetReference.class, null, item);
							ArrayReflection.set(array, i++, context.<T> getAsset(assetReference));
						} else if (ClassReflection.isAssignableFrom(ObjectReference.class, resolvedItemType)) {
							ObjectReference objectReference = context.json.readValue(ObjectReference.class, null, item);
							@SuppressWarnings("unchecked")
							T instance = (T) context.getInstance(objectReference.getId());
							ArrayReflection.set(array, i++, instance);
						} else {
							ArrayReflection.set(array, i++,
									Objects.deserialize(serializedValue, resolvedItemType, context));
						}
					}
				}

				setValue(context.initializingObject, array);
			} else {
				if (Serialization.isSimpleTypeOrPrimitive(resolvedType)) {
					setValue(initializingObject, context.json.readValue(resolvedType, null, serializedValue));
				} else if (ClassReflection.isAssignableFrom(AssetReference.class, resolvedType)) {
					AssetReference assetReference = context.json.readValue(AssetReference.class, null, serializedValue);
					setValue(initializingObject, context.<T> getAsset(assetReference));
				} else if (ClassReflection.isAssignableFrom(ObjectReference.class, resolvedType)) {
					ObjectReference objectReference = context.json.readValue(ObjectReference.class, null,
							serializedValue);
					@SuppressWarnings("unchecked")
					T instance = (T) context.getInstance(objectReference.getId());
					setValue(initializingObject, instance);
				} else if (field.isFinal()) {
					Objects.initProperties(getValue(initializingObject), serializedValue, context);
				} else {
					setValue(initializingObject, Objects.deserialize(serializedValue, resolvedType, context));
				}
			}
		}
	}

	@Override
	public T getValue(Object object) {
		if (object == null) {
			return null;
		} else if (getter == null) {
			return ReflectionUtils.getFieldValue(field, object);
		} else {
			return ReflectionUtils.invokeMethod(getter, object);
		}
	}

	@Override
	public void setValue(Object object, T value) {
		if (setter != null) {
			ReflectionUtils.invokeMethod(setter, object, value);
		} else if (field.isFinal()) {
			Object fieldValue = ReflectionUtils.getFieldValue(field, object);
			Objects.copyProperties(value, fieldValue);
		} else {
			ReflectionUtils.setFieldValue(field, object, value);
		}
	}

	@Override
	public void serialize(Object object, ObjectArchive archive) {
		T value = getValue(object);
		if (!ValueUtils.isEqual(value, defaultValue)) {
			archive.writeValue(name, value, type);
		}
	}
}

package com.gurella.engine.metatype;

import static com.badlogic.gdx.utils.reflect.ClassReflection.isAssignableFrom;
import static com.gurella.engine.metatype.MetaTypes.getPrefix;
import static com.gurella.engine.metatype.MetaTypes.isPrefix;
import static com.gurella.engine.metatype.MetaTypes.setPrefix;

import java.lang.annotation.Annotation;

import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.reflect.ArrayReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.Method;
import com.gurella.engine.asset2.Assets;
import com.gurella.engine.editor.property.PropertyEditorDescriptor;
import com.gurella.engine.serialization.Input;
import com.gurella.engine.serialization.Output;
import com.gurella.engine.utils.ImmutableArray;
import com.gurella.engine.utils.Range;
import com.gurella.engine.utils.Reflection;
import com.gurella.engine.utils.Values;

public class ReflectionProperty<T> implements Property<T> {
	private Class<?> declaringClass;
	private String name;
	private boolean editable;
	private Class<T> type;
	private Range<?> range;
	private boolean asset;
	private boolean nullable;
	private boolean finalProperty;
	private boolean copyable;
	private boolean flatSerialization;
	private T defaultValue;

	private Field field;
	private Method getter;
	private Method setter;

	public static <T> ReflectionProperty<T> newInstance(Class<?> owner, String name, MetaType<?> metaType) {
		Field field = Reflection.getDeclaredFieldSilently(owner, name);
		String upperCaseName = name.substring(0, 1).toUpperCase() + name.substring(1);
		Class<?> fieldType = field == null ? null : field.getType();

		if (fieldType == null) {
			Method boolGetter = Reflection.getDeclaredMethodSilently(owner, isPrefix + upperCaseName);
			boolGetter = boolGetter == null || !isValidBeanMethod(boolGetter)
					|| boolGetter.getReturnType() != boolean.class ? null : boolGetter;
			Method getter = Reflection.getDeclaredMethodSilently(owner, getPrefix + upperCaseName);
			getter = getter == null || !isValidBeanMethod(getter) ? null : getter;
			if (boolGetter == null && getter == null) {
				throw new GdxRuntimeException(name + " is not a property of " + owner.getSimpleName());
			}

			// TODO not finished
			Method boolSetter = Reflection.getDeclaredMethodSilently(owner, setPrefix + upperCaseName, boolean.class);
			Method setter = getter == null ? null
					: Reflection.getDeclaredMethodSilently(owner, setPrefix + upperCaseName, getter.getReturnType());
			setter = setter == null || !isValidBeanMethod(getter) ? null : getter;
			return new ReflectionProperty<T>(owner, name, field, getter, setter, metaType);
		} else {
			String prefix = boolean.class.equals(fieldType) ? isPrefix : getPrefix;
			Method getter = Reflection.getDeclaredMethodSilently(owner, prefix + upperCaseName);
			getter = getter == null || !isValidBeanMethod(getter) ? null : getter;
			if (getter == null) {
				return new ReflectionProperty<T>(owner, field, metaType);
			}

			Method setter = Reflection.getDeclaredMethodSilently(owner, setPrefix + upperCaseName, fieldType);
			setter = setter == null || !isValidBeanMethod(getter) ? null : getter;
			if (setter == null) {
				return new ReflectionProperty<T>(owner, field, metaType);
			} else {
				return new ReflectionProperty<T>(owner, name, field, getter, setter, metaType);
			}
		}
	}

	private static boolean isValidBeanMethod(Method method) {
		return !method.isPrivate() || method.getDeclaredAnnotation(PropertyDescriptor.class) != null;
	}

	public ReflectionProperty(Class<?> declaringClass, Field field, MetaType<?> metaType) {
		this(declaringClass, field.getName(), field, null, null, metaType);
	}

	public ReflectionProperty(Class<?> declaringClass, String name, Field field, Method getter, Method setter,
			MetaType<?> metaType) {
		this.declaringClass = declaringClass;
		this.name = name;

		this.field = field;
		if (this.field == null) {
			@SuppressWarnings("unchecked")
			Class<T> castedType = getter.getReturnType();
			type = castedType;
		} else {
			this.field.setAccessible(true);
			this.name = field.getName();
			finalProperty = field.isFinal();
			@SuppressWarnings("unchecked")
			Class<T> castedType = field.getType();
			type = castedType;
		}

		this.getter = getter;
		if (this.getter != null) {
			this.getter.setAccessible(true);
		}

		this.setter = setter;
		if (this.setter != null) {
			this.setter.setAccessible(true);
		}

		PropertyDescriptor propertyDescriptor = findAnnotation(PropertyDescriptor.class);
		if (propertyDescriptor == null) {
			nullable = isDefaultNullable();
			copyable = true;
			flatSerialization = isDefaultFlatSerialization();
		} else {
			nullable = isDefaultNullable() ? propertyDescriptor.nullable() : false;
			copyable = propertyDescriptor.copyable();
			flatSerialization = isDefaultFlatSerialization() ? true : propertyDescriptor.flatSerialization();
		}

		AssetProperty assetProperty = findAnnotation(AssetProperty.class);
		if (assetProperty == null) {
			asset = Assets.isAssetType(type);
		} else {
			asset = assetProperty.value() && Assets.isAssetType(type);
		}

		range = Range.valueOf(findAnnotation(ValueRange.class), type);
		PropertyEditorDescriptor editorDescriptor = findAnnotation(PropertyEditorDescriptor.class);
		if (editorDescriptor == null) {
			editable = true;
		} else {
			editable = editorDescriptor.editable();
		}

		defaultValue = getValue(DefaultInstances.getDefault(metaType.getType()));
	}

	private <A extends Annotation> A findAnnotation(Class<A> type) {
		if (field != null) {
			A annotation = Reflection.getDeclaredAnnotation(field, type);
			if (annotation != null) {
				return annotation;
			}
		}

		if (getter != null) {
			A annotation = Reflection.getDeclaredAnnotation(getter, type);
			if (annotation != null) {
				return annotation;
			}
		}

		if (setter != null) {
			A annotation = Reflection.getDeclaredAnnotation(setter, type);
			if (annotation != null) {
				return annotation;
			}
		}

		return null;
	}

	private boolean isDefaultNullable() {
		return !(type.isPrimitive() || (field != null && field.isFinal()));
	}

	private boolean isDefaultFlatSerialization() {
		return (type.isPrimitive() || (field != null && field.isFinal() && getter == null));
	}

	public Class<?> getDeclaringClass() {
		return declaringClass;
	}

	public Field getField() {
		return field;
	}

	public Method getGetter() {
		return getter;
	}

	public Method getSetter() {
		return setter;
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
	public Range<?> getRange() {
		return range;
	}

	@Override
	public boolean isAsset() {
		return asset;
	}

	@Override
	public boolean isNullable() {
		return nullable;
	}

	@Override
	public boolean isFinal() {
		return finalProperty;
	}

	@Override
	public boolean isCopyable() {
		return copyable;
	}

	@Override
	public boolean isFlatSerialization() {
		return flatSerialization;
	}

	@Override
	public boolean isEditable() {
		return editable;
	}

	@Override
	public Property<T> newInstance(MetaType<?> owner) {
		T overriden = getValue(DefaultInstances.getDefault(owner.getType()));
		return Values.isEqual(defaultValue, overriden, true) ? this
				: new ReflectionProperty<T>(declaringClass, name, field, getter, setter, owner);
	}

	@Override
	public T getValue(Object object) {
		if (object == null) {
			return null;
		} else if (getter == null) {
			return Reflection.getFieldValue(field, object);
		} else {
			return Reflection.invokeMethod(getter, object);
		}
	}

	@Override
	public void setValue(Object object, T value) {
		if (setter != null) {
			Reflection.invokeMethod(setter, object, value);
		} else if (field.isFinal()) {
			T fieldValue = Reflection.getFieldValue(field, object);
			updateFinalValueProperties(value, fieldValue);
		} else {
			Reflection.setFieldValue(field, object, value);
		}
	}

	private static <T> void updateFinalValueProperties(T source, T target) {
		if (source == null || target == null) {
			return;
		}

		Class<? extends Object> sourceType = source.getClass();
		Class<? extends Object> targetType = target.getClass();
		if (targetType.isArray() && isAssignableFrom(targetType, sourceType)) {
			int length = Math.min(ArrayReflection.getLength(source), ArrayReflection.getLength(target));
			System.arraycopy(source, 0, target, 0, length);
		} else {
			MetaType<Object> metaType = MetaTypes.getCommonMetaType(source, target);
			ImmutableArray<Property<?>> properties = metaType.getProperties();
			for (int i = 0; i < properties.size(); i++) {
				@SuppressWarnings("unchecked")
				Property<Object> property = (Property<Object>) properties.get(i);
				Object value = property.getValue(source);
				property.setValue(target, value);
			}
		}
	}

	@Override
	public void serialize(Object object, Object template, Output output) {
		T value = getValue(object);
		Object templateValue = template == null ? defaultValue : getValue(template);

		if (!Values.isEqual(value, templateValue)) {
			if (value == null) {
				output.writeNullProperty(name);
			} else {
				output.writeObjectProperty(name, type, templateValue, value, flatSerialization);
			}
		}
	}

	@Override
	public void deserialize(Object object, Object template, Input input) {
		if (input.hasProperty(name)) {
			T value = getValue(object);
			T templateValue = getValue(template);
			Object templatePropertyValue = template == null ? value : templateValue;
			setValue(object, input.readObjectProperty(name, type, templatePropertyValue));
		} else if (template != null) {
			T value = getValue(object);
			T templateValue = getValue(template);
			if (!Values.isEqual(value, templateValue)) {
				setValue(object, field.isFinal() ? templateValue : input.copyObject(templateValue));
			}
		}
	}

	@Override
	public void copy(Object original, Object duplicate, CopyContext context) {
		//TODO check if value is external asset (different file) and inject original
		setValue(duplicate, context.copy(getValue(original)));
	}
}

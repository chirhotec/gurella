package com.gurella.engine.base.model;

import static com.gurella.engine.base.model.Models.getPrefix;
import static com.gurella.engine.base.model.Models.isPrefix;
import static com.gurella.engine.base.model.Models.setPrefix;

import java.lang.annotation.Annotation;

import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.Method;
import com.gurella.engine.base.serialization.Input;
import com.gurella.engine.base.serialization.Output;
import com.gurella.engine.editor.property.PropertyEditorDescriptor;
import com.gurella.engine.pool.PoolService;
import com.gurella.engine.utils.Range;
import com.gurella.engine.utils.Reflection;
import com.gurella.engine.utils.Values;

public class ReflectionProperty<T> implements Property<T> {
	private Class<?> declaringClass;
	private String name;
	private boolean editable;
	private Class<T> type;
	private Range<?> range;
	private boolean nullable;
	private boolean finalProperty;
	private boolean copyable;
	private boolean flatSerialization;
	private T defaultValue;

	private Field field;
	private Method getter;
	private Method setter;

	public static <T> ReflectionProperty<T> newInstance(Class<?> owner, String name, Model<?> model) {
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

			//TODO not finished
			Method boolSetter = Reflection.getDeclaredMethodSilently(owner, setPrefix + upperCaseName, boolean.class);
			Method setter = getter == null ? null
					: Reflection.getDeclaredMethodSilently(owner, setPrefix + upperCaseName, getter.getReturnType());
			setter = setter == null || !isValidBeanMethod(getter) ? null : getter;
			return new ReflectionProperty<T>(owner, name, field, getter, setter, model);
		} else {
			String prefix = boolean.class.equals(fieldType) ? isPrefix : getPrefix;
			Method getter = Reflection.getDeclaredMethodSilently(owner, prefix + upperCaseName);
			getter = getter == null || !isValidBeanMethod(getter) ? null : getter;
			if (getter == null) {
				return new ReflectionProperty<T>(owner, field, model);
			}

			Method setter = Reflection.getDeclaredMethodSilently(owner, setPrefix + upperCaseName, fieldType);
			setter = setter == null || !isValidBeanMethod(getter) ? null : getter;
			if (setter == null) {
				return new ReflectionProperty<T>(owner, field, model);
			} else {
				return new ReflectionProperty<T>(owner, name, field, getter, setter, model);
			}
		}
	}

	private static boolean isValidBeanMethod(Method method) {
		return !method.isPrivate() || method.getDeclaredAnnotation(PropertyDescriptor.class) != null;
	}

	public ReflectionProperty(Class<?> declaringClass, Field field, Model<?> model) {
		this(declaringClass, field.getName(), field, null, null, model);
	}

	public ReflectionProperty(Class<?> declaringClass, String name, Field field, Method getter, Method setter,
			Model<?> model) {
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

		range = Range.valueOf(findAnnotation(ValueRange.class), type);
		PropertyEditorDescriptor editorDescriptor = findAnnotation(PropertyEditorDescriptor.class);
		if (editorDescriptor == null) {
			editable = true;
		} else {
			editable = editorDescriptor.editable();
		}

		defaultValue = getValue(ModelDefaults.getDefault(model.getType()));
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
	public Property<T> newInstance(Model<?> newModel) {
		T overriden = getValue(ModelDefaults.getDefault(newModel.getType()));
		return Values.isEqual(defaultValue, overriden, true) ? this
				: new ReflectionProperty<T>(declaringClass, name, field, getter, setter, newModel);
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
			Object fieldValue = Reflection.getFieldValue(field, object);
			CopyContext context = PoolService.obtain(CopyContext.class);
			context.copyProperties(value, fieldValue);
			PoolService.free(context);
		} else {
			Reflection.setFieldValue(field, object, value);
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

			if (Values.isEqual(value, templateValue)) {
				return;
			} else {
				setValue(object, field.isFinal() ? templateValue : input.copyObject(templateValue));
			}
		}
	}

	@Override
	public void copy(Object original, Object duplicate, CopyContext context) {
		setValue(duplicate, context.copy(getValue(original)));
	}
}

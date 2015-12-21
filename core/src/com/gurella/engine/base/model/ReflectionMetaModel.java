package com.gurella.engine.base.model;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.gurella.engine.utils.ArrayExt;
import com.gurella.engine.utils.ImmutableArray;
import com.gurella.engine.utils.ReflectionUtils;
import com.gurella.engine.utils.ValueUtils;

public class ReflectionMetaModel<T> extends AbstractMetaModel<T> {
	private static final ObjectMap<Class<?>, ArrayExt<MetaProperty<?>>> declaredPropertiesByClass = new ObjectMap<Class<?>, ArrayExt<MetaProperty<?>>>();
	private static final ObjectMap<Class<?>, ArrayExt<MetaProperty<?>>> propertiesByClass = new ObjectMap<Class<?>, ArrayExt<MetaProperty<?>>>();
	private static final ObjectMap<Class<?>, ReflectionMetaModel<?>> instancesByClass = new ObjectMap<Class<?>, ReflectionMetaModel<?>>();

	private String name;
	private ArrayExt<MetaProperty<?>> properties;

	public static <T> ReflectionMetaModel<T> getInstance(Class<T> resourceType) {
		synchronized (instancesByClass) {
			@SuppressWarnings("unchecked")
			ReflectionMetaModel<T> instance = (ReflectionMetaModel<T>) instancesByClass.get(resourceType);
			if (instance == null) {
				instance = new ReflectionMetaModel<T>(resourceType);
			}
			return instance;
		}
	}

	public ReflectionMetaModel(Class<T> type) {
		super(type);
		instancesByClass.put(type, this);
		properties = findProperties();
		name = resolveName();
	}

	private String resolveName() {
		Model resourceAnnotation = ReflectionUtils.getAnnotation(type, Model.class);
		if (resourceAnnotation == null) {
			return type.getSimpleName();
		} else {
			String descriptiveName = resourceAnnotation.descriptiveName();
			return ValueUtils.isEmpty(descriptiveName) ? type.getSimpleName() : descriptiveName;
		}
	}

	private ArrayExt<MetaProperty<?>> findProperties() {
		ArrayExt<MetaProperty<?>> cachedProperties = propertiesByClass.get(type);
		if (cachedProperties == null) {
			cachedProperties = new ArrayExt<MetaProperty<?>>();
			propertiesByClass.put(type, cachedProperties);

			Array<Class<?>> classHierarchy = getClassHierarchy();
			for (Class<?> clazz : classHierarchy) {
				appendProperties(clazz, cachedProperties);
			}

			PropertyOverrides overrides = ReflectionUtils.getDeclaredAnnotation(type, PropertyOverrides.class);
			if (overrides != null) {
				boolean updateResourceOnInit = overrides.updateResourceOnInit();
				PropertyValue[] values = overrides.values();
				for (int i = 0; i < values.length; i++) {
					PropertyValue propertyValue = values[i];
					MetaProperty<?> property = findProperty(propertyValue.name(), cachedProperties);
					if (property instanceof ReflectionMetaProperty) {
						ReflectionMetaProperty<?> reflectionProperty = (ReflectionMetaProperty<?>) property;
						if (!isDeclaredProperty(property)) {
							int index = cachedProperties.indexOf(reflectionProperty, true);
							cachedProperties.set(index, reflectionProperty.copy(propertyValue, updateResourceOnInit));
						}
					}
				}
			}
		}
		return cachedProperties;
	}

	private Array<Class<?>> getClassHierarchy() {
		Array<Class<?>> classHierarchy = new Array<Class<?>>();
		Class<?> tempClass = type;
		while (!tempClass.isInterface() && tempClass != Object.class) {
			classHierarchy.add(tempClass);
			tempClass = tempClass.getSuperclass();
		}
		classHierarchy.reverse();
		return classHierarchy;
	}

	private static void appendProperties(Class<?> resourceType, ArrayExt<MetaProperty<?>> properties) {
		synchronized (declaredPropertiesByClass) {
			ArrayExt<MetaProperty<?>> cachedProperties = declaredPropertiesByClass.get(resourceType);
			if (cachedProperties == null) {
				cachedProperties = new ArrayExt<MetaProperty<?>>();
				declaredPropertiesByClass.put(resourceType, cachedProperties);

				for (Field field : ClassReflection.getDeclaredFields(resourceType)) {
					if (!isIgnoredField(resourceType, field)) {
						MetaProperty<?> property = getModelProperty(field);
						if (property != null) {
							cachedProperties.add(property);
						}
					}
				}
			}
			properties.addAll(cachedProperties);
		}
	}

	private static boolean isIgnoredField(Class<?> resourceType, Field field) {
		return field.isStatic() || field.isTransient() || field.getDeclaredAnnotation(TransientProperty.class) != null
				|| isIgnoredFinalField(resourceType, field);
	}

	private static boolean isIgnoredFinalField(Class<?> resourceType, Field field) {
		if (!field.isFinal()) {
			return false;
		}

		Class<?> fieldType = field.getType();
		if (fieldType.isPrimitive() || fieldType.isArray()) {
			return true;
		}

		if (ReflectionUtils.getDeclaredAnnotation(field, Property.class) == null) {
			return true;
		}

		if (resourceType.equals(fieldType)) {
			return false;
		}

		ImmutableArray<MetaProperty<?>> modelProperties = ModelUtils.getModel(fieldType).getProperties();
		return modelProperties == null ? false : modelProperties.size() == 0;
	}

	private boolean isDeclaredProperty(MetaProperty<?> property) {
		ArrayExt<MetaProperty<?>> cachedProperties = declaredPropertiesByClass.get(type);
		return cachedProperties != null && cachedProperties.contains(property, true);
	}

	private static MetaProperty<?> findProperty(String propertyName, ArrayExt<MetaProperty<?>> cachedProperties) {
		for (int i = 0; i < cachedProperties.size; i++) {
			MetaProperty<?> property = cachedProperties.get(i);
			if (propertyName.equals(property.getName())) {
				return property;
			}
		}
		return null;
	}

	private static MetaProperty<?> getModelProperty(Field field) {
		Property property = ReflectionUtils.getDeclaredAnnotation(field, Property.class);
		if (property == null) {
			return createReflectionProperty(field, false);
		} else {
			@SuppressWarnings("unchecked")
			Class<? extends MetaProperty<?>> propertyType = (Class<? extends MetaProperty<?>>) property.model();
			return ReflectionMetaProperty.class.equals(propertyType) ? createReflectionProperty(field, true)
					: createAnnotationProperty(propertyType);
		}
	}

	private static MetaProperty<?> createAnnotationProperty(Class<? extends MetaProperty<?>> propertyType) {
		MetaProperty<?> property = getPropertyFromFactoryMethod(propertyType);
		return property == null ? ReflectionUtils.newInstance(propertyType) : property;
	}

	private static MetaProperty<?> getPropertyFromFactoryMethod(Class<? extends MetaProperty<?>> propertyType) {
		//TODO should be annotation based @FactoryMethod
		Method factoryMethod = ReflectionUtils.getDeclaredMethodSilently(propertyType, "getInstance");
		if (factoryMethod != null && factoryMethod.isPublic() && factoryMethod.getReturnType() == propertyType
				&& factoryMethod.isStatic()) {
			try {
				return (MetaProperty<?>) factoryMethod.invoke(null);
			} catch (ReflectionException e) {
				return null;
			}
		} else {
			return null;
		}
	}

	private static ReflectionMetaProperty<?> createReflectionProperty(Field field, boolean forced) {
		ReflectionMetaProperty<?> propertyModel = createBeanPropertyModel(field, forced);
		if (propertyModel != null) {
			return propertyModel;
		} else if (forced || field.isPublic()) {
			return new ReflectionMetaProperty<Object>(field);
		} else {
			return null;
		}
	}

	private static ReflectionMetaProperty<?> createBeanPropertyModel(Field field, boolean forced) {
		String name = field.getName();
		String upperCaseName = name.substring(0, 1).toUpperCase() + name.substring(1);
		Class<?> fieldType = field.getType();
		Class<?> resourceType = field.getDeclaringClass();

		Method getter = getPropertyGetter(resourceType, upperCaseName, fieldType, forced);
		if (getter == null) {
			return null;
		}

		Method setter = getPropertySetter(resourceType, upperCaseName, fieldType, forced);
		if (setter == null) {
			return null;
		}

		return new ReflectionMetaProperty<Object>(field, getter, setter);
	}

	private static Method getPropertyGetter(Class<?> resourceClass, String upperCaseName, Class<?> fieldType,
			boolean forced) {
		String prefix = Boolean.TYPE.equals(fieldType) ? "is" : "get";
		Method getter = ReflectionUtils.getDeclaredMethodSilently(resourceClass, prefix + upperCaseName);
		if (getter == null || (!forced && !getter.isPublic())) {
			return null;
		} else {
			return fieldType.equals(getter.getReturnType()) ? getter : null;
		}
	}

	private static Method getPropertySetter(Class<?> resourceClass, String upperCaseName, Class<?> fieldType,
			boolean forced) {
		Method setter = ReflectionUtils.getDeclaredMethodSilently(resourceClass, "set" + upperCaseName, fieldType);
		if (setter == null || (!forced && !setter.isPublic())) {
			return null;
		} else {
			return Void.TYPE.equals(setter.getReturnType()) ? setter : null;
		}
	}

	@Override
	public String getDescriptiveName() {
		return name;
	}

	@Override
	public ImmutableArray<MetaProperty<?>> getProperties() {
		return properties.immutable();
	}
}

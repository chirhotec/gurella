package com.gurella.engine.base.model;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.badlogic.gdx.utils.JsonValue;
import com.gurella.engine.base.registry.InitializationContext;
import com.gurella.engine.base.registry.Objects;
import com.gurella.engine.base.serialization.Archive;
import com.gurella.engine.base.serialization.Serialization;
import com.gurella.engine.utils.ArrayExt;
import com.gurella.engine.utils.ImmutableArray;
import com.gurella.engine.utils.Range;
import com.gurella.engine.utils.ReflectionUtils;

public class MapModel<T extends Map<?, ?>> implements Model<T> {
	private Class<T> type;
	private ArrayExt<Property<?>> properties;

	public MapModel(Class<T> type) {
		this.type = type;
		properties = new ArrayExt<Property<?>>();
		properties.add(new MapEntriesProperty(this));
	}

	@Override
	public String getName() {
		return type.getName();
	}

	@Override
	public Class<T> getType() {
		return type;
	}

	@Override
	public T createInstance(InitializationContext context) {
		JsonValue serializedValue = context.serializedValue();
		if (serializedValue == null) {
			T template = context.template();
			if (template == null) {
				return null;
			}

			@SuppressWarnings("unchecked")
			T instance = (T) ReflectionUtils.newInstance(template.getClass());
			return instance;
		} else {
			if (serializedValue.isNull()) {
				return null;
			}

			Class<T> resolvedType = Serialization.resolveObjectType(type, serializedValue);
			return ReflectionUtils.newInstance(resolvedType);
		}
	}

	@Override
	public void initInstance(InitializationContext context) {
		Map<?, ?> initializingObject = context.initializingObject();
		if (initializingObject != null) {
			properties.get(0).init(context);
		}
	}

	@Override
	public ImmutableArray<Property<?>> getProperties() {
		return properties.immutable();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <P> Property<P> getProperty(String name) {
		if (MapEntriesProperty.name.equals(name)) {
			return (Property<P>) properties.get(0);
		} else {
			return null;
		}
	}

	@Override
	public void serialize(T object, Class<?> knownType, Archive archive) {
		if (object == null) {
			archive.writeValue(null, null);
		} else {
			archive.writeObjectStart(object, knownType);
			properties.get(0).serialize(object, archive);
			archive.writeObjectEnd();
		}
	}

	private static class MapEntriesProperty implements Property<Set<Entry<?, ?>>> {
		private static final String name = "entries";

		private Model<?> model;

		public MapEntriesProperty(Model<?> model) {
			this.model = model;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Class<Set<Entry<?, ?>>> getType() {
			return (Class) Set.class;
		}

		@Override
		public Model<?> getModel() {
			return model;
		}

		@Override
		public Property<Set<Entry<?, ?>>> copy(Model<?> model) {
			return new MapEntriesProperty(model);
		}

		@Override
		public Range<?> getRange() {
			return null;
		}

		@Override
		public boolean isNullable() {
			return false;
		}

		@Override
		public String getDescriptiveName() {
			return name;
		}

		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public String getGroup() {
			return null;
		}

		@Override
		public void init(InitializationContext context) {
			@SuppressWarnings("unchecked")
			Map<Object, Object> map = (Map<Object, Object>) context.initializingObject();
			if (map == null) {
				return;
			}

			JsonValue serializedObject = context.serializedValue();
			JsonValue serializedValue = serializedObject == null ? null : serializedObject.get(name);
			if (serializedValue == null) {
				@SuppressWarnings("unchecked")
				Map<Object, Object> template = (Map<Object, Object>) context.template();
				if (template == null) {
					return;
				}

				for (Entry<Object, Object> entry : template.entrySet()) {
					map.put(Objects.copyValue(entry.getKey(), context), Objects.copyValue(entry.getValue(), context));
				}
			} else {
				for (JsonValue item = serializedValue.child; item != null; item = item.next) {
					JsonValue keyValue = item.child;
					Object key = Objects.deserialize(keyValue, Object.class, context);
					Object value = Objects.deserialize(keyValue.next, Object.class, context);
					map.put(key, value);
				}
			}
		}

		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Set<Entry<?, ?>> getValue(Object object) {
			return ((Map) object).entrySet();
		}

		@Override
		public void setValue(Object object, Set<Entry<?, ?>> value) {
			@SuppressWarnings("unchecked")
			Map<Object, Object> map = (Map<Object, Object>) object;
			map.clear();
			for (Entry<?, ?> entry : value) {
				map.put(entry.getKey(), entry.getValue());
			}
		}

		@Override
		public void serialize(Object object, Archive archive) {
			@SuppressWarnings("unchecked")
			Map<Object, Object> map = (Map<Object, Object>) object;
			if (map.isEmpty()) {
				return;
			}

			archive.writeArrayStart(name);
			for (Entry<?, ?> entry : map.entrySet()) {
				archive.writeArrayStart();
				archive.writeValue(entry.getKey(), Object.class);
				archive.writeValue(entry.getValue(), Object.class);
				archive.writeArrayEnd();
			}
			archive.writeArrayEnd();
		}
	}
}

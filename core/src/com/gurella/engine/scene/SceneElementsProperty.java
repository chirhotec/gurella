package com.gurella.engine.scene;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.gurella.engine.managedobject.ManagedObject;
import com.gurella.engine.metatype.CopyContext;
import com.gurella.engine.metatype.MetaType;
import com.gurella.engine.metatype.Property;
import com.gurella.engine.metatype.serialization.Input;
import com.gurella.engine.metatype.serialization.Output;
import com.gurella.engine.metatype.serialization.Serializable;
import com.gurella.engine.pool.PoolService;
import com.gurella.engine.utils.ImmutableArray;
import com.gurella.engine.utils.Range;
import com.gurella.engine.utils.Sequence;
import com.gurella.engine.utils.Values;

//TODO factory method
abstract class SceneElementsProperty<T extends SceneElement> implements Property<ImmutableArray<T>> {
	String name;

	public SceneElementsProperty(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<ImmutableArray<T>> getType() {
		return Values.cast(ImmutableArray.class);
	}

	@Override
	public boolean isEditable() {
		return false;
	}

	@Override
	public Range<?> getRange() {
		return null;
	}

	@Override
	public boolean isAsset() {
		return false;
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public boolean isFinal() {
		return true;
	}

	@Override
	public boolean isCopyable() {
		return true;
	}

	@Override
	public boolean isFlatSerialization() {
		return true;
	}

	@Override
	public Property<ImmutableArray<T>> newInstance(MetaType<?> metaType) {
		return this;
	}

	@Override
	public void setValue(Object object, ImmutableArray<T> value) {
		ImmutableArray<T> elements = getValue(object);
		for (int i = 0; i < elements.size(); i++) {
			T element = elements.get(i);
			element.destroy();
		}

		for (int i = 0; i < value.size(); i++) {
			addElement(object, value.get(i));
		}
	}

	protected abstract void addElement(Object object, T element);

	@Override
	public void serialize(Object object, Object template, Output output) {
		// TODO garbage
		@SuppressWarnings("unchecked")
		SceneElements<T> sceneElements = PoolService.obtain(SceneElements.class);

		ImmutableArray<T> value = getValue(object);
		if (template == null) {
			if (value.size() != 0) {
				value.appendTo(sceneElements.elements);
				output.writeObjectProperty(name, SceneElements.class, true, sceneElements, null);
			}
			return;
		}

		ImmutableArray<T> templateValue = getValue(template);
		if (value.equals(templateValue)) {
			return;
		}

		IntSet templateIds = new IntSet();
		for (int i = 0; i < value.size(); i++) {
			T element = value.get(i);
			int prefabId = getPrefabId(element);
			if (Sequence.invalidId != prefabId) {
				templateIds.add(prefabId);
			}
			sceneElements.elements.add(element);
		}

		for (int i = 0; i < templateValue.size(); i++) {
			T element = templateValue.get(i);
			if (!templateIds.contains(element.getInstanceId())) {
				sceneElements.removedElements.add(element.ensureUuid());
			}
		}

		output.writeObjectProperty(name, SceneElements.class, true, sceneElements, null);
		PoolService.free(sceneElements);
	}

	private int getPrefabId(T element) {
		ManagedObject prefab = element.getPrefab();
		return prefab == null ? Sequence.invalidId : prefab.getInstanceId();
	}

	@Override
	public void deserialize(Object object, Object template, Input input) {
		// TODO garbage
		if (input.hasProperty(name)) {
			@SuppressWarnings("unchecked")
			SceneElements<T> sceneElements = input.readObjectProperty(name, SceneElements.class, null);
			Array<T> elements = sceneElements.elements;

			if (template == null) {
				for (int i = 0; i < elements.size; i++) {
					addElement(object, elements.get(i));
				}
				return;
			}

			IntSet addedTemplateIds = new IntSet();
			for (int i = 0; i < elements.size; i++) {
				T element = elements.get(i);
				addElement(object, elements.get(i));
				int prefabId = getPrefabId(element);
				if (prefabId != Sequence.invalidId) {
					addedTemplateIds.add(prefabId);
				}
			}

			Array<String> removedElements = sceneElements.removedElements;
			ImmutableArray<T> templateElements = getValue(template);
			for (int i = 0; i < templateElements.size(); i++) {
				T removedElement = templateElements.get(i);
				if ((removedElements == null || !removedElements.contains(removedElement.getUuid(), false))
						&& !addedTemplateIds.contains(removedElement.getInstanceId())) {
					addElement(object, input.copyObject(removedElement));
				}
			}
			PoolService.free(sceneElements);
		} else if (template != null) {
			ImmutableArray<T> templateElements = getValue(template);
			for (int i = 0; i < templateElements.size(); i++) {
				T element = templateElements.get(i);
				addElement(object, input.copyObject(element));
			}
		}
	}

	@Override
	public void copy(Object original, Object duplicate, CopyContext context) {
		ImmutableArray<T> elements = getValue(original);
		for (int i = 0; i < elements.size(); i++) {
			addElement(duplicate, context.copy(elements.get(i)));
		}
	}

	static class SceneElements<T extends SceneElement> implements Serializable<SceneElements<T>>, Poolable {
		final Array<String> removedElements = new Array<String>();
		final Array<T> elements = new Array<T>();

		@Override
		public void serialize(SceneElements<T> instance, Object template, Output output) {
			// TODO garbage
			if (removedElements.size > 0) {
				output.writeObjectProperty("removedElements", String[].class, true,
						removedElements.toArray(String.class), null);
			}
			if (elements.size > 0) {
				output.writeObjectProperty("elements", SceneElement[].class, true, elements.toArray(SceneElement.class),
						null);
			}
		}

		@Override
		public void deserialize(Object template, Input input) {
			// TODO garbage
			if (input.hasProperty("removedElements")) {
				removedElements.addAll(input.readObjectProperty("removedElements", String[].class, null));
			}

			if (input.hasProperty("elements")) {
				T[] values = Values.cast(input.readObjectProperty("elements", SceneElement[].class, null));
				elements.addAll(values);
			}
		}

		@Override
		public void reset() {
			removedElements.clear();
			elements.clear();
		}
	}
}

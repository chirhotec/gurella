package com.gurella.engine.scene;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import com.gurella.engine.base.model.CopyContext;
import com.gurella.engine.base.model.Model;
import com.gurella.engine.base.model.Property;
import com.gurella.engine.base.object.PrefabReference;
import com.gurella.engine.base.serialization.Input;
import com.gurella.engine.base.serialization.Output;
import com.gurella.engine.base.serialization.Serializable;
import com.gurella.engine.utils.ImmutableArray;
import com.gurella.engine.utils.Range;
import com.gurella.engine.utils.SequenceGenerator;
import com.gurella.engine.utils.Values;

//TODO factory method
abstract class SceneElementsProperty<T extends SceneElement2> implements Property<ImmutableArray<T>> {
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
	public Range<?> getRange() {
		return null;
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public boolean isCopyable() {
		return true;
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
	public Property<ImmutableArray<T>> newInstance(Model<?> model) {
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
		SceneElements<T> elements = new SceneElements<T>();

		ImmutableArray<T> value = getValue(object);
		if (template == null) {
			if (value.size() != 0) {
				value.appendAll(elements.elements);
				output.writeObjectProperty(name, SceneElements.class, null, elements);
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
			if (SequenceGenerator.invalidId != prefabId) {
				templateIds.add(prefabId);
			}
			elements.elements.add(element);
		}

		for (int i = 0; i < templateValue.size(); i++) {
			T element = templateValue.get(i);
			if (!templateIds.contains(element.getInstanceId())) {
				elements.removedElements.add(element.ensureUuid());
			}
		}

		output.writeObjectProperty(name, SceneElements.class, null, elements);
	}

	private int getPrefabId(T element) {
		PrefabReference prefab = element.getPrefab();
		if (prefab == null) {
			return SequenceGenerator.invalidId;
		}
		return prefab.get().getInstanceId();
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
				if (prefabId != SequenceGenerator.invalidId) {
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

	static class SceneElements<T extends SceneElement2> implements Serializable<SceneElements<T>> {
		final Array<String> removedElements = new Array<String>();
		final Array<T> elements = new Array<T>();

		@Override
		public void serialize(SceneElements<T> instance, Object template, Output output) {
			// TODO garbage
			// TODO Auto-generated method stub

		}

		@Override
		public SceneElements<T> deserialize(Object template, Input input) {
			// TODO garbage
			// TODO Auto-generated method stub
			return null;
		}
	}
}
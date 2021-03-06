package com.gurella.engine.scene.tag;

import com.badlogic.gdx.utils.ObjectMap;
import com.gurella.engine.utils.ArrayExt;
import com.gurella.engine.utils.ImmutableArray;
import com.gurella.engine.utils.ValueRegistry;

public final class Tag implements Comparable<Tag> {
	private static final ValueRegistry<Tag> registry = new ValueRegistry<Tag>();
	private static final ObjectMap<String, Tag> tagsByName = new ObjectMap<String, Tag>();
	private static final ArrayExt<Tag> values = new ArrayExt<Tag>();

	public final int id;
	public final String name;

	Tag(String name) {
		id = registry.getId(this);
		this.name = name;
		values.add(this);
	}

	public static Tag valueOf(int id) {
		return registry.getValue(id);
	}

	public static Tag valueOf(String name) {
		Tag tag = tagsByName.get(name);
		if (tag == null) {
			tag = new Tag(name);
			tagsByName.put(name, tag);
		}
		return tag;
	}

	public static ImmutableArray<Tag> values() {
		return values.immutable();
	}

	@Override
	public int compareTo(Tag other) {
		return name.compareTo(other.name);
	}
}

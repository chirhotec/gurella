package com.gurella.engine.asset.persister;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.gurella.engine.application.ApplicationConfig;
import com.gurella.engine.asset.persister.object.JsonObjectPersister;
import com.gurella.engine.asset2.properties.AssetProperties;
import com.gurella.engine.graphics.material.MaterialDescriptor;
import com.gurella.engine.managedobject.ManagedObject;
import com.gurella.engine.scene.Scene;
import com.gurella.engine.scene.SceneNode;
import com.gurella.engine.utils.Values;

public class AssetPersisters {
	private static final ObjectMap<Class<?>, PersisterInfo<?>> persisters = new ObjectMap<Class<?>, PersisterInfo<?>>();

	static {
		register(Scene.class, true, new JsonObjectPersister<Scene>(Scene.class));
		register(SceneNode.class, true, new JsonObjectPersister<SceneNode>(SceneNode.class));
		register(MaterialDescriptor.class, true, new JsonObjectPersister<MaterialDescriptor>(MaterialDescriptor.class));
		register(ManagedObject.class, true, new JsonObjectPersister<ManagedObject>(ManagedObject.class));
		register(ApplicationConfig.class, true, new JsonObjectPersister<ApplicationConfig>(ApplicationConfig.class));
		Class<AssetProperties<?>> propertiesClass = Values.cast(AssetProperties.class);
		register(propertiesClass, true, new JsonObjectPersister<AssetProperties<?>>(propertiesClass));
	}

	private AssetPersisters() {
	}

	public static <T> void register(Class<T> type, boolean derivable, AssetPersister<T> persister) {
		synchronized (persisters) {
			persisters.put(type, new PersisterInfo<T>(derivable, persister));
		}
	}

	public static <T> AssetPersister<T> get(T asset) {
		return get(asset.getClass());
	}

	private static <T> AssetPersister<T> get(Class<? extends Object> type) {
		synchronized (persisters) {
			@SuppressWarnings("unchecked")
			PersisterInfo<T> info = (PersisterInfo<T>) persisters.get(type);
			if (info != null) {
				return info.persister;
			}

			for (Entry<Class<?>, PersisterInfo<?>> entry : persisters.entries()) {
				PersisterInfo<?> derivedInfo = entry.value;
				if (derivedInfo.derivable && ClassReflection.isAssignableFrom(entry.key, type)) {
					@SuppressWarnings("unchecked")
					AssetPersister<T> derivedPersister = (AssetPersister<T>) derivedInfo.persister;
					persisters.put(type, info);
					return derivedPersister;
				}
			}
		}

		return null;
	}

	private static class PersisterInfo<T> {
		private final boolean derivable;
		private final AssetPersister<T> persister;

		PersisterInfo(boolean strict, AssetPersister<T> persister) {
			this.derivable = strict;
			this.persister = persister;
		}
	}
}

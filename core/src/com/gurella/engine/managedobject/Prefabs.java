package com.gurella.engine.managedobject;

import com.gurella.engine.metatype.CopyContext;
import com.gurella.engine.utils.ImmutableArray;

public final class Prefabs {
	private Prefabs() {
	}

	public static <T extends ManagedObject> T convertToPrefab(T object, String fileName) {
		T prefab = CopyContext.copyObject(object);
		setPrefab(object, prefab, fileName);
		return prefab;
	}

	public static <T extends ManagedObject> void convertToPrefab(T object, T prefab, String fileName) {
		setPrefab(object, prefab, fileName);
	}

	private static <T extends ManagedObject> void setPrefab(T object, T prefab, String fileName) {
		object.prefab = prefab;
		ImmutableArray<ManagedObject> children = object.children;
		ImmutableArray<ManagedObject> prefabChildren = prefab.children;
		for (int i = 0, n = children.size(); i < n; i++) {
			ManagedObject child = children.get(i);
			ManagedObject prefabChild = prefabChildren.get(i);
			setPrefab(child, prefabChild, fileName);
		}
	}

	public static <T extends ManagedObject> void dettachFromPrefab(T object) {
		ManagedObject prefab = object.getPrefab();
		if (prefab == null) {
			return;
		}

		dettachFromPrefab(object, prefab);
	}

	private static <T extends ManagedObject> void dettachFromPrefab(T object, T prefab) {
		object.prefab = null;
		ImmutableArray<ManagedObject> children = object.children;
		ImmutableArray<ManagedObject> prefabChildren = prefab.children;
		for (int i = 0; i < children.size(); i++) {
			ManagedObject child = children.get(i);
			ManagedObject prefabChild = findPrefabChild(child, prefabChildren);
			if (prefabChild != null) {
				dettachFromPrefab(child, prefabChild);
			}
		}
	}

	private static ManagedObject findPrefabChild(ManagedObject child, ImmutableArray<ManagedObject> prefabChildren) {
		String uuid = child.getUuid();
		if (uuid == null) {
			return null;
		}

		ManagedObject prefab = child.getPrefab();
		if (prefab == null) {
			return null;
		}

		for (int i = 0, n = prefabChildren.size(); i < n; i++) {
			ManagedObject prefabChild = prefabChildren.get(i);
			String prefabUuid = prefab.getUuid();
			if (prefabUuid != null && prefabUuid.equals(uuid)) {
				return prefabChild;
			}
		}

		return null;
	}
}

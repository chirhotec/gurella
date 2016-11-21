package com.gurella.studio.editor.graph;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

import com.gurella.engine.scene.SceneNode2;
import com.gurella.engine.scene.SceneNodeComponent2;

class GraphViewerComparator extends ViewerComparator {
	@Override
	public int category(Object element) {
		if (element instanceof SceneNodeComponent2) {
			return 0;
		} else if (element instanceof SceneNode2) {
			return 1;
		} else {
			throw new IllegalArgumentException("Unsupported element:" + element);
		}
	}

	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		int cat1 = category(e1);
		int cat2 = category(e2);

		if (cat1 != cat2) {
			return cat1 - cat2;
		} else if (e1 instanceof SceneNodeComponent2) {
			return ((SceneNodeComponent2) e1).getIndex() - ((SceneNodeComponent2) e2).getIndex();
		} else if (e1 instanceof SceneNode2) {
			return ((SceneNode2) e1).getIndex() - ((SceneNode2) e2).getIndex();
		} else {
			throw new IllegalArgumentException("Unsupported element:" + e1);
		}
	}
}

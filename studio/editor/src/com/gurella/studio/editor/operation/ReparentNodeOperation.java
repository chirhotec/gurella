package com.gurella.studio.editor.operation;

import static com.gurella.studio.gdx.GdxContext.post;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.gurella.engine.scene.Scene;
import com.gurella.engine.scene.SceneNode;
import com.gurella.studio.editor.subscription.EditorSceneActivityListener;
import com.gurella.studio.editor.utils.SceneChangedEvent;
import com.gurella.studio.gdx.GdxContext;

public class ReparentNodeOperation extends AbstractOperation {
	final int editorId;
	final SceneNode node;
	final SceneNode oldParent;
	final int oldIndex;
	final SceneNode newParent;
	final int newIndex;
	final Scene scene;

	public ReparentNodeOperation(int editorId, SceneNode node, SceneNode newParent, int newIndex) {
		super("Set node parent");
		this.editorId = editorId;
		this.node = node;
		this.oldParent = node.getParentNode();
		this.oldIndex = node.getIndex();
		this.newParent = newParent;
		this.newIndex = newIndex;
		this.scene = node.getScene();
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable adaptable) throws ExecutionException {
		setParent(newIndex, newParent);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable adaptable) throws ExecutionException {
		setParent(oldIndex, oldParent);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable adaptable) throws ExecutionException {
		return execute(monitor, adaptable);
	}

	private void setParent(int newIndex, SceneNode newParent) {
		if (newParent == null) {
			scene.addNode(node);
		} else {
			newParent.addChild(node);
		}

		GdxContext.clean(editorId);
		post(editorId, editorId, EditorSceneActivityListener.class, l -> l.nodeRemoved(scene, newParent, node));
		post(editorId, editorId, EditorSceneActivityListener.class, l -> l.nodeAdded(scene, newParent, node));

		node.setIndex(newIndex);
		post(editorId, editorId, SceneChangedEvent.instance);
		post(editorId, editorId, EditorSceneActivityListener.class, l -> l.nodeIndexChanged(node, newIndex));
	}
}

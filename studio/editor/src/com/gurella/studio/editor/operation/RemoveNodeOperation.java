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

public class RemoveNodeOperation extends AbstractOperation {
	final int editorId;
	final Scene scene;
	final SceneNode parentNode;
	final SceneNode node;
	final int index;

	public RemoveNodeOperation(int editorId, Scene scene, SceneNode parentNode, SceneNode node) {
		super("Remove node");
		this.editorId = editorId;
		this.scene = scene;
		this.parentNode = parentNode;
		this.node = node;
		this.index = node.getIndex();
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable adaptable) throws ExecutionException {
		if (parentNode == null) {
			scene.removeNode(node, false);
		} else {
			parentNode.removeChild(node, false);
		}

		GdxContext.removeFromBundle(editorId, scene, node);
		GdxContext.clean(editorId);
		post(editorId, editorId, EditorSceneActivityListener.class, l -> l.nodeRemoved(scene, parentNode, node));
		post(editorId, editorId, SceneChangedEvent.instance);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable adaptable) throws ExecutionException {
		if (parentNode == null) {
			scene.addNode(node);
		} else {
			parentNode.addChild(node);
		}

		GdxContext.addToBundle(editorId, scene, node, node.ensureUuid());
		GdxContext.clean(editorId);
		post(editorId, editorId, EditorSceneActivityListener.class, l -> l.nodeAdded(scene, parentNode, node));

		node.setIndex(index);
		post(editorId, editorId, EditorSceneActivityListener.class, l -> l.nodeIndexChanged(node, index));
		post(editorId, editorId, SceneChangedEvent.instance);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable adaptable) throws ExecutionException {
		return execute(monitor, adaptable);
	}
}

package com.gurella.studio.editor.operation;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.gurella.engine.asset.AssetService;
import com.gurella.engine.event.EventService;
import com.gurella.engine.managedobject.Prefabs;
import com.gurella.engine.scene.SceneElement;
import com.gurella.engine.subscriptions.application.ApplicationDebugUpdateListener;
import com.gurella.studio.editor.utils.SceneChangedEvent;

public class ConvertToPrefabOperation extends AbstractOperation {
	private final int editorId;
	private final SceneElement element;
	private final SceneElement prefab;
	private final String fileName;

	public ConvertToPrefabOperation(int editorId, SceneElement element, SceneElement prefab) {
		super("Convert to prefab");
		this.editorId = editorId;
		this.element = element;
		this.prefab = prefab;
		this.fileName = AssetService.getFileName(prefab);
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		Prefabs.convertToPrefab(element, prefab, fileName);
		EventService.post(ApplicationDebugUpdateListener.class, l -> l.debugUpdate());
		EventService.post(editorId, SceneChangedEvent.instance);
		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		return execute(monitor, info);
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		Prefabs.dettachFromPrefab(element);
		EventService.post(ApplicationDebugUpdateListener.class, l -> l.debugUpdate());
		EventService.post(editorId, SceneChangedEvent.instance);
		return Status.OK_STATUS;
	}
}

package com.gurella.studio.editor.tool;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.badlogic.gdx.math.Vector3;
import com.gurella.engine.metatype.MetaTypes;
import com.gurella.engine.metatype.Property;
import com.gurella.engine.scene.transform.TransformComponent;
import com.gurella.studio.editor.history.HistoryService;
import com.gurella.studio.editor.subscription.PropertyChangeListener;
import com.gurella.studio.editor.utils.SceneChangedEvent;
import com.gurella.studio.gdx.GdxContext;

public class RotateOperation extends TransformOperation {
	private Property<?> property;
	private Vector3 initial = new Vector3();
	private Vector3 action = new Vector3();

	public RotateOperation(int editorId, TransformComponent component) {
		super("Rotate", editorId, component);
		property = MetaTypes.getMetaType(transform).getProperty("rotation");
		component.getEulerRotation(initial);
	}

	@Override
	void rollback() {
		transform.setEulerRotation(initial);
	}

	@Override
	void commit(HistoryService historyService) {
		transform.getEulerRotation(action);
		if (initial.equals(action)) {
			return;
		}

		transform.setEulerRotation(initial);
		historyService.executeOperation(this, "Error while applying scale.");
	}

	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		transform.setEulerRotation(action);
		GdxContext.post(editorId, editorId, SceneChangedEvent.instance);
		GdxContext.post(editorId, editorId, PropertyChangeListener.class,
				l -> l.propertyChanged(transform, property, action));
		return Status.OK_STATUS;
	}

	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		transform.setEulerRotation(action);
		GdxContext.post(editorId, editorId, SceneChangedEvent.instance);
		GdxContext.post(editorId, editorId, PropertyChangeListener.class,
				l -> l.propertyChanged(transform, property, action));
		return Status.OK_STATUS;
	}

	@Override
	public IStatus undo(IProgressMonitor monitor, IAdaptable info) throws ExecutionException {
		transform.setEulerRotation(initial);
		GdxContext.post(editorId, editorId, SceneChangedEvent.instance);
		GdxContext.post(editorId, editorId, PropertyChangeListener.class,
				l -> l.propertyChanged(transform, property, initial));
		return Status.OK_STATUS;
	}
}

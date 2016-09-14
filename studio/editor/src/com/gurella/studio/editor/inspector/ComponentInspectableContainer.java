package com.gurella.studio.editor.inspector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.gurella.engine.base.model.Models;
import com.gurella.engine.scene.SceneNodeComponent2;
import com.gurella.studio.GurellaStudioPlugin;
import com.gurella.studio.editor.SceneChangedMessage;
import com.gurella.studio.editor.model.MetaModelEditor;
import com.gurella.studio.editor.model.ModelEditorFactory;

public class ComponentInspectableContainer extends InspectableContainer<SceneNodeComponent2> {
	private MetaModelEditor<SceneNodeComponent2> modelEditor;

	public ComponentInspectableContainer(InspectorView parent, SceneNodeComponent2 target) {
		super(parent, target);
		Composite head = getForm().getHead();
		head.setFont(GurellaStudioPlugin.createFont(head, SWT.BOLD));
		setText(Models.getModel(target).getName());
		FormToolkit toolkit = GurellaStudioPlugin.getToolkit();
		toolkit.adapt(this);
		toolkit.decorateFormHeading(getForm());
		getBody().setLayout(new GridLayout(3, false));
		modelEditor = ModelEditorFactory.createEditor(getBody(), getSceneEditorContext(), target);
		modelEditor.getContext().signal.addListener((event) -> postMessage(SceneChangedMessage.instance));
		modelEditor.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		layout(true, true);
	}
}

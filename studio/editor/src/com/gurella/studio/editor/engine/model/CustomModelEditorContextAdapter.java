package com.gurella.studio.editor.engine.model;

import static com.gurella.studio.GurellaStudioPlugin.createFont;
import static com.gurella.studio.editor.property.PropertyEditorFactory.createEditor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.gurella.engine.base.model.Model;
import com.gurella.engine.base.model.Property;
import com.gurella.engine.editor.model.ModelEditorFactory;
import com.gurella.engine.editor.ui.EditorComposite;
import com.gurella.engine.editor.ui.EditorLabel;
import com.gurella.engine.editor.ui.layout.EditorLayoutData;
import com.gurella.studio.GurellaStudioPlugin;
import com.gurella.studio.editor.SceneEditorContext;
import com.gurella.studio.editor.engine.ui.SwtEditorComposite;
import com.gurella.studio.editor.engine.ui.SwtEditorLabel;
import com.gurella.studio.editor.engine.ui.SwtEditorUi;
import com.gurella.studio.editor.engine.ui.SwtEditorWidget;
import com.gurella.studio.editor.model.ModelEditorContext;
import com.gurella.studio.editor.property.PropertyEditor;
import com.gurella.studio.editor.property.PropertyEditorContext;

public class CustomModelEditorContextAdapter<T> extends ModelEditorContext<T>
		implements com.gurella.engine.editor.model.ModelEditorContext<T> {
	final ModelEditorFactory<T> factory;

	public CustomModelEditorContextAdapter(ModelEditorContext<?> parent, T modelInstance,
			ModelEditorFactory<T> factory) {
		super(parent, modelInstance);
		this.factory = factory;
	}

	public CustomModelEditorContextAdapter(SceneEditorContext sceneEditorContext, T modelInstance,
			ModelEditorFactory<T> factory) {
		super(sceneEditorContext, modelInstance);
		this.factory = factory;
	}

	@Override
	public Model<T> getModel() {
		return this.model;
	}

	@Override
	public T getModelInstance() {
		return this.modelInstance;
	}

	@Override
	public EditorComposite createPropertyEditor(EditorComposite parent, Property<?> property) {
		Composite swtParent = ((SwtEditorComposite) parent).getWidget();
		PropertyEditor<?> propertyEditor = createEditor(swtParent, new PropertyEditorContext<>(this, property));
		propertyEditor.getComposite().setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		Composite body = propertyEditor.getBody();
		EditorComposite editorBody = SwtEditorWidget.getEditorWidget(body);
		return editorBody == null ? new SwtEditorComposite(body) : editorBody;
	}

	@Override
	public EditorComposite createPropertyEditor(EditorComposite parent, Property<?> property,
			EditorLayoutData layoutData) {
		Composite swtParent = ((SwtEditorComposite) parent).getWidget();
		PropertyEditor<?> propertyEditor = createEditor(swtParent, new PropertyEditorContext<>(this, property));
		propertyEditor.getComposite().setLayoutData(SwtEditorUi.transformLayoutData(layoutData));
		Composite body = propertyEditor.getBody();
		EditorComposite editorBody = SwtEditorWidget.getEditorWidget(body);
		return editorBody == null ? new SwtEditorComposite(body) : editorBody;
	}

	@Override
	public EditorLabel createPropertyLabel(EditorComposite parent, Property<?> property) {
		SwtEditorLabel editorLabel = (SwtEditorLabel) parent.getUiFactory().createLabel(parent,
				property.getDescriptiveName());
		Label label = editorLabel.getWidget();
		label.setAlignment(SWT.RIGHT);
		Font font = createFont(label, SWT.BOLD);
		label.setFont(font);
		label.addDisposeListener(e -> GurellaStudioPlugin.destroyFont(font));
		GridData labelLayoutData = new GridData(SWT.END, SWT.CENTER, false, false);
		label.setLayoutData(labelLayoutData);
		return editorLabel;
	}

	@Override
	public EditorLabel createPropertyLabel(EditorComposite parent, Property<?> property, EditorLayoutData layoutData) {
		SwtEditorLabel editorLabel = (SwtEditorLabel) parent.getUiFactory().createLabel(parent,
				property.getDescriptiveName());
		editorLabel.setLayoutData(layoutData);
		return editorLabel;
	}

	@Override
	public SwtEditorUi getEditorUi() {
		return SwtEditorUi.instance;
	}
}

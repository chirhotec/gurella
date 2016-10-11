package com.gurella.studio.editor.engine.property;

import static com.gurella.studio.editor.engine.ui.SwtEditorUi.createComposite;

import org.eclipse.swt.widgets.Composite;

import com.gurella.engine.editor.property.PropertyEditorFactory;
import com.gurella.studio.editor.property.PropertyEditor;
import com.gurella.studio.editor.property.PropertyEditorContext;
import com.gurella.studio.editor.utils.UiUtils;

public class CustomPropertyEditor<P> extends PropertyEditor<P> {
	private PropertyEditorFactory<P> factory;

	public CustomPropertyEditor(Composite parent, PropertyEditorContext<?, P> context,
			PropertyEditorFactory<P> factory) {
		super(parent, context);
		this.factory = factory;
		buildUi();
	}

	private void buildUi() {
		factory.buildUi(createComposite(body), new CustomPropertyEditorContextAdapter<P>(context, this));
	}

	private void rebuildUi() {
		UiUtils.disposeChildren(body);
		buildUi();
		body.layout(true);
	}

	@Override
	protected void updateValue(P value) {
		rebuildUi();
	}
}

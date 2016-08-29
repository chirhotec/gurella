package com.gurella.studio.editor.model.extension;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;

import com.gurella.engine.editor.ui.EditorLink;

public class SwtEditorLink extends SwtEditorControl<Link> implements EditorLink {
	public SwtEditorLink(SwtEditorBaseComposite<?> parent, int style) {
		super(parent, style);
	}

	@Override
	public String getText() {
		return widget.getText();
	}

	@Override
	public void setText(String string) {
		widget.setText(string);
	}

	@Override
	Link createWidget(Composite parent, int style) {
		return new Link(parent, style);
	}
}
package com.gurella.studio.editor.engine.ui;

import java.io.InputStream;

import org.eclipse.swt.widgets.Label;

import com.gurella.engine.editor.ui.Alignment;
import com.gurella.engine.editor.ui.EditorImage;
import com.gurella.engine.editor.ui.EditorLabel;
import com.gurella.studio.GurellaStudioPlugin;
import com.gurella.studio.editor.engine.ui.style.SwtWidgetStyle;

public class SwtEditorLabel extends SwtEditorControl<Label> implements EditorLabel {
	public SwtEditorLabel(SwtEditorLayoutComposite<?> parent, int style) {
		this(parent, "", style);
	}

	public SwtEditorLabel(SwtEditorLayoutComposite<?> parent, String text, int style) {
		super(GurellaStudioPlugin.getToolkit().createLabel(parent.widget, text, style));
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
	public Alignment getAlignment() {
		return SwtWidgetStyle.alignment(widget.getAlignment());
	}

	@Override
	public void setAlignment(Alignment alignment) {
		widget.setAlignment(SwtWidgetStyle.alignment(alignment));
	}

	@Override
	public EditorImage getImage() {
		return toEditorImage(widget.getImage());
	}

	@Override
	public void setImage(InputStream imageStream) {
		widget.setImage(toSwtImage(imageStream));
	}

	@Override
	public void setImage(EditorImage image) {
		widget.setImage(toSwtImage(image));
	}
}

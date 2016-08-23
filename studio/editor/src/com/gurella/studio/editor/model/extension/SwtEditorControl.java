package com.gurella.studio.editor.model.extension;

import static com.gurella.studio.editor.model.extension.event.SwtEditorEventType.toSwtConstant;

import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.IdentityMap;
import com.gurella.engine.editor.EditorComposite;
import com.gurella.engine.editor.EditorControl;
import com.gurella.engine.editor.event.EditorEventListener;
import com.gurella.engine.editor.event.EditorEventType;
import com.gurella.engine.utils.Values;
import com.gurella.studio.editor.model.extension.event.SwtListenerBridge;

public abstract class SwtEditorControl<T extends Control> implements EditorControl {
	static final IdentityMap<Control, EditorControl> instances = new IdentityMap<>();

	T control;

	SwtEditorControl() {
	}

	public SwtEditorControl(SwtEditorComposite parent, FormToolkit toolkit) {
		control = createControl(parent.control, toolkit);
		control.addListener(SWT.Dispose, e -> instances.remove(control));
		instances.put(control, this);
	}

	abstract T createControl(Composite parent, FormToolkit toolkit);

	@Override
	public EditorComposite getParent() {
		Composite parent = control.getParent();
		return getEditorControl(parent);
	}

	public static <T extends EditorControl> T getEditorControl(Control control) {
		return Values.cast(instances.get(control));
	}

	@Override
	public void addListener(EditorEventType eventType, EditorEventListener listener) {
		control.addListener(toSwtConstant(eventType), new SwtListenerBridge(listener));
	}

	@Override
	public EditorEventListener[] getListeners(EditorEventType eventType) {
		Listener[] listeners = control.getListeners(toSwtConstant(eventType));
		return Arrays.stream(listeners).filter(l -> l instanceof SwtListenerBridge)
				.map(l -> ((SwtListenerBridge) l).listener).toArray(i -> new EditorEventListener[i]);
	}

	@Override
	public void removeListener(EditorEventType eventType, EditorEventListener listener) {
		int swtEvent = toSwtConstant(eventType);
		Listener[] listeners = control.getListeners(swtEvent);
		Arrays.stream(listeners).filter(l -> equalsBridge(listener, l)).findFirst()
				.ifPresent(l -> control.removeListener(swtEvent, l));
	}

	private static boolean equalsBridge(EditorEventListener listener, Listener l) {
		return l instanceof SwtListenerBridge && ((SwtListenerBridge) l).listener == listener;
	}

	@Override
	public boolean isListening(EditorEventType eventType) {
		return control.isListening(toSwtConstant(eventType));
	}

	@Override
	public boolean isDisposed() {
		return control.isDisposed();
	}

	@Override
	public void dispose() {
		control.dispose();
	}

	@Override
	public <V> V getData(String key) {
		return Values.cast(control.getData(key));
	}

	@Override
	public void setData(String key, Object value) {
		control.setData(key, value);
	}

	@Override
	public int getBorderWidth() {
		return control.getBorderWidth();
	}
	
	@Override
	public boolean setFocus() {
		return control.setFocus();
	}

	@Override
	public boolean forceFocus() {
		return control.forceFocus();
	}

	@Override
	public void redraw() {
		control.redraw();
	}

	@Override
	public void pack() {
		control.pack();
	}

	@Override
	public void moveAbove(EditorControl control) {
		SwtEditorControl<?> swtControl = (SwtEditorControl<?>) control;
		this.control.moveAbove(swtControl.control);
	}

	@Override
	public void moveBelow(EditorControl control) {
		SwtEditorControl<?> swtControl = (SwtEditorControl<?>) control;
		this.control.moveBelow(swtControl.control);
	}

	@Override
	public Color getBackground() {
		org.eclipse.swt.graphics.Color background = control.getBackground();
		return new Color(background.getRed() / 255f, background.getGreen() / 255f, background.getBlue() / 255f,
				background.getAlpha() / 255f);
	}

	@Override
	public void setBackground(Color color) {
		org.eclipse.swt.graphics.Color swtColor = new org.eclipse.swt.graphics.Color(control.getDisplay(),
				(int) color.r * 255, (int) color.g * 255, (int) color.b * 255, (int) color.a * 255);
		control.addListener(SWT.Dispose, e -> swtColor.dispose());
		control.setBackground(swtColor);
	}
}
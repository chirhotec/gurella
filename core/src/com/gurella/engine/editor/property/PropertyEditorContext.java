package com.gurella.engine.editor.property;

import com.gurella.engine.editor.ui.EditorComposite;
import com.gurella.engine.editor.ui.EditorLabel;
import com.gurella.engine.editor.ui.EditorUi;
import com.gurella.engine.editor.ui.layout.EditorLayoutData;
import com.gurella.engine.metatype.Property;

public interface PropertyEditorContext<P> {
	Property<P> getProperty();

	Object getBean();

	P getPropertyValue();

	void setPropertyValue(P value);

	void addMenuItem(String text, Runnable action);

	void removeMenuItem(String text);
	
	EditorUi getEditorUi();
	
	EditorComposite createPropertyEditor(EditorComposite parent, Property<?> property);

	EditorComposite createPropertyEditor(EditorComposite parent, Property<?> property, EditorLayoutData layoutData);

	EditorLabel createPropertyLabel(EditorComposite parent, Property<?> property);

	EditorLabel createPropertyLabel(EditorComposite parent, Property<?> property, EditorLayoutData layoutData);
	
	EditorComposite createBeanEditor(EditorComposite parent, Object bean);

	EditorComposite createBeanEditor(EditorComposite parent, Object bean, EditorLayoutData layoutData);
}

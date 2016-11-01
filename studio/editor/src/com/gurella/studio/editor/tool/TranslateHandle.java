package com.gurella.studio.editor.tool;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.gurella.engine.graphics.render.GenericBatch;

public class TranslateHandle extends ToolHandle {
	public TranslateHandle(int id, Color color, Model model) {
		super(id, color, model);
	}

	@Override
	void changeColor(Color color) {
		ColorAttribute diffuse = (ColorAttribute) modelInstance.materials.get(0).get(ColorAttribute.Diffuse);
		diffuse.color.set(color);
	}

	@Override
	void render(GenericBatch batch) {
		batch.render(modelInstance);
	}

	@Override
	void applyTransform() {
		rotation.setEulerAngles(rotationEuler.y, rotationEuler.x, rotationEuler.z);
		modelInstance.transform.set(position, rotation, scale);
	}

	@Override
	public void dispose() {
		this.model.dispose();
	}
}

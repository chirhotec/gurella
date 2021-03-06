package com.gurella.engine.scene.bullet.shape;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btSphereShape;
import com.gurella.engine.graphics.render.GenericBatch;
import com.gurella.engine.metatype.PropertyChangeListener;
import com.gurella.engine.scene.renderable.debug.WireframeShader;
import com.gurella.engine.scene.renderable.shape.SphereShapeModel;
import com.gurella.engine.scene.transform.TransformComponent;

public class SphereCollisionShape extends CollisionShape implements PropertyChangeListener {
	public float radius = 1;

	private SphereShapeModel debugModel;

	@Override
	public btCollisionShape createNativeShape() {
		return new btSphereShape(radius);
	}

	@Override
	public void debugRender(GenericBatch batch, TransformComponent transformComponent) {
		if (debugModel == null) {
			debugModel = new SphereShapeModel();
			debugModel.set(radius, radius, radius);
		}

		ModelInstance instance = debugModel.getModelInstance();
		if (instance != null) {
			transformComponent.getWorldTransform(instance.transform);
			batch.render(instance, WireframeShader.getInstance());
		}
	}

	@Override
	public void propertyChanged(String propertyName, Object oldValue, Object newValue) {
		if (debugModel != null) {
			debugModel.set(radius, radius, radius);
		}
	}
}

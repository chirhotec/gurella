package com.gurella.engine.scene.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.gurella.engine.resource.model.DefaultValue;
import com.gurella.engine.resource.model.PropertyOverrides;
import com.gurella.engine.resource.model.PropertyValue;

public class OrtographicCameraComponent extends CameraComponent<OrthographicCamera> implements Poolable {
	@DefaultValue(floatValue = 1)
	private float zoom = 1;

	@Override
	OrthographicCamera createCamera() {
		return new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}

	@Override
	void initCamera() {
		super.initCamera();
		camera.zoom = zoom;
		camera.near = 0;
	}

	public float getZoom() {
		return zoom;
	}

	public void setZoom(float zoom) {
		this.zoom = zoom;
		camera.zoom = zoom;
	}
}

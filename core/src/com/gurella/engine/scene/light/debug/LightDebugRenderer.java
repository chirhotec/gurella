package com.gurella.engine.scene.light.debug;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.gurella.engine.async.AsyncService;
import com.gurella.engine.disposable.DisposablesService;
import com.gurella.engine.event.EventService;
import com.gurella.engine.graphics.render.GenericBatch;
import com.gurella.engine.scene.debug.DebugRenderable.DebugRenderContext;
import com.gurella.engine.scene.light.LightComponent;
import com.gurella.engine.scene.light.PointLightComponent;
import com.gurella.engine.scene.light.SpotLightComponent;
import com.gurella.engine.subscriptions.application.ApplicationShutdownListener;

public class LightDebugRenderer implements ApplicationShutdownListener, Disposable {
	private static final String pointLightTextureLocation = "com/gurella/engine/scene/light/debug/pointLight.png";
	private static final String spotLightTextureLocation = "com/gurella/engine/scene/light/debug/spotLight.png";

	private static final Vector3 up = new Vector3(0, 1, 0);
	private static final ObjectMap<Application, LightDebugRenderer> instances = new ObjectMap<Application, LightDebugRenderer>();

	private Texture pointLightTexture;
	private Sprite pointLightSprite;
	private Texture spotLightTexture;
	private Sprite spotLightSprite;

	private Matrix4 transform = new Matrix4();
	private Vector3 position = new Vector3();

	public static void render(DebugRenderContext context, LightComponent<?> lightComponent) {
		LightDebugRenderer renderer = getRenderer();

		renderer.renderLight(context, lightComponent);
	}

	private static LightDebugRenderer getRenderer() {
		synchronized (instances) {
			Application app = AsyncService.getCurrentApplication();
			LightDebugRenderer renderer = instances.get(app);
			if (renderer == null) {
				renderer = new LightDebugRenderer();
				instances.put(app, renderer);
			}
			return renderer;
		}
	}

	private LightDebugRenderer() {
		Files files = AsyncService.getCurrentApplication().getFiles();
		pointLightTexture = new Texture(files.classpath(pointLightTextureLocation));
		pointLightTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		pointLightSprite = new Sprite(pointLightTexture);
		pointLightSprite.setSize(0.2f, 0.2f);
		pointLightSprite.setOriginCenter();

		spotLightTexture = new Texture(files.classpath(spotLightTextureLocation));
		spotLightTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		spotLightSprite = new Sprite(spotLightTexture);
		spotLightSprite.setSize(0.2f, 0.2f);
		spotLightSprite.setOriginCenter();

		EventService.subscribe(this);
	}

	private void renderLight(DebugRenderContext context, LightComponent<?> lightComponent) {
		GenericBatch batch = context.batch;

		if (lightComponent instanceof PointLightComponent) {
			PointLightComponent pointLightComponent = (PointLightComponent) lightComponent;
			pointLightComponent.getTransform(transform);
			updateTransform(batch, context.camera);
			pointLightSprite.setColor(pointLightComponent.getColor());
			batch.render(pointLightSprite);
		} else if (lightComponent instanceof SpotLightComponent) {
			SpotLightComponent spotLightComponent = (SpotLightComponent) lightComponent;
			spotLightComponent.getTransform(transform);
			updateTransform(batch, context.camera);
			spotLightSprite.setColor(spotLightComponent.getColor());
			batch.render(spotLightSprite);
		}
	}

	protected void updateTransform(GenericBatch batch, Camera camera) {
		transform.getTranslation(position);
		transform.setToLookAt(position, camera.position, up);
		Matrix4.inv(transform.val);
		batch.set2dTransform(transform);
	}

	@Override
	public void onShutdown() {
		EventService.unsubscribe(this);
		DisposablesService.dispose(this);
		synchronized (instances) {
			instances.remove(AsyncService.getCurrentApplication());
		}
	}

	@Override
	public void dispose() {
		pointLightTexture.dispose();
		spotLightTexture.dispose();
	}
}

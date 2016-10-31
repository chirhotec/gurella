package com.gurella.engine.scene.camera.debug;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ObjectMap;
import com.gurella.engine.event.EventService;
import com.gurella.engine.graphics.render.GenericBatch;
import com.gurella.engine.scene.camera.CameraComponent;
import com.gurella.engine.scene.camera.CameraViewport;
import com.gurella.engine.scene.camera.OrtographicCameraComponent;
import com.gurella.engine.scene.debug.DebugRenderable.DebugRenderContext;
import com.gurella.engine.subscriptions.application.ApplicationShutdownListener;

public class CameraDebugRenderer implements ApplicationShutdownListener {
	private static final String camera2dTextureLocation = "com/gurella/engine/scene/camera/debug/camera2d.png";
	private static final String camera3dTextureLocation = "com/gurella/engine/scene/camera/debug/camera3d.png";

	private static final int debugWidth = 240;
	private static final int debugHeight = 160;
	private static final Vector3 up = new Vector3(0, 1, 0);

	private static final ObjectMap<Application, CameraDebugRenderer> instances = new ObjectMap<Application, CameraDebugRenderer>();

	private final FrameBuffer fbo;
	private Sprite fboSprite;

	private Texture camera2dTexture;
	private Sprite camera2dSprite;
	private Texture camera3dTexture;
	private Sprite camera3dSprite;

	private Matrix4 transform = new Matrix4();
	private Vector3 position = new Vector3();

	public static void render(DebugRenderContext context, CameraComponent<?> cameraComponent) {
		Application app = Gdx.app;
		if (app == null) {
			return;
		}

		CameraDebugRenderer renderer = instances.get(app);
		if (renderer == null) {
			renderer = new CameraDebugRenderer();
			instances.put(app, renderer);
		}

		renderer.renderCamera(context, cameraComponent);
	}

	private CameraDebugRenderer() {
		fbo = new FrameBuffer(Format.RGBA8888, debugWidth, debugHeight, true);
		fboSprite = new Sprite(fbo.getColorBufferTexture());
		fboSprite.setOriginCenter();

		camera2dTexture = new Texture(Gdx.files.classpath(camera2dTextureLocation));
		camera2dSprite = new Sprite(camera2dTexture);
		camera2dSprite.setSize(0.2f, 0.2f);
		camera2dSprite.flip(true, false);
		camera2dSprite.setOriginCenter();

		camera3dTexture = new Texture(Gdx.files.classpath(camera3dTextureLocation));
		camera3dTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
		camera3dSprite = new Sprite(camera3dTexture);
		camera3dSprite.setSize(0.2f, 0.2f);
		camera3dSprite.flip(true, false);
		camera3dSprite.setOriginCenter();
		
		EventService.subscribe(this);
	}

	private void renderCamera(DebugRenderContext context, CameraComponent<?> cameraComponent) {
		renderCameraView(context, cameraComponent);
		renderBillboard(context, cameraComponent);
	}

	private void renderBillboard(DebugRenderContext context, CameraComponent<?> cameraComponent) {
		GenericBatch batch = context.batch;
		Camera camera = context.camera;
		batch.activate2dRenderer();
		batch.set2dTransform(transform.setToTranslation(camera.position));
		batch.set2dProjection(camera.combined);

		cameraComponent.getTransform(transform);
		transform.getTranslation(position);
		transform.setToLookAt(position, camera.position, up);
		Matrix4.inv(transform.val);
		batch.set2dTransform(transform);
		batch.render(cameraComponent instanceof OrtographicCameraComponent ? camera2dSprite : camera3dSprite);
		batch.flush();
	}

	private void renderCameraView(DebugRenderContext context, CameraComponent<?> cameraComponent) {
		renderSceneToFrameBuffer(cameraComponent);

		Graphics graphics = Gdx.graphics;
		int width = graphics.getWidth();
		int height = graphics.getHeight();
		Gdx.gl.glViewport(0, 0, width, height);
		GenericBatch batch = context.batch;
		batch.activate2dRenderer();

		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
		batch.set2dTransform(transform.idt().translate(width - debugWidth - 20, 20, 0));
		batch.set2dProjection(transform.setToOrtho2D(0, 0, width, height));
		batch.render(fboSprite);
		batch.flush();
		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
	}

	private void renderSceneToFrameBuffer(CameraComponent<?> cameraComponent) {
		fbo.begin();

		Color color = cameraComponent.clearColor ? cameraComponent.clearColorValue : Color.BLACK;
		Gdx.gl.glClearColor(color.r, color.g, color.b, color.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		CameraViewport viewport = cameraComponent.viewport;
		int oldViewportWidth = (int) viewport.getViewportWidth();
		int oldViewportHeight = (int) viewport.getViewportHeight();
		viewport.update(debugWidth, debugHeight);
		cameraComponent.getScene().renderSystem.render(cameraComponent);
		viewport.update(oldViewportWidth, oldViewportHeight);

		fbo.end();
	}

	@Override
	public void shutdown() {
		Application app = Gdx.app;
		if (instances.get(app) == this) {
			instances.remove(app);
			fbo.dispose();
			camera2dTexture.dispose();
			camera3dTexture.dispose();
			EventService.unsubscribe(this);
		}
	}
}
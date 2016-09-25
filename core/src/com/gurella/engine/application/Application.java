package com.gurella.engine.application;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntMap;
import com.gurella.engine.asset.AssetService;
import com.gurella.engine.disposable.DisposablesService;
import com.gurella.engine.event.Event0;
import com.gurella.engine.event.EventService;
import com.gurella.engine.graphics.GraphicsService;
import com.gurella.engine.scene.Scene;
import com.gurella.engine.subscriptions.application.ApplicationActivityListener;
import com.gurella.engine.subscriptions.application.ApplicationResizeListener;
import com.gurella.engine.subscriptions.application.ApplicationShutdownListener;
import com.gurella.engine.subscriptions.application.ApplicationUpdateListener;

public final class Application implements ApplicationListener {
	private static final PauseEvent pauseEvent = new PauseEvent();
	private static final ResumeEvent resumeEvent = new ResumeEvent();
	private static final UpdateEvent updateEvent = new UpdateEvent();
	private static final ResizeEvent resizeEvent = new ResizeEvent();

	private static boolean initialized;

	public static float deltaTime;
	private static boolean paused;

	private static ApplicationConfig config;
	private static String initialScenePath;
	private static final SceneManager sceneManager = new SceneManager(null);

	Application(ApplicationConfig config) {
		this.config = config;
	}

	@Override
	public final void create() {
		if (initialized) {
			throw new GdxRuntimeException("Application already initialized.");
		}

		// TODO create services by checking if this is studio
		Gdx.app.setLogLevel(com.badlogic.gdx.Application.LOG_DEBUG);
		//TODO config.init(this);
		// TODO add init scripts to initializer
		GraphicsService.init();
		sceneManager.showScene(initialScenePath);
	}

	@Override
	public final void resize(int width, int height) {
		EventService.notify(resizeEvent);
	}

	@Override
	public final void render() {
		deltaTime = Gdx.graphics.getDeltaTime();
		// TODO clear must be handled by RenderSystem with spec from camera
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		EventService.notify(updateEvent);
	}

	@Override
	public final void pause() {
		paused = true;
		EventService.notify(pauseEvent);
	}

	@Override
	public final void resume() {
		paused = false;
		if (Gdx.app.getType() == ApplicationType.Android) {
			AssetService.reloadInvalidated();
		}
		EventService.notify(resumeEvent);
	}

	public final boolean isPaused() {
		return paused;
	}

	public IntMap<Scene> getScenes() {
		return sceneManager.getScenes();
	}

	public Scene getCurrentScene() {
		return sceneManager.getCurrentScene();
	}

	public String getCurrentSceneGroup() {
		return sceneManager.getCurrentSceneGroup();
	}

	@Override
	public void dispose() {
		EventService.notify(new ApplicationShutdownEvent());
		// TODO sceneManager.stop();
		DisposablesService.disposeAll();
	}

	private static class ApplicationShutdownEvent implements Event0<ApplicationShutdownListener> {
		@Override
		public Class<ApplicationShutdownListener> getSubscriptionType() {
			return ApplicationShutdownListener.class;
		}

		@Override
		public void notify(ApplicationShutdownListener listener) {
			listener.shutdown();
		}
	}

	private static class PauseEvent implements Event0<ApplicationActivityListener> {
		@Override
		public Class<ApplicationActivityListener> getSubscriptionType() {
			return ApplicationActivityListener.class;
		}

		@Override
		public void notify(ApplicationActivityListener listener) {
			listener.pause();
		}
	}

	private static class ResumeEvent implements Event0<ApplicationActivityListener> {
		@Override
		public Class<ApplicationActivityListener> getSubscriptionType() {
			return ApplicationActivityListener.class;
		}

		@Override
		public void notify(ApplicationActivityListener listener) {
			listener.resume();
		}
	}

	private static class UpdateEvent implements Event0<ApplicationUpdateListener> {
		@Override
		public Class<ApplicationUpdateListener> getSubscriptionType() {
			return ApplicationUpdateListener.class;
		}

		@Override
		public void notify(ApplicationUpdateListener listener) {
			listener.update();
		}
	}

	private static class ResizeEvent implements Event0<ApplicationResizeListener> {
		@Override
		public Class<ApplicationResizeListener> getSubscriptionType() {
			return ApplicationResizeListener.class;
		}

		@Override
		public void notify(ApplicationResizeListener listener) {
			listener.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		}
	}
}

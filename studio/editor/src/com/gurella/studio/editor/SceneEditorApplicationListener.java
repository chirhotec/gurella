package com.gurella.studio.editor;

import com.badlogic.gdx.ApplicationAdapter;
import com.gurella.engine.application.GurellaStateProvider;
import com.gurella.engine.event.EventService;
import com.gurella.engine.scene.Scene;
import com.gurella.engine.subscriptions.application.ApplicationDebugUpdateListener;
import com.gurella.engine.subscriptions.application.ApplicationShutdownListener;
import com.gurella.studio.editor.camera.CameraManager;
import com.gurella.studio.editor.focus.FocusManager;
import com.gurella.studio.editor.menu.ContextMenuManager;
import com.gurella.studio.editor.render.RenderSystem;
import com.gurella.studio.editor.subscription.EditorInputUpdateListener;
import com.gurella.studio.editor.subscription.EditorPreRenderUpdateListener;
import com.gurella.studio.editor.subscription.EditorRenderUpdateListener;
import com.gurella.studio.editor.subscription.EditorResizeListener;
import com.gurella.studio.editor.subscription.SceneLoadedListener;

final class SceneEditorApplicationListener extends ApplicationAdapter
		implements GurellaStateProvider, SceneLoadedListener {
	private final int editorId;

	private Thread renderThread;

	@SuppressWarnings("unused")
	private FocusManager focusManager;
	@SuppressWarnings("unused")
	private CameraManager cameraManager;
	@SuppressWarnings("unused")
	private ContextMenuManager contextMenuManager;
	@SuppressWarnings("unused")
	private RenderSystem renderSystem;

	public SceneEditorApplicationListener(int editorId) {
		this.editorId = editorId;
		EventService.subscribe(editorId, this);
	}

	@Override
	public void create() {
		renderThread = Thread.currentThread();
		cameraManager = new CameraManager(editorId);
		focusManager = new FocusManager(editorId);
		contextMenuManager = new ContextMenuManager(editorId);
		renderSystem = new RenderSystem(editorId);
	}

	@Override
	public void sceneLoaded(Scene scene) {
		debugUpdate();
	}

	@Override
	public void resize(int width, int height) {
		EventService.post(editorId, EditorResizeListener.class, l -> l.resize(width, height));
	}

	@Override
	public void render() {
		debugUpdate();
		EventService.post(editorId, EditorInputUpdateListener.class, l -> l.onInputUpdate());
		EventService.post(editorId, EditorPreRenderUpdateListener.class, l -> l.onPreRenderUpdate());
		EventService.post(editorId, EditorRenderUpdateListener.class, l -> l.onRenderUpdate());
	}

	static void debugUpdate() {
		EventService.post(ApplicationDebugUpdateListener.class, l -> l.debugUpdate());
	}

	@Override
	public boolean isInRenderThread() {
		return renderThread == Thread.currentThread();
	}

	@Override
	public void dispose() {
		EventService.post(ApplicationShutdownListener.class, l -> l.shutdown());
		debugUpdate();
		EventService.unsubscribe(editorId, this);
		// TODO DisposablesService.disposeAll();
	}
}
package com.gurella.studio.editor.camera;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.gurella.engine.event.EventService;
import com.gurella.studio.editor.subscription.EditorMouseListener;

class CameraController extends CameraInputController {
	public CameraController(int editorId, Camera camera) {
		super(new SceneCameraGestureListener(editorId), camera);
	}

	@Override
	protected boolean process(float deltaX, float deltaY, int button) {
		if (camera instanceof OrthographicCamera) {
			if (button == rotateButton) {
				float zoom = ((OrthographicCamera) camera).zoom;
				camera.translate(-deltaX * 620 * zoom, -deltaY * 620 * zoom, 0);
			} else if (button == translateButton) {
				((OrthographicCamera) camera).rotate(deltaY * 100);
			} else if (button == forwardButton) {
				zoom(-deltaY * 0.01f);
			}

			if (autoUpdate) {
				camera.update();
			}
			return true;
		} else {
			return super.process(deltaX, deltaY, button);
		}
	}

	@Override
	public boolean scrolled(int amount) {
		if (camera instanceof OrthographicCamera) {
			return zoom(amount * 0.01f);
		} else {
			return super.scrolled(amount);
		}
	}

	@Override
	public boolean zoom(float amount) {
		if (camera instanceof OrthographicCamera) {
			if (!alwaysScroll && activateKey != 0 && !activatePressed) {
				return false;
			}
			OrthographicCamera orthographicCamera = (OrthographicCamera) camera;
			orthographicCamera.zoom += amount;
			if (orthographicCamera.zoom < Float.MIN_VALUE) {
				orthographicCamera.zoom = Float.MIN_VALUE;
			}
			if (autoUpdate) {
				camera.update();
			}
			return true;
		} else {
			return super.zoom(amount);
		}
	}

	private static class SceneCameraGestureListener extends CameraGestureListener {
		private final int editorId;

		public SceneCameraGestureListener(int editorId) {
			this.editorId = editorId;
		}

		@Override
		public boolean tap(float x, float y, int count, int button) {
			if (count != 1) {
				return false;
			}

			switch (button) {
			case Buttons.RIGHT:
				EventService.post(editorId, EditorMouseListener.class, l -> l.onMouseMenu(x, y));
				return false;
			case Buttons.LEFT:
				EventService.post(editorId, EditorMouseListener.class, l -> l.onMouseSelection(x, y));
				return false;
			default:
				return false;
			}
		}
	}
}
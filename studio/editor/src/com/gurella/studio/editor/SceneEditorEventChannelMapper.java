package com.gurella.studio.editor;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.gurella.studio.editor.scene.SceneEditorMainContainer;
import com.gurella.studio.editor.swtgl.SwtLwjglApplication;

public class SceneEditorEventChannelMapper {
	public static final int invalidId = -1;

	private static final IntMap<GurellaSceneEditor> idToEditor = new IntMap<>();
	private static final ObjectIntMap<SceneEditorMainContainer> partControlToEditorId = new ObjectIntMap<>();
	private static final ObjectIntMap<SwtLwjglApplication> gdxAppToEditorId = new ObjectIntMap<>();
	private static final IntMap<SceneEditorContext> appIdToContext = new IntMap<>();

	private SceneEditorEventChannelMapper() {
	}

	static void put(GurellaSceneEditor editor, SceneEditorMainContainer partControl, SwtLwjglApplication application,
			SceneEditorContext context) {
		int id = editor.id;
		idToEditor.put(id, editor);
		partControlToEditorId.put(partControl, id);
		gdxAppToEditorId.put(application, id);
		appIdToContext.put(id, context);
	}

	static void remove(GurellaSceneEditor editor) {
		int id = editor.id;
		idToEditor.remove(id);
		partControlToEditorId.remove(partControlToEditorId.findKey(id), invalidId);
		gdxAppToEditorId.remove(gdxAppToEditorId.findKey(id), invalidId);
		appIdToContext.remove(id);
	}

	public static int getApplicationId(Control control) {
		Composite parent = control instanceof Composite ? (Composite) control : control.getParent();
		while (parent != null) {
			if (parent instanceof SceneEditorMainContainer) {
				return partControlToEditorId.get((SceneEditorMainContainer) parent, invalidId);
			}
		}

		return invalidId;
	}

	public static int getCurrentApplicationId() {
		Application app = Gdx.app;
		if (app instanceof SwtLwjglApplication) {
			return gdxAppToEditorId.get((SwtLwjglApplication) app, invalidId);
		} else {
			return invalidId;
		}
	}
}

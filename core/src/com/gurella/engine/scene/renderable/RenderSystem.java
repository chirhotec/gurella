package com.gurella.engine.scene.renderable;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.gurella.engine.disposable.DisposablesService;
import com.gurella.engine.event.TypePriorities;
import com.gurella.engine.event.TypePriority;
import com.gurella.engine.graphics.GenericBatch;
import com.gurella.engine.scene.SceneListener;
import com.gurella.engine.scene.SceneNodeComponent;
import com.gurella.engine.scene.SceneSystem;
import com.gurella.engine.scene.camera.CameraComponent;
import com.gurella.engine.scene.layer.Layer;
import com.gurella.engine.scene.layer.Layer.LayerOrdinalComparator;
import com.gurella.engine.scene.layer.LayerMask;
import com.gurella.engine.scene.spatial.Spatial;
import com.gurella.engine.subscriptions.application.ApplicationUpdateListener;
import com.gurella.engine.subscriptions.application.CommonUpdatePriority;

//TODO attach listeners on activate
@TypePriorities({ @TypePriority(priority = CommonUpdatePriority.RENDER, type = ApplicationUpdateListener.class) })
public class RenderSystem extends SceneSystem implements SceneListener, ApplicationUpdateListener {
	private final GenericBatch batch = DisposablesService.add(new GenericBatch());
	private Array<Layer> orderedLayers = new Array<Layer>();
	private IntMap<Array<CameraComponent<?>>> camerasByLayer = new IntMap<Array<CameraComponent<?>>>();
	private final LayerMask layerMask = new LayerMask();

	private final Array<Spatial> tempSpatials = new Array<Spatial>(256);

	@Override
	public void update() {
		for (Layer layer : orderedLayers) {
			render(layer);
		}
	}

	private void render(Layer layer) {
		for (CameraComponent<?> cameraComponent : camerasByLayer.get(layer.id)) {
			render(layer, cameraComponent);
		}
	}

	private void render(Layer layer, CameraComponent<?> cameraComponent) {
		Camera camera = cameraComponent.camera;
		cameraComponent.viewport.apply();
		batch.begin(camera);
		renderSpatials(layer, camera);
		batch.end();
	}

	private void renderSpatials(Layer layer, Camera camera) {
		layerMask.reset();
		getScene().spatialPartitioningSystem.getSpatials(camera.frustum, tempSpatials, layerMask.allowed(layer));
		for (int i = 0; i < tempSpatials.size; i++) {
			Spatial spatial = tempSpatials.get(i);
			spatial.renderableComponent.render(batch);
		}
		tempSpatials.clear();
	}

	@Override
	public void componentAdded(SceneNodeComponent component) {
	}

	@Override
	public void componentRemoved(SceneNodeComponent component) {
	}

	@Override
	public void componentActivated(SceneNodeComponent component) {
		if (component instanceof CameraComponent) {
			CameraComponent<?> cameraComponent = (CameraComponent<?>) component;
			boolean layersUpdated = false;

			if (cameraComponent.renderingLayers.size > 0) {
				for (Layer layer : cameraComponent.renderingLayers) {
					layersUpdated |= addCameraComponent(layer, cameraComponent);
				}
			} else {
				layersUpdated |= addCameraComponent(Layer.DEFAULT, cameraComponent);
			}

			if (layersUpdated) {
				orderedLayers.sort(LayerOrdinalComparator.instance);
			}
		}
	}

	private boolean addCameraComponent(Layer layer, CameraComponent<?> cameraComponent) {
		int layerId = layer.id;
		if (!camerasByLayer.containsKey(layerId)) {
			camerasByLayer.put(layerId, new Array<CameraComponent<?>>());
		}

		Array<CameraComponent<?>> layerCameras = camerasByLayer.get(layerId);
		layerCameras.add(cameraComponent);
		layerCameras.sort();
		return addLayer(layer);
	}

	private boolean addLayer(Layer layer) {
		if (mustAddLayer(layer)) {
			orderedLayers.add(layer);
			return true;
		} else {
			return false;
		}
	}

	private boolean mustAddLayer(Layer layer) {
		int layerId = layer.id;
		return camerasByLayer.containsKey(layerId) && !orderedLayers.contains(layer, true);
	}

	@Override
	public void componentDeactivated(SceneNodeComponent component) {
		if (component instanceof CameraComponent) {
			CameraComponent<?> cameraComponent = (CameraComponent<?>) component;
			boolean layersUpdated = false;
			for (Layer layer : cameraComponent.renderingLayers) {
				layersUpdated |= removeCameraComponent(layer, cameraComponent);
			}

			if (layersUpdated) {
				orderedLayers.sort(LayerOrdinalComparator.instance);
			}
		}
	}

	private boolean removeCameraComponent(Layer layer, CameraComponent<?> cameraComponent) {
		int layerId = layer.id;
		Array<CameraComponent<?>> layerCameras = camerasByLayer.get(layerId);
		layerCameras.removeValue(cameraComponent, true);

		if (layerCameras.size < 1) {
			camerasByLayer.remove(layerId);
			orderedLayers.removeValue(layer, true);
			return true;
		} else {
			layerCameras.sort();
			return false;
		}
	}
}

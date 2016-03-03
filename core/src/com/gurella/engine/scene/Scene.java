package com.gurella.engine.scene;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.gurella.engine.base.object.ManagedObject;
import com.gurella.engine.event.EventService;
import com.gurella.engine.pool.PoolService;
import com.gurella.engine.scene.audio.AudioSystem;
import com.gurella.engine.scene.bullet.BulletPhysicsSystem;
import com.gurella.engine.scene.input.InputSystem;
import com.gurella.engine.scene.layer.LayerManager;
import com.gurella.engine.scene.manager.ComponentManager;
import com.gurella.engine.scene.manager.NodeManager;
import com.gurella.engine.scene.renderable.RenderSystem;
import com.gurella.engine.scene.spatial.SpatialPartitioningSystem;
import com.gurella.engine.scene.spatial.bvh.BvhSpatialPartitioningSystem;
import com.gurella.engine.scene.tag.TagManager;
import com.gurella.engine.subscriptions.application.ApplicationUpdateListener;
import com.gurella.engine.subscriptions.scene.SceneActivityListener;
import com.gurella.engine.utils.IdentityOrderedSet;
import com.gurella.engine.utils.ImmutableArray;
import com.gurella.engine.utils.ImmutableIntMapValues;
import com.gurella.engine.utils.Values;

public final class Scene extends ManagedObject {
	private final Array<Object> tempListeners = new Array<Object>(64);

	final IntMap<SceneSystem2> _systems = new IntMap<SceneSystem2>();
	public transient final ImmutableIntMapValues<SceneSystem2> systems = ImmutableIntMapValues.with(_systems);
	transient final IdentityOrderedSet<SceneSystem2> _activeSystems = new IdentityOrderedSet<SceneSystem2>();
	public transient final ImmutableArray<SceneSystem2> activeSystems = _activeSystems.orderedItems();

	final IdentityOrderedSet<SceneNode2> _nodes = new IdentityOrderedSet<SceneNode2>();
	public transient final ImmutableArray<SceneNode2> nodes = _nodes.orderedItems();
	transient final IdentityOrderedSet<SceneNode2> _activeNodes = new IdentityOrderedSet<SceneNode2>();
	public transient final ImmutableArray<SceneNode2> activeNodes = _activeNodes.orderedItems();

	private transient final IdentityOrderedSet<SceneNodeComponent> _components = new IdentityOrderedSet<SceneNodeComponent>();
	public transient final ImmutableArray<SceneNodeComponent> components = _components.orderedItems();
	private transient final IdentityOrderedSet<SceneNodeComponent> _activeComponents = new IdentityOrderedSet<SceneNodeComponent>();
	public transient final ImmutableArray<SceneNodeComponent> activeComponents = _activeComponents.orderedItems();

	public final ComponentManager componentManager = new ComponentManager();
	public final NodeManager nodeManager = new NodeManager();
	public final TagManager tagManager = new TagManager();
	public final LayerManager layerManager = new LayerManager();

	public final SpatialPartitioningSystem<?> spatialPartitioningSystem = new BvhSpatialPartitioningSystem();
	public final InputSystem inputSystem = new InputSystem();
	public final RenderSystem renderSystem = new RenderSystem();
	public final AudioSystem audioSystem = new AudioSystem();
	//public final BulletPhysicsSystem bulletPhysicsSystem = new BulletPhysicsSystem();

	public final void start() {
		if (isActive()) {
			throw new GdxRuntimeException("Scene is already active.");
		}
		activate();
	}

	@Override
	protected void activated() {
		Array<SceneActivityListener> globalListeners = Values.cast(tempListeners);
		EventService.getSubscribers(SceneActivityListener.class, globalListeners);
		for (int i = 0; i < globalListeners.size; i++) {
			globalListeners.get(i).sceneStarted(this);
		}
	}

	public final void stop() {
		destroy();
		// TODO releaseResources();
	}

	@Override
	protected void deactivated() {
		Array<SceneActivityListener> globalListeners = Values.cast(tempListeners);
		EventService.getSubscribers(SceneActivityListener.class, globalListeners);
		for (int i = 0; i < globalListeners.size; i++) {
			globalListeners.get(i).sceneStopped(this);
		}
	}

	@Override
	protected final void childAdded(ManagedObject child) {
		if (child instanceof SceneSystem2) {
			SceneSystem2 system = (SceneSystem2) child;
			int baseSystemType = system.baseSystemType;
			if (_systems.containsKey(baseSystemType)) {
				throw new IllegalArgumentException("Scene already contains system: " + system.getClass().getName());
			}
			system.scene = this;
			_systems.put(baseSystemType, system);
		} else {
			SceneNode2 node = (SceneNode2) child;
			node.scene = this;
			_nodes.add(node);
		}
	}

	@Override
	protected void childRemoved(ManagedObject child) {
		if (child instanceof SceneSystem2) {
			SceneSystem2 system = (SceneSystem2) child;
			system.scene = null;
			_systems.remove(system.baseSystemType);
		} else {
			SceneNode2 node = (SceneNode2) child;
			node.scene = null;
			_nodes.remove(node);
		}
	}

	public void addSystem(SceneSystem2 system) {
		system.setParent(this);
	}

	public void removeSystem(SceneSystem2 system) {
		int typeId = SystemType.findType(system.getClass());
		SceneNodeComponent2 value = _systems.get(ComponentType.findBaseType(typeId));
		if (value == component) {
			component.destroy();
		}
		if (isDefaultSystem(system)) {
			throw new GdxRuntimeException("Can't remove default system.");
		}
		system.destroy();
	}

	private boolean isDefaultSystem(SceneSystem2 system) {
		// TODO Auto-generated method stub
		return false;
	}

	public <T extends SceneSystem2> T getSystem(Class<T> systemClass) {
		T system = getSystem(SystemType.getBaseSystemType(systemClass));
		// TODO fast check without reflection
		if (system == null || ClassReflection.isAssignableFrom(systemClass, system.getClass())) {
			return system;
		} else {
			return null;
		}
	}

	public <T extends SceneSystem2> T getSystem(int systemType) {
		return Values.cast(_systems.get(systemType));
	}
	
	public <T extends SceneSystem2 & Poolable> T newSystem(Class<T> systemType) {
		T system = PoolService.obtain(systemType);
		system.setParent(this);
		return system;
	}

	public void addNode(SceneNode2 node) {
		node.setParent(this);
	}

	public void removeNode(SceneNode2 node) {
		node.destroy();
	}
	
	public SceneNode2 newNode(String name) {
		SceneNode2 node = PoolService.obtain(SceneNode2.class);
		node.name = name;
		node.setParent(this);
		return node;
	}

	public String getDiagnostics() {
		StringBuilder builder = new StringBuilder();
		builder.append("Systems [");
		for (SceneSystem2 system : _systems.values()) {
			builder.append("\n\t");
			if (!system.isActive()) {
				builder.append("*");
			}
			builder.append(system.getClass().getSimpleName());
		}
		builder.append("]\n");
		builder.append("Nodes [");
		for (SceneNode2 node : nodes) {
			builder.append("\n");
			builder.append(node.getDiagnostics());
		}
		builder.append("]");

		return builder.toString();
	}
	
	public static void main(String[] args) {
		Scene scene = new Scene();
		scene.newSystem(TestSystem.class);
		SceneNode2 node = scene.newNode("node 1");
		node.newComponent(TestComponent.class);
		System.out.println(scene.getDiagnostics());

		System.out.println("\n\n\n");
		scene.activate();
		update();
		System.out.println(scene.getDiagnostics());
		
		System.out.println("\n\n\n");
		node.removeComponent(TestComponent.class);
		update();
		System.out.println(scene.getDiagnostics());
	}
	
	private static void update() {
		Array<ApplicationUpdateListener> listeners = new Array<ApplicationUpdateListener>();
		EventService.getSubscribers(ApplicationUpdateListener.class, listeners);
		for (int i = 0; i < listeners.size; i++) {
			listeners.get(i).update();
		}
	}
	
	private static class TestSystem extends SceneSystem2 implements Poolable {
		@Override
		public void reset() {
			super.reset();
		}
	}
	
	private static class TestComponent extends SceneNodeComponent2 implements Poolable {
		@Override
		public void reset() {
			super.reset();
		}
	}
}

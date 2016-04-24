package com.gurella.studio.editor.inspector;

import static org.eclipse.swt.SWT.BEGINNING;
import static org.eclipse.swt.SWT.BORDER;
import static org.eclipse.swt.SWT.CENTER;
import static org.eclipse.swt.SWT.CHECK;
import static org.eclipse.swt.SWT.END;
import static org.eclipse.swt.SWT.FILL;
import static org.eclipse.swt.SWT.NONE;
import static org.eclipse.swt.SWT.POP_UP;
import static org.eclipse.swt.SWT.PUSH;
import static org.eclipse.swt.SWT.SEPARATOR;
import static org.eclipse.ui.forms.widgets.ExpandableComposite.NO_TITLE_FOCUS_BOX;
import static org.eclipse.ui.forms.widgets.ExpandableComposite.SHORT_TITLE_BAR;
import static org.eclipse.ui.forms.widgets.ExpandableComposite.TWISTIE;

import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import com.badlogic.gdx.utils.Array;
import com.gurella.engine.base.model.Models;
import com.gurella.engine.scene.SceneNode2;
import com.gurella.engine.scene.SceneNodeComponent2;
import com.gurella.engine.scene.audio.AudioListenerComponent;
import com.gurella.engine.scene.audio.AudioSourceComponent;
import com.gurella.engine.scene.bullet.BulletPhysicsRigidBodyComponent;
import com.gurella.engine.scene.camera.OrtographicCameraComponent;
import com.gurella.engine.scene.camera.PerspectiveCameraComponent;
import com.gurella.engine.scene.light.DirectionalLightComponent;
import com.gurella.engine.scene.light.PointLightComponent;
import com.gurella.engine.scene.movement.TransformComponent;
import com.gurella.engine.scene.renderable.AtlasRegionComponent;
import com.gurella.engine.scene.renderable.ModelComponent;
import com.gurella.engine.scene.renderable.ShapeComponent;
import com.gurella.engine.scene.renderable.TextureComponent;
import com.gurella.engine.scene.renderable.TextureRegionComponent;
import com.gurella.engine.scene.tag.TagComponent;
import com.gurella.engine.test.TestInputComponent;
import com.gurella.engine.test.TestPropertyEditorsComponnent;
import com.gurella.engine.utils.ImmutableArray;
import com.gurella.engine.utils.Reflection;
import com.gurella.engine.utils.Values;
import com.gurella.studio.editor.GurellaStudioPlugin;
import com.gurella.studio.editor.SceneChangedMessage;
import com.gurella.studio.editor.model.ModelEditorContainer;
import com.gurella.studio.editor.model.property.ModelEditorContext;
import com.gurella.studio.editor.scene.ComponentAddedMessage;
import com.gurella.studio.editor.scene.NodeNameChangedMessage;

public class NodeInspectableContainer extends InspectableContainer<SceneNode2> {
	private Text nameText;
	private Button enabledCheck;
	private Label menuButton;
	private Composite componentsComposite;
	private Array<ModelEditorContainer<?>> componentContainers = new Array<>();

	public NodeInspectableContainer(InspectorView parent, SceneNode2 target) {
		super(parent, target);

		FormToolkit toolkit = GurellaStudioPlugin.getToolkit();
		toolkit.adapt(this);
		GridLayout layout = new GridLayout(4, false);
		layout.marginWidth = 0;
		layout.marginRight = 10;
		getBody().setLayout(layout);

		Label nameLabel = toolkit.createLabel(getBody(), " Name: ");
		nameLabel.setLayoutData(new GridData(BEGINNING, CENTER, false, false));

		nameText = toolkit.createText(getBody(), target.getName(), BORDER);
		nameText.setLayoutData(new GridData(FILL, BEGINNING, true, false));
		nameText.addListener(SWT.Modify, (e) -> nodeNameChanged());

		enabledCheck = toolkit.createButton(getBody(), "Enabled", CHECK);
		enabledCheck.setLayoutData(new GridData(END, CENTER, false, false));
		enabledCheck.setSelection(target.isEnabled());
		enabledCheck.addListener(SWT.Selection, (e) -> nodeEnabledChanged());

		menuButton = toolkit.createLabel(getBody(), " ", NONE);
		menuButton.setImage(GurellaStudioPlugin.createImage("icons/popup_menu.gif"));
		menuButton.setLayoutData(new GridData(END, CENTER, false, false));
		menuButton.addListener(SWT.MouseUp, (e) -> showMenu());

		componentsComposite = toolkit.createComposite(getBody());
		GridLayout componentsLayout = new GridLayout(1, false);
		componentsLayout.marginHeight = 0;
		componentsLayout.marginWidth = 0;
		componentsComposite.setLayout(componentsLayout);
		componentsComposite.setLayoutData(new GridData(FILL, FILL, true, true, 4, 1));
		initComponentContainers();
		layout(true, true);
	}

	private void nodeNameChanged() {
		target.setName(nameText.getText());
		postMessage(new NodeNameChangedMessage(target));
	}

	private void nodeEnabledChanged() {
		target.setEnabled(enabledCheck.getSelection());
		postMessage(SceneChangedMessage.instance);
	}

	@SuppressWarnings("unused")
	private void showMenu() {
		Menu menu = new Menu(getShell(), POP_UP);
		addMenuItem(menu, TransformComponent.class);
		new MenuItem(menu, SEPARATOR);
		addMenuItem(menu, BulletPhysicsRigidBodyComponent.class);
		new MenuItem(menu, SEPARATOR);
		addMenuItem(menu, OrtographicCameraComponent.class);
		addMenuItem(menu, PerspectiveCameraComponent.class);
		new MenuItem(menu, SEPARATOR);
		addMenuItem(menu, PointLightComponent.class);
		addMenuItem(menu, DirectionalLightComponent.class);
		new MenuItem(menu, SEPARATOR);
		addMenuItem(menu, AudioListenerComponent.class);
		addMenuItem(menu, AudioSourceComponent.class);
		new MenuItem(menu, SEPARATOR);
		addMenuItem(menu, TagComponent.class);
		new MenuItem(menu, SEPARATOR);
		// addItem("Layer", LayerComponent.class);
		addMenuItem(menu, TextureComponent.class);
		addMenuItem(menu, TextureRegionComponent.class);
		addMenuItem(menu, AtlasRegionComponent.class);
		new MenuItem(menu, SEPARATOR);
		addMenuItem(menu, ModelComponent.class);
		addMenuItem(menu, ShapeComponent.class);
		new MenuItem(menu, SEPARATOR);
		addMenuItem(menu, TestPropertyEditorsComponnent.class);
		addMenuItem(menu, TestInputComponent.class);
		new MenuItem(menu, SEPARATOR);
		addScriptMenuItem(menu);

		Point buttonLocation = menuButton.getLocation();
		Rectangle rect = menuButton.getBounds();
		Point menuLocation = new Point(buttonLocation.x - 1, buttonLocation.y + rect.height);

		menu.setLocation(getDisplay().map(menuButton.getParent(), null, menuLocation));
		menu.setVisible(true);
	}

	private void initComponentContainers() {
		ImmutableArray<SceneNodeComponent2> components = target.components;
		for (int i = 0; i < components.size(); i++) {
			SceneNodeComponent2 component = components.get(i);
			ModelEditorContainer<SceneNodeComponent2> propertiesContainer = createSection(component);
			componentContainers.add(propertiesContainer);
		}
	}

	private ModelEditorContainer<SceneNodeComponent2> createSection(SceneNodeComponent2 component) {
		FormToolkit toolkit = GurellaStudioPlugin.getToolkit();
		Section section = toolkit.createSection(componentsComposite, TWISTIE | SHORT_TITLE_BAR | NO_TITLE_FOCUS_BOX);
		section.setText(Models.getModel(component).getName());
		section.setLayoutData(new GridData(FILL, FILL, true, false, 1, 1));

		ModelEditorContext<SceneNodeComponent2> context = new ModelEditorContext<>(component);
		context.signal.addListener((event) -> postMessage(SceneChangedMessage.instance));

		ModelEditorContainer<SceneNodeComponent2> propertiesContainer = new ModelEditorContainer<>(section, context);
		section.setClient(propertiesContainer);
		section.setExpanded(true);

		return propertiesContainer;
	}

	private void addComponent(SceneNodeComponent2 component) {
		target.addComponent(component);
		ModelEditorContainer<SceneNodeComponent2> propertiesContainer = createSection(component);
		propertiesContainer.pack(true);
		propertiesContainer.layout(true, true);
		componentContainers.add(propertiesContainer);
		postMessage(new ComponentAddedMessage(component));
		reflow(true);
	}

	private void addMenuItem(Menu menu, final Class<? extends SceneNodeComponent2> componentType) {
		MenuItem item1 = new MenuItem(menu, PUSH);
		item1.setText(Models.getModel(componentType).getName());
		item1.addListener(SWT.Selection, (e) -> addComponent(Reflection.newInstance(componentType)));
		item1.setEnabled(target.getComponent(componentType) == null);
	}

	private void addScriptMenuItem(Menu menu) {
		MenuItem item1 = new MenuItem(menu, PUSH);
		item1.setText("Script");
		item1.addListener(SWT.Selection, (e) -> scriptMenuSeleted());
	}

	private void scriptMenuSeleted() {
		Thread current = Thread.currentThread();
		ClassLoader contextClassLoader = current.getContextClassLoader();
		try {
			current.setContextClassLoader(getSceneEditor().getClassLoader());
			addScriptComponent();
		} catch (Exception e2) {
			// TODO: handle exception
			e2.printStackTrace();
		} finally {
			current.setContextClassLoader(contextClassLoader);
		}
	}

	private void addScriptComponent()
			throws JavaModelException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		IJavaSearchScope scope = SearchEngine
				.createHierarchyScope(getSceneEditor().getJavaProject().findType(SceneNodeComponent2.class.getName()));
		SelectionDialog dialog = JavaUI.createTypeDialog(getShell(), new ProgressMonitorDialog(getShell()), scope,
				IJavaElementSearchConstants.CONSIDER_CLASSES, false);
		if (dialog.open() != IDialogConstants.OK_ID) {
			return;
		}

		Object[] types = dialog.getResult();
		if (types != null && types.length > 0) {
			IType type = (IType) types[0];
			SceneNodeComponent2 component = Values
					.cast(getSceneEditor().getClassLoader().loadClass(type.getFullyQualifiedName()).newInstance());
			addComponent(component);
		}
	}
}

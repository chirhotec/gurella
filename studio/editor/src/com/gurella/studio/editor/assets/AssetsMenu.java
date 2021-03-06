package com.gurella.studio.editor.assets;

import static com.gurella.engine.asset.descriptor.DefaultAssetDescriptors.material;
import static com.gurella.engine.asset.descriptor.DefaultAssetDescriptors.prefab;
import static com.gurella.engine.asset.descriptor.DefaultAssetDescriptors.renderTarget;
import static com.gurella.engine.asset.descriptor.DefaultAssetDescriptors.scene;
import static com.gurella.studio.GurellaStudioPlugin.log;
import static com.gurella.studio.GurellaStudioPlugin.showError;
import static com.gurella.studio.editor.utils.FileDialogUtils.enterNewFileName;
import static com.gurella.studio.editor.utils.PrettyPrintSerializer.serialize;
import static com.gurella.studio.editor.utils.Try.run;
import static java.util.stream.Collectors.toList;
import static org.eclipse.swt.SWT.POP_UP;
import static org.eclipse.swt.SWT.SEPARATOR;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import com.gurella.engine.graphics.material.MaterialDescriptor;
import com.gurella.engine.graphics.render.RenderTarget;
import com.gurella.engine.scene.Scene;
import com.gurella.engine.scene.SceneNode;
import com.gurella.engine.scene.camera.PerspectiveCameraComponent;
import com.gurella.engine.scene.light.DirectionalLightComponent;
import com.gurella.engine.scene.transform.TransformComponent;
import com.gurella.studio.editor.preferences.PreferencesNode;
import com.gurella.studio.editor.preferences.PreferencesStore;
import com.gurella.studio.editor.utils.Try;

class AssetsMenu {
	private final AssetsView view;

	AssetsMenu(AssetsView view) {
		this.view = view;
	}

	void show(IResource selection) {
		Menu menu = new Menu(view.getShell(), POP_UP);
		new MenuPopulator(view, selection).populate(menu);
		menu.setLocation(view.getDisplay().getCursorLocation());
		menu.setVisible(true);
	}

	private static class MenuPopulator {
		private final AssetsView view;
		private final Clipboard clipboard;
		private final IResource selection;

		public MenuPopulator(AssetsView view, IResource selection) {
			this.view = view;
			this.clipboard = view.clipboard;
			this.selection = selection;
		}

		public void populate(Menu menu) {
			boolean selected = selection != null;
			final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();
			boolean assetInClipboard = clipboard.getContents(transfer) instanceof AssetSelection;

			MenuItem item = new MenuItem(menu, SWT.PUSH);
			item.setText("Cut");
			item.addListener(SWT.Selection, e -> view.cut(selection));
			item.setEnabled(selected);

			item = new MenuItem(menu, SWT.PUSH);
			item.setText("Copy");
			item.addListener(SWT.Selection, e -> view.copy(selection));
			item.setEnabled(selected);

			item = new MenuItem(menu, SWT.PUSH);
			item.setText("Paste");
			item.addListener(SWT.Selection, e -> view.paste(selection));
			item.setEnabled(selected && assetInClipboard);
			addSeparator(menu);

			item = new MenuItem(menu, SWT.PUSH);
			item.setText("Delete");
			item.addListener(SWT.Selection, e -> view.delete(selection));
			IFile sceneFile = view.context.sceneFile;
			item.setEnabled(selected && !selection.equals(sceneFile));

			item = new MenuItem(menu, SWT.PUSH);
			item.setText("Rename");
			item.addListener(SWT.Selection, e -> rename());
			item.setEnabled(selected);
			addSeparator(menu);

			item = new MenuItem(menu, SWT.PUSH);
			item.setText("New folder");
			item.addListener(SWT.Selection, e -> addNewFolder());
			item.setEnabled(!selected || selection instanceof IFolder);

			item = new MenuItem(menu, SWT.PUSH);
			item.setText("Import");
			item.addListener(SWT.Selection, e -> importAssets());
			item.setEnabled(selection instanceof IFolder);
			addSeparator(menu);

			addCreateSubMenu(menu);
		}

		private static MenuItem addSeparator(Menu menu) {
			return new MenuItem(menu, SEPARATOR);
		}

		private void rename() {
			IFolder parent = (IFolder) selection.getParent();
			enterNewFileName(parent, selection.getName(), false, null).ifPresent(n -> view.rename(selection, n));
		}

		private void addNewFolder() {
			IFolder parent = getParentFolder();
			enterNewFileName(parent, "New folder", true, null).map(n -> parent.getFolder(n))
					.ifPresent(nf -> run(() -> nf.create(true, true, new NullProgressMonitor()),
							e -> log(e, "Error creating new folder")));
		}

		private void importAssets() {
			PreferencesStore store = view.preferencesStore;
			PreferencesNode preferencesNode = store.resourceNode().node(AssetsMenu.class);
			String lastPath = preferencesNode.get("lastPath", null);
			Shell shell = view.getShell();
			FileDialog dlg = new FileDialog(shell, SWT.MULTI);
			dlg.setFilterPath(lastPath);
			if (dlg.open() == null) {
				return;
			}

			String path = dlg.getFilterPath();
			List<String> unsuccessful = Stream.of(dlg.getFileNames()).filter(n -> !importAsset(path, n))
					.collect(toList());
			preferencesNode.put("lastPath", path);
			Optional.of(unsuccessful).filter(u -> !u.isEmpty()).ifPresent(u -> MessageDialog.openWarning(shell,
					"Import problems", "Some assets could not be imported: " + u.toString()));
		}

		private boolean importAsset(String path, String fileName) {
			IFolder folder = (IFolder) selection;
			File file = new File(path, fileName);
			IFile newFile = folder.getFile(fileName);
			return Try.successful(newFile).peek(nf -> nf.create(new FileInputStream(file), true, null)).isSuccess();
		}

		private void addCreateSubMenu(Menu menu) {
			boolean enabled = selection == null || selection instanceof IFolder;

			MenuItem subItem = new MenuItem(menu, SWT.CASCADE);
			subItem.setText("New");
			subItem.setEnabled(enabled);
			Menu subMenu = new Menu(menu);
			subItem.setMenu(subMenu);

			MenuItem item = new MenuItem(subMenu, SWT.PUSH);
			item.setText("Prefab");
			item.addListener(SWT.Selection, e -> addNewPrefab());
			item.setEnabled(enabled);

			item = new MenuItem(subMenu, SWT.PUSH);
			item.setText("Material");
			item.addListener(SWT.Selection, e -> addNewMaterial());
			item.setEnabled(enabled);

			item = new MenuItem(subMenu, SWT.PUSH);
			item.setText("Render target");
			item.addListener(SWT.Selection, e -> addNewRendeTarget());
			item.setEnabled(enabled);

			item = new MenuItem(subMenu, SWT.PUSH);
			item.setText("Scene");
			item.addListener(SWT.Selection, e -> addNewScene());
			item.setEnabled(enabled);
		}

		private void addNewPrefab() {
			IFolder parent = getParentFolder();
			enterNewFileName(parent, "node", true, prefab.getSingleExtension())
					.ifPresent(n -> addAsset(parent, n, newPrefab(n)));
		}

		private IFolder getParentFolder() {
			return selection == null ? (IFolder) view.rootAssetsFolder : (IFolder) selection;
		}

		private static SceneNode newPrefab(String name) {
			SceneNode node = new SceneNode();
			node.setName(name);
			node.newComponent(TransformComponent.class);
			return node;
		}

		private void addNewMaterial() {
			IFolder parent = getParentFolder();
			enterNewFileName(parent, "material", true, material.getSingleExtension())
					.ifPresent(n -> addAsset(parent, n, new MaterialDescriptor()));
		}

		private static <T> void addAsset(IFolder parent, String name, T asset) {
			IFile file = parent.getFile(name);
			@SuppressWarnings("unchecked")
			Class<T> expectedType = (Class<T>) asset.getClass();
			String serialized = serialize(expectedType, asset);
			Try.successful(serialized).map(s -> new ByteArrayInputStream(s.getBytes("UTF-8")))
					.peek(is -> file.create(is, true, new NullProgressMonitor()))
					.onFailure(e -> showError(e, "Error creating material."));
		}

		private void addNewRendeTarget() {
			IFolder parent = getParentFolder();
			enterNewFileName(parent, "renderTarget", true, renderTarget.getSingleExtension())
					.ifPresent(n -> addAsset(parent, n, new RenderTarget()));
		}

		private void addNewScene() {
			IFolder parent = getParentFolder();
			enterNewFileName(parent, "scene", true, scene.getSingleExtension())
					.ifPresent(n -> addAsset(parent, n, newScene()));
		}

		private static Scene newScene() {
			Scene scene = new Scene();
			SceneNode node = scene.newNode("Main camera");
			node.newComponent(TransformComponent.class);
			node.newComponent(PerspectiveCameraComponent.class);
			node = scene.newNode("Directional light");
			node.newComponent(TransformComponent.class);
			node.newComponent(DirectionalLightComponent.class);
			return scene;
		}
	}
}

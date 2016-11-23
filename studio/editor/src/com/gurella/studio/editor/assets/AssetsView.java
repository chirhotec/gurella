package com.gurella.studio.editor.assets;

import static com.gurella.engine.event.EventService.post;
import static com.gurella.studio.GurellaStudioPlugin.getImage;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ResourceTransfer;

import com.gurella.engine.asset.AssetType;
import com.gurella.engine.scene.SceneNode2;
import com.gurella.engine.utils.Values;
import com.gurella.studio.GurellaStudioPlugin;
import com.gurella.studio.editor.SceneEditor;
import com.gurella.studio.editor.common.ErrorComposite;
import com.gurella.studio.editor.control.DockableView;
import com.gurella.studio.editor.inspector.Inspectable;
import com.gurella.studio.editor.inspector.audio.AudioInspectable;
import com.gurella.studio.editor.inspector.bitmapfont.BitmapFontInspectable;
import com.gurella.studio.editor.inspector.material.MaterialInspectable;
import com.gurella.studio.editor.inspector.model.ModelInspectable;
import com.gurella.studio.editor.inspector.pixmap.PixmapInspectable;
import com.gurella.studio.editor.inspector.polygonregion.PolygonRegionInspectable;
import com.gurella.studio.editor.inspector.prefab.PrefabInspectable;
import com.gurella.studio.editor.inspector.texture.TextureInspectable;
import com.gurella.studio.editor.inspector.textureatlas.TextureAtlasInspectable;
import com.gurella.studio.editor.subscription.EditorSelectionListener;
import com.gurella.studio.editor.utils.DelegatingDropTargetListener;

public class AssetsView extends DockableView {
	Tree tree;
	TreeViewer viewer;
	private final AssetsViewMenu menu;
	final Clipboard clipboard;

	IResource rootResource;

	private Object lastSelection;

	public AssetsView(SceneEditor editor, int style) {
		super(editor, "Assets", getImage("icons/resource_persp.gif"), style);

		setLayout(new GridLayout());
		FormToolkit toolkit = GurellaStudioPlugin.getToolkit();
		toolkit.adapt(this);

		rootResource = editorContext.project.getFolder("assets");

		clipboard = new Clipboard(getDisplay());
		addDisposeListener(e -> clipboard.dispose());
		menu = new AssetsViewMenu(this);

		tree = toolkit.createTree(this, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		tree.setHeaderVisible(false);
		tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tree.addListener(SWT.KeyDown, e -> onKeyDown());
		tree.addListener(SWT.KeyUp, e -> onKeyUp());
		tree.addListener(SWT.MouseUp, e -> presentInspectable());
		tree.addListener(SWT.MouseUp, this::showMenu);
		tree.addListener(SWT.MouseDoubleClick, this::flipExpansion);

		viewer = new TreeViewer(tree);
		viewer.setContentProvider(new AssetsViewerContentProvider());
		viewer.setLabelProvider(new AssetsViewerLabelProvider());
		viewer.setComparator(new AssetsViewerComparator());
		viewer.setUseHashlookup(true);

		initDragManagers();

		AssetsTreeChangedListener listener = new AssetsTreeChangedListener(this);
		IWorkspace workspace = editorContext.workspace;
		workspace.addResourceChangeListener(listener);
		addDisposeListener(e -> workspace.removeResourceChangeListener(listener));

		viewer.setInput(rootResource);
	}

	private void initDragManagers() {
		LocalSelectionTransfer localTransfer = LocalSelectionTransfer.getTransfer();

		final DragSource source = new DragSource(tree, DND.DROP_DEFAULT | DND.DROP_COPY | DND.DROP_MOVE);
		source.setTransfer(new Transfer[] { ResourceTransfer.getInstance(), localTransfer });
		source.addDragListener(new ResourceDragSourceListener(tree));

		final DropTarget dropTarget = new DropTarget(tree, DND.DROP_DEFAULT | DND.DROP_MOVE);
		dropTarget.setTransfer(new Transfer[] { localTransfer });
		dropTarget.addDropListener(
				new DelegatingDropTargetListener(new MoveAssetDropTargetListener(editorContext.sceneResource),
						new ConvertToPrefabDropTargetListener(editorContext)));
	}

	protected void presentInitException(Throwable e) {
		tree.dispose();
		String message = "Error creating assets tree";
		IStatus status = GurellaStudioPlugin.log(e, message);
		ErrorComposite errorComposite = new ErrorComposite(this, status, message);
		errorComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	private IResource getResourceAt(int x, int y) {
		return Optional.ofNullable(viewer.getCell(new Point(x, y))).map(c -> c.getElement())
				.filter(IResource.class::isInstance).map(e -> (IResource) e).orElse(null);
	}

	Optional<IResource> getFirstSelectedElement() {
		return Optional.ofNullable(((ITreeSelection) viewer.getSelection()).getFirstElement())
				.map(IResource.class::cast);
	}

	Optional<IFile> getFirstSelectedFile() {
		return getFirstSelectedElement().filter(IFile.class::isInstance).map(IFile.class::cast);
	}

	private void onKeyDown() {
		lastSelection = getFirstSelectedElement().orElse(null);
	}

	private void onKeyUp() {
		getFirstSelectedElement().filter(s -> s != lastSelection).ifPresent(s -> presentInspectable());
	}

	private void presentInspectable() {
		int editorId = editorContext.editorId;
		getFirstSelectedFile().ifPresent(
				f -> post(editorId, EditorSelectionListener.class, l -> l.selectionChanged(getInspectable(f))));
	}

	private void showMenu(Event event) {
		Optional.of(event).filter(e -> e.button == 3).ifPresent(e -> menu.show(getResourceAt(event.x, event.y)));
	}

	private void flipExpansion(Event event) {
		Optional.of(event).filter(e -> e.button == 1).map(e -> getResourceAt(e.x, e.y))
				.filter(SceneNode2.class::isInstance).map(SceneNode2.class::cast)
				.ifPresent(n -> viewer.setExpandedState(n, !viewer.getExpandedState(n)));
	}

	// TODO create plugin extension
	private static Inspectable<?> getInspectable(IFile file) {
		String extension = file.getFileExtension();
		if (AssetType.texture.containsExtension(extension)) {
			return new TextureInspectable(file);
		} else if (AssetType.pixmap.containsExtension(extension)) {
			return new PixmapInspectable(file);
		} else if (AssetType.sound.containsExtension(extension)) {
			return new AudioInspectable(file);
		} else if (AssetType.textureAtlas.containsExtension(extension)) {
			return new TextureAtlasInspectable(file);
		} else if (AssetType.bitmapFont.containsExtension(extension)) {
			return new BitmapFontInspectable(file);
		} else if (AssetType.model.containsExtension(extension)) {
			return new ModelInspectable(file);
		} else if (AssetType.polygonRegion.containsExtension(extension)) {
			return new PolygonRegionInspectable(file);
		} else if (AssetType.prefab.containsExtension(extension)) {
			return new PrefabInspectable(file);
		} else if (AssetType.material.containsExtension(extension)) {
			return new MaterialInspectable(file);
		}
		return null;
	}

	TreeItem createItem(TreeItem parentItem, IResource resource, int index) {
		TreeItem nodeItem = parentItem == null ? new TreeItem(tree, SWT.NONE, index)
				: new TreeItem(parentItem, SWT.NONE, index);
		nodeItem.setText(resource.getName());
		nodeItem.setData(resource);

		if (resource instanceof IFolder) {
			nodeItem.setImage(getPlatformImage(ISharedImages.IMG_OBJ_FOLDER));
		} else if (resource instanceof IFile) {
			IFile file = (IFile) resource;
			String extension = file.getFileExtension();
			if (Values.isBlank(extension)) {
				nodeItem.setImage(getPlatformImage(ISharedImages.IMG_OBJ_FILE));
			} else if (AssetType.texture.containsExtension(extension)
					|| AssetType.pixmap.containsExtension(extension)) {
				nodeItem.setImage(getImage("icons/picture.png"));
			} else if (AssetType.sound.containsExtension(extension)) {
				nodeItem.setImage(getImage("icons/music.png"));
			} else if (AssetType.textureAtlas.containsExtension(extension)) {
				nodeItem.setImage(getImage("icons/textureAtlas.gif"));
			} else if (AssetType.polygonRegion.containsExtension(extension)) {
				nodeItem.setImage(getImage("icons/textureAtlas.gif"));
			} else if (AssetType.bitmapFont.containsExtension(extension)) {
				nodeItem.setImage(getImage("icons/font.png"));
			} else if (AssetType.model.containsExtension(extension)) {
				nodeItem.setImage(getImage("icons/16-cube-green_16x16.png"));
			} else if (AssetType.prefab.containsExtension(extension)) {
				nodeItem.setImage(getImage("icons/ice_cube.png"));
			} else if (AssetType.material.containsExtension(extension)) {
				nodeItem.setImage(getImage("icons/material.png"));
			} else {
				nodeItem.setImage(getPlatformImage(ISharedImages.IMG_OBJ_FILE));
			}
		}

		return nodeItem;
	}

	private static Image getPlatformImage(String symbolicName) {
		return PlatformUI.getWorkbench().getSharedImages().getImage(symbolicName);
	}

	public void cut(IResource selection) {
		// TODO Auto-generated method stub
	}

	public void copy(IResource selection) {
		// TODO Auto-generated method stub
	}

	public void paste(IResource selection) {
		// TODO Auto-generated method stub
	}

	public void delete(IResource selection) {
		// TODO Auto-generated method stub
	}

	public void rename(IResource selection) {
		// TODO Auto-generated method stub
	}
}

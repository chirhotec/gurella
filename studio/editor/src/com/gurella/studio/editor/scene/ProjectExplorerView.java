package com.gurella.studio.editor.scene;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IPathEditorInput;

import com.gurella.studio.editor.GurellaEditor;

public class ProjectExplorerView extends SceneEditorView {
	private static final String GURELLA_PROJECT_FILE_EXTENSION = "gprj";

	private Tree graph;

	public ProjectExplorerView(GurellaEditor editor, int style) {
		super(editor, "Project explorer", null, style);

		setLayout(new GridLayout());
		editor.getToolkit().adapt(this);
		graph = editor.getToolkit().createTree(this, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		graph.setHeaderVisible(false);
		graph.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		try {
			IPath assetsRoot = getAssetsRoot().makeRelativeTo(editor.getProject().getLocation());
			IResource resource = editor.getProject().findMember(assetsRoot);
			if (resource instanceof IContainer) {
				for (IResource member : ((IContainer) resource).members()) {
					createItems(null, member);
				}
			}
		} catch (CoreException e) {
			graph.dispose();
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private IPath getAssetsRoot() throws CoreException {
		IPathEditorInput pathEditorInput = (IPathEditorInput) editor.getEditorInput();
		IPath projectPath = editor.getProject().getLocation().makeAbsolute();
		IPath scenePath = pathEditorInput.getPath().removeLastSegments(1).makeAbsolute();
		IPath temp = scenePath;
		while (projectPath.isPrefixOf(temp)) {
			IResource member = editor.getProject().findMember(temp);
			if (member instanceof IContainer && isProjectAssetsFolder((IContainer) member)) {
				return temp;
			}
			temp = temp.removeLastSegments(1);
		}

		return scenePath;
	}

	private static boolean isProjectAssetsFolder(IContainer container) throws CoreException {
		for (IResource member : container.members()) {
			if (member instanceof IFile && GURELLA_PROJECT_FILE_EXTENSION.equals(((IFile) member).getFileExtension())) {
				return true;
			}
		}
		return false;
	}

	private void createItems(TreeItem parentItem, IResource resource) throws CoreException {
		TreeItem nodeItem = parentItem == null ? new TreeItem(graph, 0) : new TreeItem(parentItem, 0);
		nodeItem.setText(resource.getName());
		nodeItem.setData(resource);

		if (resource instanceof IContainer) {
			for (IResource member : ((IContainer) resource).members()) {
				createItems(nodeItem, member);
			}
		}
	}
}

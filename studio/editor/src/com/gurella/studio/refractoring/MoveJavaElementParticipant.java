package com.gurella.studio.refractoring;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.ui.text.FileTextSearchScope;

public class MoveJavaElementParticipant extends MoveParticipant {
	private IJavaElement element;

	@Override
	protected boolean initialize(Object element) {
		this.element = (IJavaElement) element;
		return true;
	}

	@Override
	public String getName() {
		return "Gurella java element move participant";
	}

	@Override
	public RefactoringStatus checkConditions(IProgressMonitor pm, CheckConditionsContext context)
			throws OperationCanceledException {
		return new RefactoringStatus();
	}

	@Override
	public Change createChange(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		if (RefractoringUtils.qualifiedNamesHandledByProcessor(getProcessor())) {
			return null;
		}

		IJavaElement destination = (IJavaElement) getArguments().getDestination();
		if (destination instanceof IPackageFragmentRoot) {
			return null;
		}

		IFolder assetsFolder = element.getResource().getProject().getFolder("assets");
		if (assetsFolder == null || !assetsFolder.exists()) {
			return null;
		}

		String oldName = element instanceof IType ? ((IType) element).getFullyQualifiedName('.')
				: element.getElementName();
		String destinationPath = destination instanceof IType ? ((IType) destination).getFullyQualifiedName('.')
				: destination.getElementName();
		String newName = element instanceof IType ? destinationPath + "." + element.getElementName() : destinationPath;

		System.out.println("Move java element: " + oldName + " to " + newName);

		IResource[] roots = { assetsFolder };
		String[] fileNamePatterns = { "*.pref", "*.gscn", "*.gmat", "*.glslt", "*.grt", "*.giam" };
		FileTextSearchScope scope = FileTextSearchScope.newSearchScope(roots, fileNamePatterns, false);

		final Map<IFile, TextFileChange> changes = new HashMap<>();
		TextSearchRequestor requestor = new RenameAssetSearchRequestor(changes, newName);
		Pattern pattern = Pattern.compile(oldName.replace(".", "\\."));
		TextSearchEngine.create().search(scope, requestor, pattern, monitor);

		if (changes.isEmpty()) {
			return null;
		}

		CompositeChange result = new CompositeChange("Gurella asset references update");
		changes.values().forEach(result::add);
		return result;
	}
}

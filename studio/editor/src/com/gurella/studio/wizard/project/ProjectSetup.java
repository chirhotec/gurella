package com.gurella.studio.wizard.project;

import static com.gurella.studio.wizard.project.Dependency.BOX2D;
import static com.gurella.studio.wizard.project.Dependency.BULLET;
import static com.gurella.studio.wizard.project.Dependency.GDX;
import static com.gurella.studio.wizard.project.Dependency.GURELLA;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gurella.studio.wizard.project.build.ProjectFile;

public class ProjectSetup {
	public String appName;
	public String location;
	public String packageName;
	public String initialScene;
	public String androidSdkLocation;
	public String androidApiLevel;
	public String androidBuildToolsVersion;

	public List<ProjectType> projects = new ArrayList<ProjectType>(singletonList(ProjectType.CORE));
	public List<Dependency> dependencies = Arrays.asList(GDX, BULLET, BOX2D, GURELLA);

	public File settingsFile;
	public File buildFile;
	public List<ProjectFile> files = new ArrayList<ProjectFile>();
	public Map<String, String> replacements = new HashMap<String, String>();

	public boolean isSelected(ProjectType projectType) {
		return projects.contains(projectType);
	}

	// TODO unused
	List<String> getIncompatibilities() {
		return dependencies.stream().flatMap(d -> projects.stream().flatMap(p -> d.getIncompatibilities(p).stream()))
				.collect(toList());
	}
}

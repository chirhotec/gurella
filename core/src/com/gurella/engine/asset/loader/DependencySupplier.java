package com.gurella.engine.asset.loader;

import com.badlogic.gdx.Files.FileType;

public interface DependencySupplier {
	<T> T getDependency(String fileName, FileType fileType, Class<?> assetType, String bundleId);
}

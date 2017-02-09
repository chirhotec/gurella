package com.gurella.engine.asset.loader.bitmapfont;

import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.gurella.engine.asset.loader.AssetLoader;
import com.gurella.engine.asset.loader.DependencyCollector;
import com.gurella.engine.asset.loader.DependencySupplier;

public class BitmapFontLoader implements AssetLoader<BitmapFontData, BitmapFont, BitmapFontProperties> {
	@Override
	public Class<BitmapFontProperties> getAssetPropertiesType() {
		return BitmapFontProperties.class;
	}

	@Override
	public BitmapFontData init(DependencyCollector collector, FileHandle assetFile) {
		BitmapFontData data = new BitmapFontData(assetFile, false);
		FileType fileType = assetFile.type();
		String[] imagePaths = data.getImagePaths();
		for (int i = 0, n = imagePaths.length; i < n; i++) {
			collector.addDependency(imagePaths[i], fileType, Texture.class);
		}

		return data;
	}

	@Override
	public BitmapFontData processAsync(DependencySupplier provider, FileHandle file, BitmapFontData asyncData,
			BitmapFontProperties properties) {
		if (properties != null && properties.flip) {
			asyncData.flipped = true;
		}
		return asyncData;
	}

	@Override
	public BitmapFont finish(DependencySupplier provider, FileHandle file, BitmapFontData asyncData,
			BitmapFontProperties properties) {
		FileType fileType = file.type();
		String[] imagePaths = asyncData.getImagePaths();
		int length = imagePaths.length;
		Array<TextureRegion> regs = new Array<TextureRegion>(length);
		for (int i = 0; i < length; i++) {
			regs.add(new TextureRegion(provider.getDependency(imagePaths[i], fileType, Texture.class, null)));
		}
		return new BitmapFont(asyncData, regs, true);
	}
}

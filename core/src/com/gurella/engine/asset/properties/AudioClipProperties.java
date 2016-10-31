package com.gurella.engine.asset.properties;

import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.gurella.engine.audio.AudioClip;

public class AudioClipProperties implements AssetProperties<AudioClip> {
	@Override
	public Class<AudioClip> getAssetType() {
		return AudioClip.class;
	}

	@Override
	public AssetLoaderParameters<AudioClip> createLoaderParameters() {
		// TODO Auto-generated method stub
		return null;
	}
}
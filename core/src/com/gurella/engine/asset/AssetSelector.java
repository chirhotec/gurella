package com.gurella.engine.asset;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.utils.Predicate;

public class AssetSelector<T> {
	Predicate<Void> predicate;

	String fileName;
	AssetLoaderParameters<T> parameters;

	AssetSelector() {
	}

	public AssetSelector(Predicate<Void> predicate, String fileName) {
		this.predicate = predicate;
		this.fileName = fileName;
	}
	
	public static class OsAssetPredicate implements Predicate<Void> {
		public ApplicationType applicationType;

		@Override
		public boolean evaluate(Void arg0) {
			return Gdx.app.getType() == applicationType;
		}
	}
}
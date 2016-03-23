package com.gurella.engine.desktop;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import com.gurella.engine.action.PropertiesTween;
import com.gurella.engine.action.TweenAction;
import com.gurella.engine.application.Application;

public class TweenActionTestApp {
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = "Gdx Editor";
		cfg.useGL30 = false;

		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		cfg.width = 800;
		cfg.height = 600;
		cfg.initialBackgroundColor = Color.BLACK;

		new LwjglApplication(new TestApplicationListener(), cfg);
	}

	private static class TestApplicationListener extends ApplicationAdapter {
		TweenAction action;

		@Override
		public void create() {
			action = new TweenAction(new PropertiesTween<Color>(new Color(), Color.BLACK, Color.WHITE), 0.3f,
					Interpolation.linear);
			action.setReverse(true);
		}

		@Override
		public void render() {
			Application.deltaTime = Gdx.graphics.getDeltaTime();
			super.render();
			if (!action.isComplete()) {
				action.act();
			}
		}
	}
}

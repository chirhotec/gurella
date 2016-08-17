package com.gurella.engine.scene.action;

import com.badlogic.gdx.Gdx;

public class DelayAction extends SceneAction {
	float duration;
	private float time;

	DelayAction() {
	}

	public DelayAction(float duration) {
		this.duration = duration;
	}

	@Override
	public boolean doAct() {
		time += Gdx.graphics.getDeltaTime();
		return time >= duration;
	}

	@Override
	public void restart() {
		super.restart();
		time = 0;
	}
}

package com.gurella.studio.editor.inspector.audio;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.forms.widgets.FormToolkit;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.gurella.engine.asset.loader.audio.SoundDuration;
import com.gurella.studio.GurellaStudioPlugin;
import com.gurella.studio.editor.inspector.InspectableContainer;
import com.gurella.studio.editor.inspector.InspectorView;

public class AudioInspectableContainer extends InspectableContainer<IFile> {
	private Label durationLabel;
	private Button playButton;
	private Button stopButton;
	private ProgressBar progressBar;

	private Image playImage;
	private Image pauseImage;

	private Music music;
	private float totalDuration;

	public AudioInspectableContainer(InspectorView parent, IFile target) {
		super(parent, target);

		Composite head = getForm().getHead();
		head.setFont(GurellaStudioPlugin.getFont(head, SWT.BOLD));
		setText(target.getName());

		FormToolkit toolkit = GurellaStudioPlugin.getToolkit();
		toolkit.adapt(this);
		toolkit.decorateFormHeading(getForm());

		Composite body = getBody();
		body.setLayout(new GridLayout(2, false));

		FileHandle fileHandle = new FileHandle(target.getLocation().toFile());
		music = Gdx.audio.newMusic(fileHandle);
		body.addDisposeListener(e -> music.dispose());
		totalDuration = SoundDuration.totalDuration(fileHandle);

		playImage = GurellaStudioPlugin.getImage("icons/audio/play-button.png");
		pauseImage = GurellaStudioPlugin.getImage("icons/audio/pause-button.png");

		durationLabel = toolkit.createLabel(body, "00:00:000 / " + formatDuration((int) (totalDuration * 1000)));
		GridDataFactory.swtDefaults().span(2, 1).grab(true, false).align(SWT.CENTER, SWT.TOP).applyTo(durationLabel);

		progressBar = new ProgressBar(body, SWT.SMOOTH | SWT.HORIZONTAL);
		GridData layoutData = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
		layoutData.horizontalSpan = 2;
		layoutData.minimumWidth = 100;
		progressBar.setLayoutData(layoutData);
		progressBar.setMinimum(0);
		progressBar.setMaximum((int) (totalDuration * 10000));

		playButton = toolkit.createButton(body, "", SWT.PUSH);
		playButton.setImage(playImage);
		playButton.setLayoutData(new GridData(SWT.RIGHT, SWT.BEGINNING, true, false));
		playButton.addListener(SWT.Selection, e -> play());

		stopButton = toolkit.createButton(body, "", SWT.PUSH);
		stopButton.setImage(GurellaStudioPlugin.getImage("icons/audio/stop-button.png"));
		stopButton.setLayoutData(new GridData(SWT.LEFT, SWT.BEGINNING, true, false));
		stopButton.addListener(SWT.Selection, e -> music.stop());

		Label separator = new Label(body, SWT.HORIZONTAL | SWT.SEPARATOR);
		GridDataFactory.swtDefaults().span(2, 1).grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(separator);

		getDisplay().timerExec(40, () -> updateProgress());
		reflow(true);
	}

	private void play() {
		synchronized (music) {
			if (music.isPlaying()) {
				music.pause();
				playButton.setImage(playImage);
			} else {
				music.play();
				playButton.setImage(pauseImage);
			}
		}
	}

	private void updateProgress() {
		if (!isDisposed()) {
			synchronized (music) {
				float position = music.getPosition();
				progressBar.setSelection((int) (position * 10000));
				durationLabel.setText(
						formatDuration((int) (position * 1000)) + " / " + formatDuration((int) (totalDuration * 1000)));

				if (!music.isPlaying() && playButton.getImage() != playImage) {
					playButton.setImage(playImage);
				}

				getDisplay().timerExec(40, () -> updateProgress());
			}
		}
	}

	private static String formatDuration(int duration) {
		int durationTemp = duration;
		int minutes = durationTemp / 60000;
		durationTemp = durationTemp % 60000;
		int seconds = durationTemp / 1000;
		durationTemp = durationTemp % 1000;
		int milis = durationTemp;

		StringBuffer buffer = new StringBuffer();
		if (minutes < 10) {
			buffer.append('0');
		}
		buffer.append(minutes).append(":");

		if (seconds < 10) {
			buffer.append('0');
		}
		buffer.append(seconds).append(":");

		if (milis < 100) {
			buffer.append('0');
		}

		if (milis < 10) {
			buffer.append('0');
		}
		buffer.append(milis);

		return buffer.toString();
	}
}

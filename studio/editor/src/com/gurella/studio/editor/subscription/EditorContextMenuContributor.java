package com.gurella.studio.editor.subscription;

import com.gurella.engine.event.EventSubscription;
import com.gurella.studio.editor.menu.ContextMenuActions;

public interface EditorContextMenuContributor extends EventSubscription {
	void contribute(ContextMenuActions actions);
}

package com.gurella.engine.state.transitionmanager;

import com.badlogic.gdx.utils.ObjectMap;
import com.gurella.engine.application.CommonUpdatePriority;
import com.gurella.engine.application.events.UpdateEvent;
import com.gurella.engine.application.events.UpdateListener;
import com.gurella.engine.event.EventService;
import com.gurella.engine.event.Signal1;
import com.gurella.engine.event.Signal2;
import com.gurella.engine.state.StateTransition;

public class StateMachine<STATE> extends Signal2<StateMachine.StateChangedListener<STATE>, STATE, STATE>
		implements UpdateListener {
	private StateTransition<STATE> currentTransition;
	private ObjectMap<STATE, StateChangedSignal> stateListeners = new ObjectMap<STATE, StateChangedSignal>();
	private StateMachineContext<STATE> context;

	public StateMachine(StateMachineContext<STATE> stateTransitionManager) {
		this.context = stateTransitionManager;
		stateChanged(stateTransitionManager.getCurrentState());
	}

	public boolean apply(STATE newState) {
		StateTransition<STATE> stateTransition = getStateTransition(newState);

		if (stateTransition == null) {
			return false;
		} else {
			currentTransition = stateTransition;
			processTransition();
			return true;
		}
	}

	private StateTransition<STATE> getStateTransition(STATE newState) {
		if (isInTransition()) {
			return null;
		} else {
			return context.getStateTransition(newState);
		}
	}

	private void processTransition() {
		currentTransition.process();
		if (currentTransition.isFinished()) {
			endTransition();
		} else {
			EventService.addListener(UpdateEvent.class, this);
		}
	}

	private void endTransition() {
		STATE currentState = currentTransition.getDestination();
		context.stateChanged(currentState);
		stateChanged(currentState);
		dispatchStateChanged(currentState);
		currentTransition = null;
	}

	protected void stateChanged(@SuppressWarnings("unused") STATE newState) {
	}

	private void dispatchStateChanged(STATE currentState) {
		dispatch(currentTransition.getSource(), currentState);
		StateChangedSignal stateChangedSignal = stateListeners.get(currentState);
		if (stateChangedSignal != null) {
			stateChangedSignal.dispatch(currentState);
		}
	}

	public STATE getCurrentState() {
		return context.getCurrentState();
	}

	public boolean isInState(STATE... states) {
		STATE currentState = getCurrentState();
		if (states == null || currentState == null) {
			return false;
		} else {
			for (STATE state : states) {
				if (currentState == state) {
					return true;
				}
			}

			return false;
		}
	}

	public boolean isInTransition() {
		return currentTransition != null;
	}

	@Override
	protected void dispatch(StateChangedListener<STATE> listener, STATE oldState, STATE newState) {
		listener.stateChanged(oldState, newState);
	}

	@Override
	public void update() {
		if (currentTransition.isFinished()) {
			endTransition();
			EventService.removeListener(UpdateEvent.class, this);
		}
	}

	@Override
	public int getPriority() {
		return CommonUpdatePriority.THINK;
	}

	public void addListener(STATE state, StateListener<STATE> listener) {
		if (listener != null) {
			if (!stateListeners.containsKey(state)) {
				stateListeners.put(state, new StateChangedSignal());
			}
			stateListeners.get(state).addListener(listener);
		}
	}

	public void removeListener(STATE state, StateListener<STATE> listener) {
		if (listener != null && stateListeners.containsKey(state)) {
			stateListeners.get(state).removeListener(listener);
		}
	}

	public interface StateChangedListener<STATE> {
		void stateChanged(STATE oldState, STATE newState);
	}

	public interface StateListener<STATE> {
		void stateChanged(STATE newState);
	}

	private class StateChangedSignal extends Signal1<StateListener<STATE>, STATE> {
		@Override
		protected void dispatch(StateListener<STATE> listener, STATE event) {
			listener.stateChanged(event);
		}
	}
}

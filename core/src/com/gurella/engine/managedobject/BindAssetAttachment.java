package com.gurella.engine.managedobject;

import com.badlogic.gdx.utils.Pool.Poolable;
import com.gurella.engine.asset.AssetService;
import com.gurella.engine.async.AsyncCallback;
import com.gurella.engine.pool.PoolService;
import com.gurella.engine.subscriptions.managedobject.ObjectDestroyedListener;

//TODO maybe not needed
public class BindAssetAttachment<T> extends Attachment<T> implements Poolable {
	static <T> BindAssetAttachment<T> obtain(T asset) {
		@SuppressWarnings("unchecked")
		BindAssetAttachment<T> attachment = PoolService.obtain(BindAssetAttachment.class);
		attachment.value = asset;
		return attachment;
	}

	static <T> void loadAsync(ManagedObject object, String fileName, Class<T> assetType, AsyncCallback<T> callback) {
		AssetService.loadAsync(Callback.obtain(object, callback), fileName, assetType, 0);
	}

	@Override
	protected void attach() {
	}

	@Override
	protected void detach() {
	}

	@Override
	public void reset() {
		AssetService.unload(value);
		value = null;
	}

	static class Callback<T> implements AsyncCallback<T>, ObjectDestroyedListener, Poolable {
		private final Object mutex = new Object();
		ManagedObject object;
		AsyncCallback<T> delegate;
		boolean objectDestroyed;

		static <T> Callback<T> obtain(ManagedObject object, AsyncCallback<T> delegate) {
			@SuppressWarnings("unchecked")
			Callback<T> callback = PoolService.obtain(Callback.class);
			callback.object = object;
			object.subscribeTo(callback);
			callback.delegate = delegate;
			return callback;
		}

		@Override
		public void onSuccess(T value) {
			synchronized (mutex) {
				if (objectDestroyed) {
					AssetService.unload(value);
				} else {
					object.bindAsset(value);
					delegate.onSuccess(value);
				}
				PoolService.free(this);
			}
		}

		@Override
		public void onException(Throwable exception) {
			synchronized (mutex) {
				if (!objectDestroyed) {
					delegate.onException(exception);
					PoolService.free(this);
				}
			}
		}

		@Override
		public void onCanceled(String message) {
			synchronized (mutex) {
				if (!objectDestroyed) {
					delegate.onCanceled(message);
					PoolService.free(this);
				}
			}
		}

		@Override
		public void onProgress(float progress) {
			synchronized (mutex) {
				if (!objectDestroyed) {
					delegate.onProgress(progress);
				}
			}
		}

		@Override
		public void onObjectDestroyed() {
			synchronized (mutex) {
				objectDestroyed = true;
			}
		}

		@Override
		public void reset() {
			objectDestroyed = false;
			object = null;
			delegate = null;
		}
	}
}

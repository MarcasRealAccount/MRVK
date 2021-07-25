package marcasrealaccount.vulkan.instance;

import java.util.ArrayList;

public abstract class VulkanHandle<T> {
	protected T nullHandle;
	protected T handle;
	protected boolean created = false;
	protected boolean invalidated = false;
	private boolean recreate = false, wasInvalidated = false;

	private final ArrayList<VulkanHandle<?>> invalidates = new ArrayList<>();

	public VulkanHandle(T nullHandle) {
		this.handle = this.nullHandle = nullHandle;
	}

	public VulkanHandle(T nullHandle, T handle) {
		this.nullHandle = nullHandle;
		this.handle = handle;
		this.created = false;
	}

	public final boolean create() {
		if (this.created) {
			this.recreate = true;
			close();
			this.recreate = false;
		} else {
			this.handle = this.nullHandle;
		}

		createAbstract();
		this.created = this.handle != this.nullHandle;

		this.invalidated = false;
		return isValid();
	}

	public final void close() {
		for (int i = this.invalidates.size() - 1; i >= 0; --i)
			this.invalidates.get(i).invalidate();
		this.invalidates.clear();

		if (this.handle != this.nullHandle)
			closeAbstract(this.recreate, this.wasInvalidated);
		this.handle = this.nullHandle;
	}

	public final void invalidate() {
		this.invalidated = true;
		this.wasInvalidated = true;
		close();
		this.wasInvalidated = false;
	}

	public boolean isValid() {
		return !this.invalidated && this.handle != this.nullHandle;
	}

	public boolean isInvalidated() {
		return this.invalidated;
	}

	public boolean isCreated() {
		return this.created;
	}

	public final T getHandle() {
		return this.handle;
	}

	public final void addInvalidate(VulkanHandle<?> invalidate) {
		this.invalidates.add(invalidate);
	}

	public final void removeInvalidate(VulkanHandle<?> invalidate) {
		this.invalidates.remove(invalidate);
	}

	protected abstract void createAbstract();

	protected abstract void closeAbstract(boolean recreate, boolean wasInvalidated);
}

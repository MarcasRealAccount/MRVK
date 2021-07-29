package marcasrealaccount.vulkan;

import java.util.ArrayList;
import java.util.Iterator;

public abstract class VulkanHandle<T> {
	protected T       nullHandle;
	protected T       handle;
	private boolean   destroyable;
	private boolean   created  = false;
	protected boolean recreate = false;

	private Iterator<VulkanHandle<?>>        destroyIterator      = null;
	private VulkanHandle<?>                  destroyCurrentHandle = null;
	private final ArrayList<VulkanHandle<?>> destroyedChildren    = new ArrayList<>();
	private final ArrayList<VulkanHandle<?>> children             = new ArrayList<>();

	public VulkanHandle(T nullHandle) {
		this(nullHandle, true);
	}

	public VulkanHandle(T nullHandle, boolean destroyable) {
		this.handle      = this.nullHandle = nullHandle;
		this.destroyable = destroyable;
	}

	public final boolean create() {
		boolean pCreated = this.created;
		if (pCreated) {
			this.recreate = true;
			destroy();
		}

		createAbstract();
		this.created = isValid();
		if (pCreated && this.created) {
			for (var child : this.destroyedChildren) child.create();
			this.destroyedChildren.clear();
		}
		this.recreate = false;
		return this.created;
	}

	public final void destroy() {
		if (this.recreate) this.destroyedChildren.clear();
		this.destroyIterator = this.children.iterator();
		while (this.destroyIterator.hasNext()) {
			this.destroyCurrentHandle = this.destroyIterator.next();
			if (this.destroyCurrentHandle.isValid()) {
				this.destroyCurrentHandle.destroy();
				if (this.recreate && this.destroyCurrentHandle.destroyable) this.destroyedChildren.add(this.destroyCurrentHandle);
			}
		}
		this.destroyIterator      = null;
		this.destroyCurrentHandle = null;

		if (this.destroyable && this.handle != this.nullHandle) destroyAbstract();
		this.handle = this.nullHandle;
	}

	public final void remove() {
		if (this.created) destroy();
		removeAbstract();
	}

	public final boolean isValid() {
		return this.handle != this.nullHandle;
	}

	public boolean isCreated() {
		return created;
	}

	public final T getHandle() {
		return this.handle;
	}

	public final <Y> void addChild(VulkanHandle<Y> handle) {
		this.children.add(handle);
	}

	public final <Y> void removeChild(VulkanHandle<Y> handle) {
		if (this.destroyIterator != null) {
			if (this.destroyCurrentHandle == handle)
				this.destroyIterator.remove();
			else
				throw new RuntimeException("Can't remove children whilst iterating them!");
		} else {
			this.children.remove(handle);
		}
	}

	protected abstract void createAbstract();

	protected abstract void destroyAbstract();

	protected abstract void removeAbstract();
}

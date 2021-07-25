package marcasrealaccount.vulkan;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Reference {
	public static final Logger LOGGER = LogManager.getLogger("MRVK");
	public static final String ID = "mrvk";
	public static final int MAJOR = 0;
	public static final int MINOR = 1;

	public static final boolean USE_VALIDATION_LAYERS = true;

	private Reference() {
	}
}

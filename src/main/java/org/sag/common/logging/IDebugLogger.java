package org.sag.common.logging;

import java.nio.file.Path;

public interface IDebugLogger extends ILogger {
	public String getFileNameWithoutExtension();
	public Path getFilePath();
	public void setRemove(boolean remove);
	public void closeOrCloseAndRemove();
	public void closeAndRemove();
}

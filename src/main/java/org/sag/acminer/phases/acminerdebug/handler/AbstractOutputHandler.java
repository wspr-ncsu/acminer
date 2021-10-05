package org.sag.acminer.phases.acminerdebug.handler;

import java.nio.file.Path;
import java.util.Objects;

import org.sag.acminer.IACMinerDataAccessor;


public abstract class AbstractOutputHandler implements OutputHandler {

	protected final String cn;
	protected final Path rootOutputDir;
	protected final IACMinerDataAccessor dataAccessor;
	
	public AbstractOutputHandler(Path rootOutputDir, IACMinerDataAccessor dataAccessor) {
		Objects.requireNonNull(rootOutputDir);
		Objects.requireNonNull(dataAccessor);
		this.rootOutputDir = rootOutputDir;
		this.dataAccessor = dataAccessor;
		this.cn = this.getClass().getSimpleName();
	}
	
}

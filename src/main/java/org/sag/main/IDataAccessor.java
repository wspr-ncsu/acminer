package org.sag.main;

import org.sag.main.config.Config;

public interface IDataAccessor {
	
	Config getConfig();

	void resetAllSootData(boolean resetSootInstance);

	void resetAllDatabasesAndData();

}

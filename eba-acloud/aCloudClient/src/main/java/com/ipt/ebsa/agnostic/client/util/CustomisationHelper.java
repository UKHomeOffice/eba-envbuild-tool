package com.ipt.ebsa.agnostic.client.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.ipt.ebsa.agnostic.client.logging.LogUtils;
import com.ipt.ebsa.agnostic.client.logging.LogUtils.LogAction;
import com.ipt.ebsa.agnostic.client.skyscape.exception.UnresolvedDependencyException;

public class CustomisationHelper {

	public static String readCustomisationScript(String custScriptFileName, String guestCustScriptDir) throws UnresolvedDependencyException, IOException {
		String customisationScriptData = null;
		LogUtils.log(LogAction.GETTING, custScriptFileName, "Customisations script");
		if (StringUtils.isNotBlank(custScriptFileName)) {
			File file;
			if (guestCustScriptDir == null) {
				file = new File(custScriptFileName);
			} else {
				file = new File(guestCustScriptDir, custScriptFileName);
			}
			if (!file.exists()) {
				throw new UnresolvedDependencyException("The guest customisation script '" + file.getAbsolutePath() + "' does not exist or cannot be read");
			}
			LogUtils.log(LogAction.GOT, file.getAbsolutePath(), "Customisation script location");
			customisationScriptData = FileUtils.readFileToString(file);
		} else {
			LogUtils.log(LogAction.GOT, "Customisation script not specified");
		}
		return customisationScriptData;
	}
}

package com.ipt.ebsa.util;

import java.io.File;

import org.apache.log4j.Logger;

public class FileUtil {

	private static final Logger LOG = Logger.getLogger(FileUtil.class);
	
	public static void checkDirExistsOrCreate(File dir) {
		if (dir.exists()) {
			LOG.info(String.format("Found dir at [%s]", dir.getAbsolutePath()));
		} else {
			if (dir.mkdirs()) {
				LOG.info(String.format("Created dir at [%s]", dir.getAbsolutePath()));
			} else {
				throw new RuntimeException(String.format("Failed to create dir [%s]", dir.getAbsolutePath()));
			}
		}
	}
}

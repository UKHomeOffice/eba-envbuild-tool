package com.ipt.ebsa.environment.hiera;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.ipt.ebsa.hiera.HieraMachineState;
import com.ipt.ebsa.yaml.YamlUtil;

/**
 * Manages sharing hiera files and their changes between actions
 * @author James Shepherd
 */
public class HieraFileManager {
	private static final Logger LOG = Logger.getLogger(HieraFileManager.class);
	
	private TreeMap<File, HieraMachineState> hieraFiles = new TreeMap<>();
	
	public HieraMachineState getHieraFile(File hieraFilePath, String environment, String hostOrRole) {
		
		HieraMachineState hieraFile = hieraFiles.get(hieraFilePath);

		if (null == hieraFile) {
			Map<String, Object> yaml = new TreeMap<>();
			if (hieraFilePath.exists()) {
				try {
					LOG.info(String.format("Found yaml file [%s]", hieraFilePath.getName()));
					yaml = YamlUtil.readYaml(hieraFilePath);
				} catch (FileNotFoundException e) {
					throw new RuntimeException(String.format("Failed to parse yaml file [%s]", hieraFilePath.getName()), e);
				} catch (Exception e) {
					throw new RuntimeException(String.format("Failed to load yaml file [%s]", hieraFilePath.getAbsolutePath()), e);
				}
			} else {
				LOG.info(String.format("Didn't find yaml file [%s], will create later", hieraFilePath.getName()));
			}
			hieraFile = new HieraMachineState(environment, hostOrRole, hieraFilePath, yaml);
			hieraFiles.put(hieraFilePath, hieraFile);
		} else {
			LOG.debug(String.format("Using existing HieraFile [%s]", hieraFilePath.getAbsolutePath()));
		}
		
		return hieraFile;
	}
}

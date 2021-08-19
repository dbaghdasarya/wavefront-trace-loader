package com.wavefront.helpers;

import com.wavefront.config.ApplicationConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.util.StdConverter;


public class ApplicationConfigValidator extends StdConverter<ApplicationConfig, ApplicationConfig> {
  static final Logger LOGGER = Logger.getLogger((ApplicationConfigValidator.class.getCanonicalName()));
  private List<String> inputJsonFiles = null;

  public ApplicationConfigValidator() {}

  public boolean isValidFileName(final String fileName) {
    final File aFile = new File(fileName);
    if (!aFile.exists()) {
      return false;
    }
    return true;
  }

  public ApplicationConfig convert(ApplicationConfig applicationConfig) {
    // check inputJsonFiles
    List<String> input = applicationConfig.getInputJsonFiles();
    if (!input.isEmpty()) {
      input.forEach(inputJsonFile -> {
        try {
          if (!isValidFileName(inputJsonFile)) {
            throw new FileNotFoundException();
          }
        } catch (FileNotFoundException e) {
          LOGGER.log(Level.SEVERE, " Specified file: " + inputJsonFile + " doesn't exist");
          System.exit(1);
        }
      });
    }
    return applicationConfig;
  }
}

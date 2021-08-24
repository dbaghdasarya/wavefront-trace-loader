package com.wavefront.helpers;

import com.fasterxml.jackson.databind.util.StdConverter;
import com.wavefront.config.ApplicationConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ApplicationConfigValidator extends StdConverter<ApplicationConfig, ApplicationConfig> {
  static final Logger LOGGER = Logger.getLogger((ApplicationConfigValidator.class.getCanonicalName()));
  private List<String> inputJsonFiles = null;

  public ApplicationConfigValidator() {
  }

  public boolean isValidFileName(final String fileName) {
    final File aFile = new File(fileName);
    return (aFile.exists() && !aFile.isDirectory());
  }

  private void checkCycle(ApplicationConfig applicationConfig) {
    //check ApplicationConfig cycle
    final String infinite = "Infinite";
    String cycle = applicationConfig.getCycle();
    if (cycle == null) {
      applicationConfig.setCycle("1");
    }
    try {
      int count = Integer.parseInt(cycle);
      if (count < 0) {
        throw new NumberFormatException("Cycle must be positive number");
      }
    } catch (NumberFormatException e) {
      if (!cycle.equalsIgnoreCase(infinite)) {
        LOGGER.info("cycle in applicationConfig.yaml must be positive " +
            "number or Infinite");
        throw e;
      }
    }
  }

  private void checkInputJsonFile(ApplicationConfig applicationConfig) {
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
  }

  public ApplicationConfig convert(ApplicationConfig applicationConfig) {
    //check ApplicationConfig cycle
    checkCycle(applicationConfig);
    // check inputJsonFiles
    checkInputJsonFile(applicationConfig);
    return applicationConfig;
  }
}

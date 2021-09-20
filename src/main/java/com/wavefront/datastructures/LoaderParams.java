package com.wavefront.datastructures;

import com.beust.jcommander.Parameter;

public class LoaderParams {
  @Parameter(names = {"--configFile"}, description = "Location of application config yaml file.")
  private String appConfigFile = "applicationConfig.yaml";

  public String getAppConfigFile() {
    return appConfigFile;
  }
}

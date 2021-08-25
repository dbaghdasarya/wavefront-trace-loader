package com.wavefront;

import com.beust.jcommander.ParameterException;
import com.wavefront.config.ApplicationConfig;

import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AbstractTraceLoaderTest {
  private WavefrontTraceLoader wavefrontTraceLoader;
  private ApplicationConfig applicationConfig;
  private static final String PATTERN_FOR_TEST = "patternForTest.json";
  private static final String TOPOLOGY_FOR_TEST = "topologyForTest.json";
  private static final String TRACES_OUTPUT_FILE = "wftraces.txt";

  @Before
  public void setup() {
    applicationConfig = createMock(ApplicationConfig.class);
    wavefrontTraceLoader = new WavefrontTraceLoader(applicationConfig);
    expect(applicationConfig.getTraceOutputFile()).andReturn(TRACES_OUTPUT_FILE).anyTimes();
    expect(applicationConfig.getSpanOutputFile()).andReturn("").anyTimes();
    expect(applicationConfig.getWfTracesFile()).andReturn("").anyTimes();
    expect(applicationConfig.getReportStat()).andReturn(null).anyTimes();
  }


  private String loadFile(String fileName) {
    try {
      File newFile = new File(
          Objects.requireNonNull(getClass().getClassLoader().getResource(fileName)).toURI());
      return String.valueOf(newFile);
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
    return null;
  }

  public long countTracesBufferedReader(String fileName) {
    long lines = 0;
    try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
      String line;
      while ((line = reader.readLine()) != null)
        if (line.startsWith("data: {\"traceId\":\"") || line.startsWith(
            "[{\"root\":\""))
          lines++;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return lines;
  }

  @Test
  public void testByLinesWithCommandLineArgumentsOption() {
    expect(applicationConfig.getInputJsonFiles()).andReturn(Collections.emptyList()).anyTimes();

    replay(applicationConfig);

    wavefrontTraceLoader.start(new String[]{"-f", loadFile(PATTERN_FOR_TEST)});
    int traceCount = wavefrontTraceLoader.generatorConfig.getTotalTraceCount();
    assertEquals(traceCount, countTracesBufferedReader(applicationConfig.getTraceOutputFile()));

    verify(applicationConfig);
  }

  @Test
  public void testByLinesWithYamlConfigurationOptionWhenCommandLineArgumentsAreNotSpecified() {
    List<String> JsonFiles = Arrays.asList(loadFile(PATTERN_FOR_TEST), loadFile(PATTERN_FOR_TEST));
    expect(applicationConfig.getInputJsonFiles()).andReturn(JsonFiles).anyTimes();

    replay(applicationConfig);

    wavefrontTraceLoader.start(new String[]{});
    int traceCount = wavefrontTraceLoader.generatorConfig.getTotalTraceCount();
    assertEquals(traceCount * 2, countTracesBufferedReader(applicationConfig.getTraceOutputFile()));

    verify(applicationConfig);
  }

  @Test
  public void testByLinesWithYamlConfigurationOptionWhenCommandLineArgumentsAreSpecified() {
    List<String> jsonFiles = Arrays.asList(loadFile(PATTERN_FOR_TEST), loadFile(PATTERN_FOR_TEST));
    expect(applicationConfig.getInputJsonFiles()).andReturn(jsonFiles).anyTimes();

    replay(applicationConfig);

    wavefrontTraceLoader.start(new String[]{loadFile(TOPOLOGY_FOR_TEST)});
    int traceCount = wavefrontTraceLoader.generatorConfig.getTotalTraceCount();
    assertEquals(traceCount * 2, countTracesBufferedReader(applicationConfig.getTraceOutputFile()));

    verify(applicationConfig);
  }

  @Test
  public void testWhenNeitherCommandLineArgumentsNorYamlConfigurationFilesAreProvided() {
    expect(applicationConfig.getInputJsonFiles()).andReturn(Collections.emptyList()).anyTimes();
    boolean exception = false;

    replay(applicationConfig);

    try {
      wavefrontTraceLoader.parseArguments(new String[]{"-f"});
    } catch (ParameterException e) {
      exception = true;
    }
    assertTrue(exception);

    verify(applicationConfig);
  }
}
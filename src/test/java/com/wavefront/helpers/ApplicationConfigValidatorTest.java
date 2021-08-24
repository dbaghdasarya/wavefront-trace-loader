package com.wavefront.helpers;

import com.wavefront.config.ApplicationConfig;
import org.junit.Before;
import org.junit.Test;
import java.util.Collections;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertTrue;

public class ApplicationConfigValidatorTest {

  private final ApplicationConfig applicationConfig = createMock(ApplicationConfig.class);
  private ApplicationConfigValidator applicationConfigValidator;

  @Before
  public void setup() {
    applicationConfigValidator = new ApplicationConfigValidator();
  }

  @Test
  public void testCycleForNegativeNumbers() {
    boolean t = false;
    expect(applicationConfig.getInputJsonFiles()).andReturn(Collections.emptyList()).anyTimes();
    expect(applicationConfig.getCycle()).andReturn("-3");
    replay(applicationConfig);
    try {
      applicationConfigValidator.convert(applicationConfig);
    } catch (NumberFormatException e) {
      t = true;
    }
    verify(applicationConfig);
    assertTrue(t);
  }

  @Test
  public void testCycleValidateForInvalidString() {
    boolean t = false;
    expect(applicationConfig.getInputJsonFiles()).andReturn(Collections.emptyList()).anyTimes();
    expect(applicationConfig.getCycle()).andReturn("String");
    replay(applicationConfig);
    try {
      applicationConfigValidator.convert(applicationConfig);
    } catch (Exception e) {
      t = true;
    }
    verify(applicationConfig);
    assertTrue(t);
  }

  @Test
  public void testCycleValidateForPositiveNumber() {
    boolean t = true;
    expect(applicationConfig.getInputJsonFiles()).andReturn(Collections.emptyList()).anyTimes();
    expect(applicationConfig.getCycle()).andReturn("3");
    replay(applicationConfig);
    try {
      applicationConfigValidator.convert(applicationConfig);
    } catch (Exception e) {
      t = false;
    }
    verify(applicationConfig);
    assertTrue(t);
  }
}
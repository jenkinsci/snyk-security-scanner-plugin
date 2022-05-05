package io.snyk.jenkins.tools;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;

public class PlatformItemTest {

  @Test
  public void convert_shouldReturnNull_whenPlatformItemIsNull() {
    //noinspection ConstantConditions
    Platform platform = PlatformItem.convert(null);

    //noinspection ConstantConditions
    assertThat(platform, nullValue());
  }

  @Test
  public void convert_shouldReturnNull_whenPlatformItemIsAuto() {
    Platform platform = PlatformItem.convert(PlatformItem.AUTO);

    assertThat(platform, nullValue());
  }

  @Test
  public void convert_shouldReturnLINUX_whenPlatformItemIsLINUX() {
    Platform platform = PlatformItem.convert(PlatformItem.LINUX);

    assertThat(platform, notNullValue());
    assertThat(platform, equalTo(Platform.LINUX));
  }

  @Test
  public void convert_shouldReturnLINUX_ALPINE_whenPlatformItemIsLINUX_ALPINE() {
    Platform platform = PlatformItem.convert(PlatformItem.LINUX_ALPINE);

    assertThat(platform, notNullValue());
    assertThat(platform, equalTo(Platform.LINUX_ALPINE));
  }

  @Test
  public void convert_shouldReturnWINDOWS_whenPlatformItemIsWINDOWS() {
    Platform platform = PlatformItem.convert(PlatformItem.WINDOWS);

    assertThat(platform, notNullValue());
    assertThat(platform, equalTo(Platform.WINDOWS));
  }
}

package io.github.djfdyuruiry.jmeter.azure;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.stream.Stream;

import static io.github.djfdyuruiry.jmeter.azure.SampleResultField.*;
import static java.time.Duration.ofMinutes;
import static java.time.LocalDateTime.now;

import com.microsoft.applicationinsights.telemetry.Duration;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.samplers.SampleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

public class SampleResultToTelemetryMapperTests {
  private static final long SAMPLE_TIME_IN_EPOCH_MS =
      now().minus(ofMinutes(10))
          .toInstant(ZoneOffset.UTC)
          .toEpochMilli();

  private SampleResultToTelemetryMapper mapper;

  private HTTPSampleResult httpResult;

  @BeforeEach
  public void setup() throws MalformedURLException {
    mapper = new SampleResultToTelemetryMapper();

    httpResult = new HTTPSampleResult();

    // generic attributes
    httpResult.setSampleLabel("GET duckduckgo homepage");
    httpResult.setThreadName("GET duckduckgo homepage 1");
    httpResult.setGroupThreads(1);
    httpResult.setAllThreads(1);
    httpResult.setSampleCount(1);

    httpResult.setURL(new URL("https://duckduckgo.com"));
    httpResult.setQueryString("useless=data");
    httpResult.setRequestHeaders("some request headers");

    httpResult.setConnectTime(10);
    httpResult.setIdleTime(20);
    httpResult.setLatency(10);
    httpResult.setErrorCount(0);

    httpResult.setResponseCode("200");
    httpResult.setSuccessful(true);
    httpResult.setResponseHeaders("some response headers");
    httpResult.setResponseData("some nonsense", "utf8");
    httpResult.setContentType("text/html");

    httpResult.setStampAndTime(SAMPLE_TIME_IN_EPOCH_MS, 40);

    // HTTP only attributes
    httpResult.setHTTPMethod("GET");
    httpResult.setCookies("some-cookie=value");
    httpResult.setRedirectLocation("some redirect location");
  }

  @Test
  public void when_map_called_with_sample_result_then_request_timestamp_is_set() {
    var telemetry = mapper.map("some request name", buildSampleResult());

    assertEquals(new Date(SAMPLE_TIME_IN_EPOCH_MS), telemetry.getTimestamp());
  }

  @Test
  public void when_map_called_with_sample_result_then_request_duration_is_set() {
    var telemetry = mapper.map("some request name", buildSampleResult());

    assertEquals(new Duration(40), telemetry.getDuration());
  }

  @Test
  public void when_map_called_with_sample_result_and_error_count_is_zero_then_success_flag_is_true() {
    var telemetry = mapper.map("some request name", buildSampleResult());

    assertTrue(telemetry.isSuccess());
  }

  @Test
  public void when_map_called_with_sample_result_and_error_count_greater_than_zero_then_success_flag_is_false() {
    httpResult.setSuccessful(false);

    var telemetry = mapper.map("some request name", buildSampleResult());

    assertFalse(telemetry.isSuccess());
  }

  @Test
  public void when_map_called_with_sample_result_then_request_name_is_set() {
    var telemetry = mapper.map("some request name", buildSampleResult());

    assertEquals("some request name", telemetry.getName());
  }

  @Test
  public void when_map_called_with_sample_result_then_response_code_is_set() {
    var telemetry = mapper.map("some request name", buildSampleResult());

    assertEquals("200", telemetry.getResponseCode());
  }

  @ParameterizedTest(name = "when_map_called_with_result_then_result_{0}_field_is_copied_to_property")
  @MethodSource("getResultFields")
  public void when_map_called_with_result_then_result_field_is_copied_to_property(
      String propertyName,
      String propertyValue
  ) {
    enableAllResultFields();

    var telemetry = mapper.map("some request name", buildSampleResult());

    assertEquals(propertyValue, telemetry.getProperties().get(propertyName));
  }

  @ParameterizedTest(name = "when_map_called_with_http_result_then_result_{0}_field_is_copied_to_property")
  @MethodSource("getHttpResultFields")
  public void when_map_called_with_http_result_then_result_field_is_copied_to_property(
      String propertyName,
      String propertyValue
  ) {
    enableAllResultFields();

    var telemetry = mapper.map("some request name", httpResult);

    assertEquals(propertyValue, telemetry.getProperties().get(propertyName));
  }

  @Test
  public void when_map_called_with_http_result_and_request_url_field_is_enabled_then_url_is_set() throws MalformedURLException {
    mapper.enableResultField(REQUEST_URL);

    var telemetry = mapper.map("some request name", httpResult);

    assertEquals(new URL("https://duckduckgo.com"), telemetry.getUrl());
  }

  @Test
  public void when_map_called_with_http_result_and_http_method_field_enabled_then_request_method_is_set() {
    mapper.enableResultField(HTTP_METHOD);

    var telemetry = mapper.map("some request name", httpResult);

    assertEquals("GET", telemetry.getHttpMethod());
  }


  @Test
  public void when_map_called_with_result_and_custom_properties_added_then_properties_are_copied() {
    mapper.addCustomProperty("harvey", "scorpy");
    mapper.addCustomProperty("sun", "dargo");

    var telemetry = mapper.map("some request name", httpResult);

    assertEquals("scorpy", telemetry.getProperties().get("harvey"));
    assertEquals("dargo", telemetry.getProperties().get("sun"));
  }

  private SampleResult buildSampleResult() {
    return new SampleResult(httpResult);
  }

  private static Stream<Arguments> getResultFields() {
    return Stream.of(
        Arguments.of("Bytes", "13"),
        Arguments.of("SentBytes", "0"),
        Arguments.of("ConnectTime", "10"),
        Arguments.of("ErrorCount", "0"),
        Arguments.of("IdleTime", "20"),
        Arguments.of("Latency", "10"),
        Arguments.of("BodySize", "13"),
        Arguments.of("ContentType", "text/html"),
        Arguments.of("MediaType", "text/html"),
        Arguments.of("TestStartTime", Long.toString(SAMPLE_TIME_IN_EPOCH_MS - 40)),
        Arguments.of("SampleStartTime", Long.toString(SAMPLE_TIME_IN_EPOCH_MS- 40)),
        Arguments.of("SampleEndTime", Long.toString(SAMPLE_TIME_IN_EPOCH_MS)),
        Arguments.of("SampleLabel", "GET duckduckgo homepage"),
        Arguments.of("ThreadName", "GET duckduckgo homepage 1"),
        Arguments.of("GroupThreads", "1"),
        Arguments.of("AllThreads", "1"),
        Arguments.of("SampleCount", "1")
    );
  }

  private static Stream<Arguments> getHttpResultFields() {
    return Stream.concat(
        getResultFields(),
        Stream.of(
            Arguments.of("HttpUrl", "https://duckduckgo.com"),
            Arguments.of("HttpMethod", "GET"),
            Arguments.of("HttpRequestHeaders", "some request headers"),
            Arguments.of("HttpResponseHeaders", "some response headers"),
            Arguments.of("HttpCookies", "some-cookie=value"),
            Arguments.of("HttpQueryString", "useless=data"),
            Arguments.of("HttpRedirect", "some redirect location")
        )
    );
  }

  private void enableAllResultFields() {
    for (var f : SampleResultField.values()) {
      mapper.enableResultField(f);
    }
  }
}

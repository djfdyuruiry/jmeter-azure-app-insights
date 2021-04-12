package io.github.djfdyuruiry.jmeter.azure;

import java.util.UUID;

import static java.time.LocalTime.now;
import static java.util.Collections.singletonList;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static io.github.djfdyuruiry.jmeter.azure.SampleResultField.*;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;

public class AppInsightsListenerTests {
  private static final String INSTRUMENTATION_KEY = UUID.randomUUID().toString();

  private TelemetryClient telemetryClient;
  private boolean quickPulseInitialiserCalled;
  private SampleResultToTelemetryMapper resultMapper;

  private AppInsightsListener listener;

  private Arguments jmeterArguments;
  private TelemetryConfiguration configPassedToTelemetryFactory;
  private TelemetryConfiguration configPassedToQuickPulseInitialiser;

  @BeforeEach
  public void setup() {
    telemetryClient = mock(TelemetryClient.class);
    resultMapper = mock(SampleResultToTelemetryMapper.class);

    when(resultMapper.map(anyString(), any(SampleResult.class))).thenAnswer(c -> new RequestTelemetry());

    listener = new AppInsightsListener(
        c -> {
          configPassedToTelemetryFactory = c;

          return telemetryClient;
        },
        c -> {
          configPassedToQuickPulseInitialiser = c;
          quickPulseInitialiserCalled = true;
        },
        resultMapper
    );

    jmeterArguments = new Arguments();

    jmeterArguments.addArgument("testName", String.format("custom-test-52-%s", now()));
    jmeterArguments.addArgument("instrumentationKey", INSTRUMENTATION_KEY);
    jmeterArguments.addArgument("resultFields", "RequestUrl;httpUrl;httpmethod");
  }

  @Test
  public void when_setupTest_isCalled_then_instrumentationKey_isSet_in_telemetry_client_config() {
    listener.setupTest(buildContext());

    assertEquals(INSTRUMENTATION_KEY, configPassedToTelemetryFactory.getInstrumentationKey());
  }

  @Test
  public void when_setupTest_isCalled_and_live_metrics_are_not_enabled_then_quick_pulse_is_initialised() {
    jmeterArguments.addArgument("liveMetrics", "false");

    listener.setupTest(buildContext());

    assertFalse(quickPulseInitialiserCalled);
  }

  @Test
  public void when_setupTest_isCalled_and_live_metrics_are_enabled_then_quick_pulse_is_initialised() {
    jmeterArguments.addArgument("liveMetrics", "true");

    listener.setupTest(buildContext());

    assertTrue(quickPulseInitialiserCalled);
  }

  @Test
  public void when_setupTest_isCalled_and_live_metrics_are_enabled_then_instrumentationKey_isSet_in_quick_pulse_config() {
    jmeterArguments.addArgument("liveMetrics", "true");

    listener.setupTest(buildContext());

    assertEquals(INSTRUMENTATION_KEY, configPassedToQuickPulseInitialiser.getInstrumentationKey());
  }

  @Test
  public void when_setupTest_isCalled_and_custom_properties_are_defined_then_properties_are_passed_to_result_mapper() {
    jmeterArguments.addArgument("ai.test-metric", "reeeee");
    jmeterArguments.addArgument("ai.location", "the moon");

    listener.setupTest(buildContext());

    verify(resultMapper, times(1)).addCustomProperty(eq("test-metric"), eq("reeeee"));
    verify(resultMapper, times(1)).addCustomProperty(eq("location"), eq("the moon"));
  }

  @Test
  public void when_setupTest_isCalled_then_mapper_fields_specified_in_jmeter_args_are_enabled() {
    listener.setupTest(buildContext());

    verify(resultMapper, times(1)).enableResultField(eq(REQUEST_URL));
    verify(resultMapper, times(1)).enableResultField(eq(HTTP_URL));
    verify(resultMapper, times(1)).enableResultField(eq(HTTP_METHOD));
  }

  @Test
  public void when_handleSampleResults_isCalled_then_result_mapper_isCalled() {
    var context = buildContext();

    runSampleTest();

    verify(resultMapper, times(1)).map(anyString(), any(SampleResult.class));
  }

  @Test
  public void when_handleSampleResults_isCalled_then_telemetry_client_isCalled() {
    var context = buildContext();

    runSampleTest();

    verify(telemetryClient, times(1)).trackRequest(any(RequestTelemetry.class));
  }

  @Test
  public void when_handleSampleResults_isCalled_and_sampler_is_enabled_then_telemetry_client_isCalled() {
    jmeterArguments.addArgument("samplersList", "Hunky Dory;Malorn;Farbot");

    runSampleTest("Farbot");

    verify(telemetryClient, times(1)).trackRequest(any(RequestTelemetry.class));
  }

  @Test
  public void when_handleSampleResults_isCalled_and_sampler_is_enabled_but_label_does_not_match_then_telemetry_client_isCalled() {
    jmeterArguments.addArgument("samplersList", "Hunky Dory;Pool Fargate 90;Farbot");

    runSampleTest("Crantue");

    verify(telemetryClient, never()).trackRequest(any(RequestTelemetry.class));
  }

  @Test
  public void when_handleSampleResults_isCalled_and_sampler_is_enabled_using_regex_then_telemetry_client_isCalled() {
    jmeterArguments.addArgument("samplersList", "(Hunky Dory|Farbot \\d+$)");
    jmeterArguments.addArgument("useRegexForSamplerList", "true");

    runSampleTest("Farbot 34");

    verify(telemetryClient, times(1)).trackRequest(any(RequestTelemetry.class));
  }

  @Test
  public void when_handleSampleResults_isCalled_and_sampler_is_enabled_using_regex_but_does_not_match_label_then_telemetry_client_isCalled() {
    jmeterArguments.addArgument("samplersList", "(Farbot \\d+$|Hunky Dory|Twenty Floors)");
    jmeterArguments.addArgument("useRegexForSamplerList", "true");

    runSampleTest("Farbot Khan");

    verify(telemetryClient, never()).trackRequest(any(RequestTelemetry.class));
  }

  @Test
  public void when_handleSampleResults_isCalled_and_sampler_is_not_enabled_then_telemetry_client_isCalled() {
    jmeterArguments.addArgument("samplersList", "Malorn;Farbot");

    runSampleTest("Carpet Line");

    verify(telemetryClient, never()).trackRequest(any(RequestTelemetry.class));
  }

  private BackendListenerContext buildContext() {
    return new BackendListenerContext(jmeterArguments);
  }

  private void runSampleTest() {
    runSampleTest(buildContext());
  }

  private void runSampleTest(String sampleLabel) {
    runSampleTest(sampleLabel, buildContext());
  }

  private void runSampleTest(BackendListenerContext context) {
    runSampleTest(UUID.randomUUID().toString(), context);
  }

  private void runSampleTest(String sampleLabel, BackendListenerContext context) {
    listener.setupTest(context);

    listener.handleSampleResults(
        singletonList(
            new SampleResult() {{
              setSampleLabel(sampleLabel);
            }}
        ),
        context
    );
  }
}

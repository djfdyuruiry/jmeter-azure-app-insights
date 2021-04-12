package io.github.djfdyuruiry.jmeter.azure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.function.Predicate.not;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.internal.quickpulse.QuickPulse;

@SuppressWarnings("NullableProblems")
public class AppInsightsListener extends AbstractBackendListenerClient {
  private static final String KEY_TEST_NAME = "testName";
  private static final String KEY_INSTRUMENTATION_KEY = "instrumentationKey";
  private static final String KEY_RESULT_FIELDS = "resultFields";
  private static final String KEY_LIVE_METRICS = "liveMetrics";
  private static final String KEY_SAMPLERS_LIST = "samplersList";
  private static final String KEY_USE_REGEX_FOR_SAMPLER_LIST = "useRegexForSamplerList";
  private static final String KEY_CUSTOM_PROPERTIES_PREFIX = "ai.";

  private static final String DEFAULT_TEST_NAME = "jmeter";
  private static final String DEFAULT_INSTRUMENTATION_KEY = "";
  private static final boolean DEFAULT_LIVE_METRICS = true;
  private static final String DEFAULT_SAMPLERS_LIST = "";
  private static final boolean DEFAULT_USE_REGEX_FOR_SAMPLER_LIST = false;

  private static final Logger LOG = LoggerFactory.getLogger(AppInsightsListener.class);

  private static final String SEPARATOR = ";";

  private final Function<TelemetryConfiguration, TelemetryClient> telemetryClientFactory;
  private final Consumer<TelemetryConfiguration> quickPulseInitialiser;
  private final SampleResultToTelemetryMapper resultMapper;

  private String testName;
  private String samplersList = "";
  private Boolean useRegexForSamplerList;
  private Set<String> samplersToFilter;

  private TelemetryClient telemetryClient;

  /**
   * Constructor called by JMeter when loading this plugin.
   */
  public AppInsightsListener() {
    super();
    telemetryClientFactory = TelemetryClient::new;
    quickPulseInitialiser = QuickPulse.INSTANCE::initialize;

    resultMapper = new SampleResultToTelemetryMapper();
  }

  /**
   * Constructor for unit testing.
   */
  public AppInsightsListener(
      Function<TelemetryConfiguration, TelemetryClient> telemetryClientFactory,
      Consumer<TelemetryConfiguration> quickPulseInitialiser,
      SampleResultToTelemetryMapper resultMapper
  ) {
    super();

    this.telemetryClientFactory = telemetryClientFactory;
    this.quickPulseInitialiser = quickPulseInitialiser;
    this.resultMapper = resultMapper;
  }

  @Override
  public Arguments getDefaultParameters() {
    var arguments = new Arguments();

    arguments.addArgument(KEY_TEST_NAME, DEFAULT_TEST_NAME);
    arguments.addArgument(KEY_INSTRUMENTATION_KEY, DEFAULT_INSTRUMENTATION_KEY);
    arguments.addArgument(KEY_LIVE_METRICS, Boolean.toString(DEFAULT_LIVE_METRICS));
    arguments.addArgument(KEY_SAMPLERS_LIST, DEFAULT_SAMPLERS_LIST);
    arguments.addArgument(KEY_USE_REGEX_FOR_SAMPLER_LIST, Boolean.toString(DEFAULT_USE_REGEX_FOR_SAMPLER_LIST));

    return arguments;
  }

  private void initialiseLiveMetricsIfEnabled(BackendListenerContext context, TelemetryConfiguration config) {
    var liveMetricsEnabled = context.getBooleanParameter(KEY_LIVE_METRICS, DEFAULT_LIVE_METRICS);

    if (!liveMetricsEnabled) {
      LOG.warn("Live metrics are disabled");

      return;
    }

    LOG.info("Init live metrics start");

    quickPulseInitialiser.accept(config);

    LOG.info("Init live metrics finish");
  }

  private void initialiseTelemetryClient(BackendListenerContext context) {
    LOG.info("Init telemetry client start");

    var instrumentationKey = context.getParameter(KEY_INSTRUMENTATION_KEY);

    var config = TelemetryConfiguration.createDefault();
    config.setInstrumentationKey(instrumentationKey);

    telemetryClient = telemetryClientFactory.apply(config);

    initialiseLiveMetricsIfEnabled(context, config);

    LOG.info("Init telemetry client finish");
  }

  private void loadCustomProperties(BackendListenerContext context) {
    Iterable<String> paramNames = context::getParameterNamesIterator;

    StreamSupport.stream((paramNames).spliterator(), false)
        .filter(p -> p.startsWith(KEY_CUSTOM_PROPERTIES_PREFIX))
        .forEach(p ->
            resultMapper.addCustomProperty(
                p.replace(KEY_CUSTOM_PROPERTIES_PREFIX, EMPTY),
                context.getParameter(p)
            )
        );
  }

  private void loadRequestFields(BackendListenerContext context) {
    var resultFieldsCsv = context.getParameter(KEY_RESULT_FIELDS, EMPTY);

    if (isBlank(resultFieldsCsv)) {
      return;
    }

    Arrays.stream(resultFieldsCsv.trim().split(SEPARATOR))
        .filter(not(StringUtils::isEmpty))
        .map(SampleResultField::parse)
        .forEach(resultMapper::enableResultField);
  }

  private void loadParameters(BackendListenerContext context) {
    LOG.info("Loading plugin parameters start");

    testName = context.getParameter(KEY_TEST_NAME, DEFAULT_TEST_NAME);

    loadRequestFields(context);

    samplersList = context.getParameter(KEY_SAMPLERS_LIST, DEFAULT_SAMPLERS_LIST).trim();
    useRegexForSamplerList = context.getBooleanParameter(
        KEY_USE_REGEX_FOR_SAMPLER_LIST,
        DEFAULT_USE_REGEX_FOR_SAMPLER_LIST
    );

    samplersToFilter = useRegexForSamplerList ?
        new HashSet<>() :
        Arrays.stream(samplersList.split(SEPARATOR)).collect(Collectors.toSet());

    LOG.info("Loading plugin parameters finish");
  }
  @Override
  public void setupTest(BackendListenerContext context) {
    LOG.info("Setup start");

    loadParameters(context);
    loadCustomProperties(context);
    initialiseTelemetryClient(context);

    LOG.info("Setup finish");
  }

  private void trackRequest(String requestName, SampleResult jmeterRequest) {
    LOG.debug("Sending result to Azure AppInsights");

    telemetryClient.trackRequest(
        resultMapper.map(requestName, jmeterRequest)
    );

    LOG.debug("Result sent to Azure AppInsights");
  }

  @Override
  public void handleSampleResults(List<SampleResult> results, BackendListenerContext context) {
    for (SampleResult sr : results) {
      if (!samplersList.isEmpty()) {
        var samplerShouldBeTracked = useRegexForSamplerList ?
            sr.getSampleLabel().matches(samplersList) :
            samplersToFilter.contains(sr.getSampleLabel());

        if (!samplerShouldBeTracked) {
          continue;
        }
      }

      trackRequest(testName, sr);
    }
  }

  @Override
  public void teardownTest(BackendListenerContext context) throws Exception {
    LOG.warn("Stopping Azure AppInsights Listener");

    samplersToFilter.clear();
    telemetryClient.flush();
    super.teardownTest(context);

    LOG.warn("Azure AppInsights Listener Stopped");
  }
}

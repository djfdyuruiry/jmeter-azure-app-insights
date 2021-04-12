package io.github.djfdyuruiry.jmeter.azure;

import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.jmeter.samplers.SampleResult;

import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;

import static io.github.djfdyuruiry.jmeter.azure.SampleResultField.HTTP_METHOD;
import static io.github.djfdyuruiry.jmeter.azure.SampleResultField.REQUEST_URL;

class SampleResultToTelemetryMapper {
  private final Map<SampleResultField, SampleResultFieldExtractor> fieldExtractors;
  private final Map<String, String> customProperties;
  private final SampleResultFieldExtractorFactory extractorFactory;

  SampleResultToTelemetryMapper() {
    fieldExtractors = new HashMap<>();
    customProperties = new HashMap<>();
    extractorFactory = new SampleResultFieldExtractorFactory();
  }

  public void addCustomProperty(String name, String value) {
    customProperties.put(name, value);
  }

  public void enableResultField(SampleResultField field) {
    fieldExtractors.put(
        field,
        extractorFactory.getExtractorForField(field)
    );
  }

  private void setTelemetryValueForResultField(SampleResultField field, Object value, RequestTelemetry telemetry) {
    if (field == REQUEST_URL && value instanceof URL) {
      telemetry.setUrl((URL) value);
      return;
    }

    if (field == HTTP_METHOD) {
      telemetry.setHttpMethod(value.toString());
    }

    telemetry.getProperties()
        .put(field.toString(), value.toString());
  }

  private void mapResultFields(SampleResult jmeterRequest, RequestTelemetry telemetry) {
    telemetry.setResponseCode(jmeterRequest.getResponseCode());

    fieldExtractors.keySet()
        .forEach(f -> {
          var extractor = fieldExtractors.get(f);

          extractor.getValueFrom(jmeterRequest)
              .ifPresent(v ->
                  setTelemetryValueForResultField(f, v, telemetry)
              );
        });

    customProperties.keySet()
        .forEach(p ->
            telemetry.getProperties().put(p, customProperties.get(p))
        );
  }

  public RequestTelemetry map(String requestName, SampleResult jmeterRequest) {
    var timestamp = new Date(jmeterRequest.getTimeStamp());
    var duration = new Duration(jmeterRequest.getTime());

    var telemetry = new RequestTelemetry(
        requestName,
        timestamp,
        duration,
        jmeterRequest.getResponseCode(),
        jmeterRequest.getErrorCount() == 0
    );

    telemetry.getContext()
        .getOperation()
        .setName(requestName);

    mapResultFields(jmeterRequest, telemetry);

    return telemetry;
  }
}

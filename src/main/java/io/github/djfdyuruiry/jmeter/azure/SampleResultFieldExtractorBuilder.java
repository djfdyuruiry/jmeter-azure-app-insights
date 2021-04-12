package io.github.djfdyuruiry.jmeter.azure;

import java.util.Optional;
import java.util.function.Function;

import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.samplers.SampleResult;

class SampleResultFieldExtractorBuilder {
  private SampleResultFieldExtractorBuilder() {
  }

  public static SampleResultFieldExtractor extractor(Function<SampleResult, Object> extractor) {
    return r -> Optional.ofNullable(extractor.apply(r));
  }

  public static SampleResultFieldExtractor httpExtractor(Function<HTTPSampleResult, Object> extractor) {
    return sr -> sr instanceof HTTPSampleResult
        ? Optional.ofNullable(extractor.apply((HTTPSampleResult)sr))
        : Optional.empty();
  }
}

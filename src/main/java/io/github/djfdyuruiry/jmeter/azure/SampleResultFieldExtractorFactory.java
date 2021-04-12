package io.github.djfdyuruiry.jmeter.azure;

import java.util.Map;

import static java.util.Map.entry;

import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.samplers.SampleResult;

import static org.apache.commons.lang3.StringUtils.isBlank;

import static io.github.djfdyuruiry.jmeter.azure.SampleResultField.*;
import static io.github.djfdyuruiry.jmeter.azure.SampleResultFieldExtractorBuilder.extractor;
import static io.github.djfdyuruiry.jmeter.azure.SampleResultFieldExtractorBuilder.httpExtractor;
import static org.apache.commons.lang3.StringUtils.isEmpty;

class SampleResultFieldExtractorFactory {
  private static final String URL_PLACEHOLDER = "<URL MISSING>";
  private static final String METHOD_PLACEHOLDER = "<HTTP METHOD MISSING>";

  private final Map<SampleResultField, SampleResultFieldExtractor> extractorRegistry;

  public SampleResultFieldExtractorFactory() {
    extractorRegistry = Map.ofEntries(
        entry(REQUEST_URL, extractor(SampleResult::getURL)),
        entry(SENT_BYTES, extractor(SampleResult::getSentBytes)),
        entry(BYTES, extractor(SampleResult::getBytesAsLong)),
        entry(CONNECT_TIME, extractor(SampleResult::getConnectTime)),
        entry(ERROR_COUNT, extractor(SampleResult::getErrorCount)),
        entry(IDLE_TIME, extractor(SampleResult::getIdleTime)),
        entry(LATENCY, extractor(SampleResult::getLatency)),
        entry(BODY_SIZE, extractor(SampleResult::getBodySizeAsLong)),
        entry(CONTENT_TYPE, extractor(SampleResult::getContentType)),
        entry(MEDIA_TYPE, extractor(SampleResult::getMediaType)),
        entry(TEST_START_TIME, extractor(SampleResult::getStartTime)),
        entry(SAMPLE_START_TIME, extractor(SampleResult::getStartTime)),
        entry(SAMPLE_END_TIME, extractor(SampleResult::getEndTime)),
        entry(SAMPLE_LABEL, extractor(SampleResult::getSampleLabel)),
        entry(THREAD_NAME, extractor(SampleResult::getThreadName)),
        entry(GROUP_THREADS, extractor(SampleResult::getGroupThreads)),
        entry(ALL_THREADS, extractor(SampleResult::getAllThreads)),
        entry(SAMPLE_COUNT, extractor(SampleResult::getSampleCount)),
        entry(HTTP_URL, httpExtractor(hsr -> {
          var jmeterUrl = hsr.getUrlAsString();

          return !isEmpty(jmeterUrl)
            ? jmeterUrl
            : URL_PLACEHOLDER;
        })),
        entry(HTTP_METHOD, httpExtractor(hsr-> {
          var jmeterHttpMethod = hsr.getHTTPMethod();

          return !isBlank(jmeterHttpMethod) ?
              jmeterHttpMethod:
              METHOD_PLACEHOLDER;
        }) ),
        entry(HTTP_REQUEST_HEADERS, httpExtractor(HTTPSampleResult::getRequestHeaders)),
        entry(HTTP_RESPONSE_HEADERS, httpExtractor(HTTPSampleResult::getResponseHeaders)),
        entry(HTTP_COOKIES, httpExtractor(HTTPSampleResult::getCookies)),
        entry(HTTP_QUERY_STRING, httpExtractor(HTTPSampleResult::getQueryString)),
        entry(HTTP_REDIRECT, httpExtractor(HTTPSampleResult::getRedirectLocation))
    );
  }

  public SampleResultFieldExtractor getExtractorForField(SampleResultField field) {
    return extractorRegistry.get(field);
  }
}

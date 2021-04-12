package io.github.djfdyuruiry.jmeter.azure;

import static java.lang.String.format;

import static org.apache.commons.lang3.StringUtils.isBlank;

enum SampleResultField {
    REQUEST_URL("RequestUrl"),
    BYTES("Bytes"),
    SENT_BYTES("SentBytes"),
    CONNECT_TIME("ConnectTime"),
    ERROR_COUNT("ErrorCount"),
    IDLE_TIME("IdleTime"),
    LATENCY("Latency"),
    BODY_SIZE("BodySize"),
    CONTENT_TYPE("ContentType"),
    MEDIA_TYPE("MediaType"),
    TEST_START_TIME("TestStartTime"),
    SAMPLE_START_TIME("SampleStartTime"),
    SAMPLE_END_TIME("SampleEndTime"),
    SAMPLE_LABEL("SampleLabel"),
    THREAD_NAME("ThreadName"),
    GROUP_THREADS("GroupThreads"),
    ALL_THREADS("AllThreads"),
    SAMPLE_COUNT("SampleCount"),
    HTTP_URL("HttpUrl"),
    HTTP_METHOD("HttpMethod"),
    HTTP_REQUEST_HEADERS("HttpRequestHeaders"),
    HTTP_RESPONSE_HEADERS("HttpResponseHeaders"),
    HTTP_COOKIES("HttpCookies"),
    HTTP_QUERY_STRING("HttpQueryString"),
    HTTP_REDIRECT("HttpRedirect");

    public static SampleResultField parse(String stringValue) {
        if (isBlank(stringValue)) {
            throw new IllegalArgumentException("Sample result field name was blank");
        }

        var lowerStringValue = stringValue.toLowerCase().trim();

        for (var f : SampleResultField.values()) {
            if (f.toString().toLowerCase().equals(lowerStringValue)) {
                return f;
            }
        }

        throw new IllegalArgumentException(
            format("Unrecognised sample result field name provided: %s", stringValue)
        );
    }

    private String fieldName;

    private SampleResultField(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String toString() {
        return fieldName;
    }
}

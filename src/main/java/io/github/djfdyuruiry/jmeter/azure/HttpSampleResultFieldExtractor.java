package io.github.djfdyuruiry.jmeter.azure;

import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;

import java.util.Optional;

@FunctionalInterface
interface HttpSampleResultFieldExtractor {
    public Optional<Object> extract(HTTPSampleResult r);
}

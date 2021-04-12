package io.github.djfdyuruiry.jmeter.azure;

import org.apache.jmeter.samplers.SampleResult;

import java.util.Optional;

@FunctionalInterface
interface SampleResultFieldExtractor {
    public Optional<Object> getValueFrom(SampleResult r);
}

package com.lightstep.otel;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Labels;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.Context;
import io.opentelemetry.exporter.otlp.OtlpGrpcSpanExporter;
import io.opentelemetry.exporter.otlp.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.MeterSdkProvider;
import io.opentelemetry.sdk.metrics.export.IntervalMetricReader;
import io.opentelemetry.sdk.trace.TracerSdkManagement;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.LongValueObserver;
import io.opentelemetry.api.metrics.LongSumObserver;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Span.Kind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.oshi.SystemMetrics;
import java.util.Collections;
import java.util.Random;

public class App 
{
  static final String TOKEN = System.getenv("ACCESS_TOKEN");
  static final Random RAND = new Random();

  public static void main( String[] args ) throws Exception
  {
    System.setProperty("otel.resource.attributes", "service.name=metrics_example,service.version=0.1");

    OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter.builder()
      .addHeader("lightstep-access-token", TOKEN)
      .setEndpoint("ingest.staging.lightstep.com:443")
      .setDeadlineMs(30000)
      .setUseTls(true)
      .build();
    OpenTelemetrySdk.getGlobalTracerManagement()
      .addSpanProcessor(SimpleSpanProcessor.builder(spanExporter).build());

    OtlpGrpcMetricExporter metricExporter = OtlpGrpcMetricExporter.builder()
      .addHeader("lightstep-access-token", TOKEN)
      .setEndpoint("ingest.staging.lightstep.com:443")
      .setDeadlineMs(30000)
      .setUseTls(true)
      .build();
    IntervalMetricReader intervalMetricReader =
      IntervalMetricReader.builder()
      .setMetricExporter(metricExporter)
      .setMetricProducers(
          Collections.singleton(OpenTelemetrySdk.getGlobalMeterProvider().getMetricProducer()))
      .setExportIntervalMillis(500)
      .build();

    Tracer tracer =
      OpenTelemetry.getGlobalTracerProvider().get("LightstepExample");
    Meter meter = OpenTelemetry.getGlobalMeter("LightstepExample");

    LongCounter counter = meter.longCounterBuilder("long_counter").build();
    DoubleCounter counter2 = meter.doubleCounterBuilder("double_counter").build();
    LongValueObserver observer = meter.longValueObserverBuilder("long_observer").build();
    observer.setCallback((result) -> {
      result.observe(RAND.nextInt(10) + 1, Labels.of("hello", "foo"));
    });
    LongSumObserver observer2 = meter.longSumObserverBuilder("long_sum_observer").build();
    observer2.setCallback((result) -> {
      result.observe(RAND.nextInt(5) + 1, Labels.of("hey", "sweet"));
    });

    // Register metrics *after* initializing the SDK :(
    SystemMetrics.registerObservers();

    // Send one thousand Spans.
    for (int i = 1; i <= 10000; i++) {
      Span exampleSpan = tracer.spanBuilder("sample_span").startSpan();
      try (Scope scope = Context.current().with(exampleSpan).makeCurrent()) {
        counter.add(1);
        counter2.add(1.01);
        exampleSpan.setAttribute("stringValue", "foo");
        exampleSpan.setAttribute("longValue", i);

        Thread.sleep(RAND.nextInt(500));
      } finally {
        exampleSpan.end();
      }

      if (i % 50 == 0) {
        System.out.println("Sent " + i + " spans");
      }
    }

    System.out.println("Shutting down...");
    intervalMetricReader.shutdown();
    OpenTelemetrySdk.getGlobalTracerManagement().shutdown();
  }
}

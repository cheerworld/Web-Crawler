package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
public final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  public ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    // Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
    //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
    //       See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.

    // Check if the wrapped interface contains a @Profiled method
    boolean hasProfiledMethod = Arrays.stream(klass.getMethods())
            .anyMatch(method -> method.isAnnotationPresent(Profiled.class));

    if (!hasProfiledMethod) {
      throw new IllegalArgumentException("Wrapped interface does not contain a @Profiled method");
    }

    // Create an instance of the ProfilingMethodInterceptor
    ProfilingMethodInterceptor interceptor = new ProfilingMethodInterceptor(clock, delegate, state);

    // Create a dynamic proxy using the Proxy.newProxyInstance method
    return (T) Proxy.newProxyInstance(klass.getClassLoader(), new Class[]{klass}, interceptor);
  }

  @Override
  public void writeData(Path path) throws IOException {
    try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      PrintWriter writer = new PrintWriter(bufferedWriter);
      writeData(writer);
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}

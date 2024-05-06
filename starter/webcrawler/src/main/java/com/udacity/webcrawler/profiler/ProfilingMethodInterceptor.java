package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object delegate;
  private final ProfilingState state;


  ProfilingMethodInterceptor(Clock clock, Object delegate, ProfilingState state) {
    this.clock = Objects.requireNonNull(clock);
    this.delegate = delegate;
    this.state = state;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.
    if(method.isAnnotationPresent(Profiled.class)) {
      long startTime = clock.millis();
      try {
        Object result = method.invoke(delegate, args);
        return result;
      } catch (InvocationTargetException e) {
        throw e.getTargetException(); // Rethrow the original exception thrown by the method
      } finally {
        long endTime = clock.millis();
        state.record(delegate.getClass(), method, Duration.ofMillis(endTime - startTime));
      }
    } else {
      return method.invoke(delegate, args);
    }
  }
}
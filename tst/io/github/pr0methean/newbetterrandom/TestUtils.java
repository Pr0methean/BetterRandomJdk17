package io.github.pr0methean.newbetterrandom;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;

public enum TestUtils {
  ;

  /**
   * Reflectively calls all public constructors, or all public and protected constructors, of the
   * given class with the given parameters. Passes each constructed instance to a consumer.
   *
   * @param <T> {@code clazz} as a type.
   * @param clazz The class whose constructors are to be tested.
   * @param includeProtected Whether to test protected constructors
   * @param params A map of parameter types to values.
   * @param test The consumer to pass the instances to.
   */
  public static <T> void testConstructors(final Class<? extends T> clazz,
                                          final boolean includeProtected, final Map<Class<?>, Object> params,
                                          final Consumer<? super T> test) {
    testConstructors(includeProtected, params, test,
        Arrays.asList(clazz.getDeclaredConstructors()));
  }

  /**
   * Reflectively calls all public constructors, or all public and protected constructors, of the
   * given class with the given parameters. Passes each constructed instance to a consumer.
   *
   * @param <T> the type of object being constructed
   * @param includeProtected Whether to test protected constructors
   * @param params A map of parameter types to values.
   * @param test The consumer to pass the instances to.
   * @param constructorsToTest the constructors to test
   */
  @SuppressWarnings({"ObjectAllocationInLoop", "unchecked"})
  public static <T> void testConstructors(final boolean includeProtected, final Map<Class<?>, Object> params,
                                          final Consumer<? super T> test, final Iterable<Constructor<?>> constructorsToTest) {
    for (final Constructor<?> constructor : constructorsToTest) {
      final int modifiers = constructor.getModifiers();
      if (Modifier.isPublic(modifiers) || (includeProtected && Modifier.isProtected(modifiers))) {
        constructor.setAccessible(true);
        final int nParams = constructor.getParameterCount();
        final Class<?>[] parameterTypes = constructor.getParameterTypes();
        final Object[] constructorParams = new Object[nParams];
        try {
          for (int i = 0; i < nParams; i++) {
            constructorParams[i] = params.get(parameterTypes[i]);
          }
          test.accept((T) constructor.newInstance(constructorParams));
        } catch (final IllegalAccessException | InstantiationException | InvocationTargetException | IllegalArgumentException e) {
          Assertions.fail(String
                    .format("Failed to call%n%s%nwith parameters%n%s", constructor.toGenericString(),
                        Arrays.toString(constructorParams)), e);
        }
      }
    }
  }

  public static void assertLessOrEqual(final long actual, final long expected) {
    assertLessOrEqual(actual, expected, "Value too high");
  }

  public static void assertLessOrEqual(final long actual, final long expected, String message) {
    if (actual > expected) {
      Assertions.fail(String.format("%s: Expected no more than %d but found %d", message, expected, actual));
    }
  }

  public static void assertLessOrEqual(final double actual, final double expected) {
    assertLessOrEqual(actual, expected, "Value too high");
  }

  public static void assertLessOrEqual(final double actual, final double expected, String message) {
    if (actual > expected) {
      Assertions.fail(String.format("%s: Expected no more than %f but found %f", message, expected, actual));
    }
  }

  public static void assertGreaterOrEqual(final long actual, final long expected) {
    assertGreaterOrEqual(actual, expected, "Value too low");
  }

  public static void assertGreaterOrEqual(final long actual, final long expected, String message) {
    if (actual < expected) {
      Assertions.fail(String.format("%s: Expected at least %d but found %d", message, expected, actual));
    }
  }

  public static void assertGreaterOrEqual(final double actual, final double expected) {
    assertGreaterOrEqual(actual, expected, "Value too low");
  }

  public static void assertGreaterOrEqual(final double actual, final double expected,
                                          String message) {
    if (actual < expected) {
      Assertions.fail(String.format("%s: Expected at least %f but found %f", message, expected, actual));
    }
  }

  public static void assertLess(final double actual, final double expected) {
    if (actual >= expected) {
      Assertions.fail(String.format("Expected less than %f but found %f", expected, actual));
    }
  }

}
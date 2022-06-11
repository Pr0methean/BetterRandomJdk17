package io.github.pr0methean.newbetterrandom;

import java.util.DoubleSummaryStatistics;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;
import java.util.stream.BaseStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class RandomTestUtils {
  public static final int STREAM_SIZE = 20;

  public static void checkRangeAndEntropy(final Supplier<? extends Number> numberSupplier, final double origin, final double bound) {
    final Number output = numberSupplier.get();
    TestUtils.assertGreaterOrEqual(output.doubleValue(), origin);
    TestUtils.assertLess(output.doubleValue(), bound);
  }

  /**
   * Test that the given parameterless constructor, called twice, doesn't produce RNGs that compare
   * as equal. Also checks for compliance with basic parts of the Object.equals() contract.
   */
  @SuppressWarnings({"ObjectEqualsNull"})
  public static void doEqualsSanityChecks(final Supplier<? extends RandomGenerator> ctor) {
    final RandomGenerator rng = ctor.get();
    final RandomGenerator rng2 = ctor.get();
    assertNotEquals(rng, rng2);
    assertEquals(rng, rng, "RNG doesn't compare equal to itself");
    assertNotEquals(rng, null, "RNG compares equal to null");
    assertNotEquals(rng, RandomGenerator.getDefault(), "RNG compares equal to new Random()");
  }

  /**
   * This is a rudimentary check to ensure that the output of a given RNG is approximately uniformly
   * distributed.  If the RNG output is not uniformly distributed, this method will return a poor
   * estimate for the value of pi.
   *
   * @param rng The RNG to test.
   * @param iterations The number of random points to generate for use in the calculation.  This
   *     value needs to be sufficiently large in order to produce a reasonably
   *     accurate result
   *     (assuming the RNG is uniform). Less than 10,000 is not particularly useful
   *     .  100,000 should
   *     be sufficient.
   * @return An approximation of pi generated using the provided RNG.
   */
  private static double calculateMonteCarloValueForPi(final RandomGenerator rng, final int iterations) {
    // Assumes a quadrant of a circle of radius 1, bounded by a box with
    // sides of length 1.  The area of the square is therefore 1 square unit
    // and the area of the quadrant is (pi * r^2) / 4.
    int totalInsideQuadrant = 0;
    // Generate the specified number of random points and count how many fall
    // within the quadrant and how many do not.  We expect the number of points
    // in the quadrant (expressed as a fraction of the total number of points)
    // to be pi/4.  Therefore pi = 4 * ratio.
    for (int i = 0; i < iterations; i++) {
      final double x = rng.nextDouble();
      final double y = rng.nextDouble();
      if (isInQuadrant(x, y)) {
        ++totalInsideQuadrant;
      }
    }
    // From these figures we can deduce an approximate value for Pi.
    return 4 * ((double) totalInsideQuadrant / iterations);
  }

  /**
   * Uses Pythagoras' theorem to determine whether the specified coordinates fall within the area of
   * the quadrant of a circle of radius 1 that is centered on the origin.
   *
   * @param x The x-coordinate of the point (must be between 0 and 1).
   * @param y The y-coordinate of the point (must be between 0 and 1).
   * @return True if the point is within the quadrant, false otherwise.
   */
  private static boolean isInQuadrant(final double x, final double y) {
    final double distance = Math.sqrt((x * x) + (y * y));
    return distance <= 1;
  }

  /**
   * Generates a sequence of integers from a given random number generator and then calculates the
   * standard deviation of the sample.
   *
   * @param rng The RNG to use.
   * @param maxValue The maximum value for generated integers (values will be in the range [0,
   *     maxValue)).
   * @param iterations The number of values to generate and use in the standard deviation
   *     calculation.
   * @return The standard deviation of the generated sample.
   */
  public static DoubleSummaryStatistics summaryStats(final RandomGenerator rng,
                                                     final long maxValue, final int iterations) {
    final DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
    final BaseStream<? extends Number, ?> stream =
        (maxValue <= Integer.MAX_VALUE) ? rng.ints(iterations, 0, (int) maxValue) :
            rng.longs(iterations, 0, maxValue);
    stream.spliterator().forEachRemaining(n -> stats.accept(n.doubleValue()));
    return stats;
  }

  public static void assertMonteCarloPiEstimateSane(final RandomGenerator rng) {
    final double pi = calculateMonteCarloValueForPi(rng, 100000);
    assertEquals(pi, Math.PI, 0.01 * Math.PI,
        "Monte Carlo value for Pi is outside acceptable range:" + pi);
  }

  public enum EntropyCheckMode {
    EXACT, LOWER_BOUND, OFF
  }
}

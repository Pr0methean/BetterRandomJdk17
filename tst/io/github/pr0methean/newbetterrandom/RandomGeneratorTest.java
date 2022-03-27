package io.github.pr0methean.newbetterrandom;

import static io.github.pr0methean.newbetterrandom.RandomTestUtils.STREAM_SIZE;
import static io.github.pr0methean.newbetterrandom.RandomTestUtils.assertMonteCarloPiEstimateSane;
import static io.github.pr0methean.newbetterrandom.RandomTestUtils.checkRangeAndEntropy;
import static io.github.pr0methean.newbetterrandom.TestUtils.assertGreaterOrEqual;
import static io.github.pr0methean.newbetterrandom.TestUtils.assertLessOrEqual;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.github.pr0methean.newbetterrandom.producer.SeedException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

public abstract class RandomGeneratorTest<T extends RandomGenerator> {

  protected static final int INSTANCES_TO_HASH = 25;
  protected static final int EXPECTED_UNIQUE_HASHES = (int) (0.8 * INSTANCES_TO_HASH);

  protected static final long TEST_SEED = 0x0123456789ABCDEFL;

  protected static final int TEST_BYTE_ARRAY_LENGTH = STREAM_SIZE;
  private static final double UPPER_BOUND_FOR_ROUNDING_TEST =
        Double.longBitsToDouble(Double.doubleToLongBits(1.0) + 3);

  protected RandomTestUtils.EntropyCheckMode getEntropyCheckMode() {
      return RandomTestUtils.EntropyCheckMode.EXACT;
    }

  protected Map<Class<?>, Object> constructorParams() {
      final HashMap<Class<?>, Object> params = new HashMap<>(4);
      params.put(long.class, TEST_SEED);
      params.put(byte[].class, new byte[16]);
      return params;
    }

  /**
     * Must have a looser type bound than T in case T is a generic type.
     * @return the class under test
     */
    protected Class<? extends RandomGenerator> getClassUnderTest() {
      return createRng().getClass();
    }

  protected abstract T createRng();

  /**
     * Test to ensure that the output from the RNG is broadly as expected.  This will not detect the
     * subtle statistical anomalies that would be picked up by Diehard, but it provides a simple check
     * for major problems with the output.
     */
    @Test public void testDistribution()
        throws SeedException {
      final RandomGenerator rng = createRng();
      assertMonteCarloPiEstimateSane(rng);
    }

    /**
     * Test to ensure that the output from the RNG is broadly as expected.  This will not detect the
     * subtle statistical anomalies that would be picked up by Diehard, but it provides a simple check
     * for major problems with the output.
     */
    @Test public void testIntegerSummaryStats()
        throws SeedException {
      final RandomGenerator rng = createRng();
      // Expected standard deviation for a uniformly distributed population of values in the range
      // 0..n
      // approaches n/sqrt(12).
      // Expected standard deviation for a uniformly distributed population of values in the range
      // 0..n
      // approaches n/sqrt(12).
      for (final long n : new long[]{100, 1L << 32, Long.MAX_VALUE}) {
        final int iterations = 10_000;
        final DoubleSummaryStatistics stats =
            RandomTestUtils.summaryStats(rng, n, iterations);
        assertGreaterOrEqual(stats.getMax(), 0.9 * n);
        assertLessOrEqual(stats.getMax(), n - 1);
        assertGreaterOrEqual(stats.getMin(), 0);
        assertLessOrEqual(stats.getMin(), 0.1 * n);
        assertGreaterOrEqual(stats.getAverage(), 0.4 * n);
        assertLessOrEqual(stats.getAverage(), 0.6 * n);
      }
    }

    /**
     * Test to ensure that the output from nextGaussian is broadly as expected.
     */
    @Test public void testNextGaussianStatistically()
        throws SeedException {
      final RandomGenerator rng = createRng();
      final DoubleSummaryStatistics stats = new DoubleSummaryStatistics();
      for (int i = 0; i < 20_000; i++) {
        stats.accept(rng.nextGaussian());
      }
      assertGreaterOrEqual(stats.getMax(), 2.0);
      assertLessOrEqual(stats.getMin(), -2.0);
      assertGreaterOrEqual(stats.getAverage(), -0.1);
      assertLessOrEqual(stats.getAverage(), 0.1);
    }

  @Test
    public void testAllPublicConstructors() throws SeedException {
      TestUtils.testConstructors(getClassUnderTest(), false, Map.copyOf(constructorParams()),
          RandomGenerator::nextInt);
    }

  @Test public void testEquals() throws SeedException {
    RandomTestUtils.doEqualsSanityChecks(this::createRng);
  }

  @Test public void testHashCode() {
    final HashSet<Integer> uniqueHashCodes = new HashSet<>(INSTANCES_TO_HASH);
    for (int i = 0; i < INSTANCES_TO_HASH; i++) {
      uniqueHashCodes.add(createRng().hashCode());
    }
    assertGreaterOrEqual(uniqueHashCodes.size(), EXPECTED_UNIQUE_HASHES,
        "Too many hashCode collisions");
  }

  @Test public void testNextBooleanStatistically() {
      final RandomGenerator prng = createRng();
      int trues = 0;
      for (int i = 0; i < 3000; i++) {
        if (prng.nextBoolean()) {
          trues++;
        }
      }
      // Significance test at p=4.54E-6 (unadjusted for the multiple subclasses and environments!)
      assertGreaterOrEqual(trues, 1375);
      assertLessOrEqual(trues, 1625);
    }

    @Test public void testNextBytes() {
      final byte[] testBytes = new byte[TEST_BYTE_ARRAY_LENGTH];
      final RandomGenerator prng = createRng();
      prng.nextBytes(testBytes);
      assertFalse(Arrays.equals(testBytes, new byte[TEST_BYTE_ARRAY_LENGTH]));
    }

    @Test public void testNextInt1() {
      final RandomGenerator prng = createRng();
      final Supplier<? extends Number> numberSupplier = () -> prng.nextInt(3 << 29);
      getEntropyCheckMode();
      checkRangeAndEntropy(numberSupplier, 0, 3 << 29);
    }

    @Test
    public void testNextInt1InvalidBound() {
      try {
        createRng().nextInt(0);
        fail("Should have thrown IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }

    @Test public void testNextInt() {
      final RandomGenerator prng = createRng();
      getEntropyCheckMode();
      checkRangeAndEntropy((Supplier<? extends Number>) prng::nextInt, Integer.MIN_VALUE, (double) (Integer.MAX_VALUE + 1L));
    }

    @Test public void testNextInt2() {
      final RandomGenerator prng = createRng();
      final Supplier<? extends Number> numberSupplier = () -> prng.nextInt(1 << 27, 1 << 29);
      getEntropyCheckMode();
      checkRangeAndEntropy(numberSupplier, 1 << 27, 1 << 29);
    }

    public void testNextInt2InvalidBound() {
      try {
        createRng().nextInt(1, 1);
        fail("Should have thrown IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }

    @Test public void testNextInt2HugeRange() {
      final RandomGenerator prng = createRng();
      final Supplier<? extends Number> numberSupplier =
          () -> prng.nextInt(Integer.MIN_VALUE, 1 << 29);
      getEntropyCheckMode();
      checkRangeAndEntropy(numberSupplier, Integer.MIN_VALUE, 1 << 29);
    }

    @Test public void testNextLong() {
      final RandomGenerator prng = createRng();
      getEntropyCheckMode();
      checkRangeAndEntropy((Supplier<? extends Number>) prng::nextLong, (double) Long.MIN_VALUE, Long.MAX_VALUE + 1.0);
    }

    @Test public void testNextLong1() {
      final RandomGenerator prng = createRng();
      for (int i = 0; i < STREAM_SIZE; i++) {
        // check that the bound is exclusive, to kill an off-by-one mutant
        final Supplier<? extends Number> numberSupplier = () -> prng.nextLong(2);
        getEntropyCheckMode();
        checkRangeAndEntropy(numberSupplier, 0, 2);
      }
      final Supplier<? extends Number> numberSupplier = () -> prng.nextLong(1L << 42);
      getEntropyCheckMode();
      checkRangeAndEntropy(numberSupplier, 0, (double) (1L << 42));
    }

    @Test
    public void testNextLong1InvalidBound() {
      try {
        createRng().nextLong(-1);
        fail("Should have thrown IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }

    @Test public void testNextLong2() {
      final RandomGenerator prng = createRng();
      final Supplier<? extends Number> numberSupplier = () -> prng.nextLong(1L << 40, 1L << 42);
      getEntropyCheckMode();
      checkRangeAndEntropy(numberSupplier, (double) (1L << 40), (double) (1L << 42));
    }

    @Test
    public void testNextLong2InvalidBound() {
      try {
        createRng().nextLong(10, 9);
        fail("Should have thrown IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }

    @Test public void testNextLong2HugeRange() {
      final RandomGenerator prng = createRng();
      final Supplier<? extends Number> numberSupplier = () -> prng.nextLong(Long.MIN_VALUE, 1L << 62);
      getEntropyCheckMode();
      checkRangeAndEntropy(numberSupplier, (double) Long.MIN_VALUE, (double) (1L << 62));
    }

    @Test public void testNextDouble() {
      final RandomGenerator prng = createRng();
      getEntropyCheckMode();
      checkRangeAndEntropy((Supplier<? extends Number>) prng::nextDouble, 0.0, 1.0);
    }

    @Test public void testNextFloat() {
      final RandomGenerator prng = createRng();
      getEntropyCheckMode();
      checkRangeAndEntropy((Supplier<? extends Number>) prng::nextFloat, 0.0, 1.0);
    }

    @Test public void testNextDouble1() {
      final RandomGenerator prng = createRng();
      final Supplier<? extends Number> numberSupplier = () -> prng.nextDouble(13.37);
      getEntropyCheckMode();
      checkRangeAndEntropy(numberSupplier, 0.0, 13.37);
    }

    @Test
    public void testNextDouble1InvalidBound() {
      try {
        createRng().nextDouble(-1.0);
        fail("Should have thrown IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }

    @Test public void testNextDouble2() {
      final RandomGenerator prng = createRng();
      final Supplier<? extends Number> numberSupplier2 = () -> prng.nextDouble(-1.0, 13.37);
      getEntropyCheckMode();
      checkRangeAndEntropy(numberSupplier2, -1.0, 13.37);
      final Supplier<? extends Number> numberSupplier1 = () -> prng.nextDouble(5.0, 13.37);
      getEntropyCheckMode();
      checkRangeAndEntropy(numberSupplier1, 5.0, 13.37);
      final Supplier<? extends Number> numberSupplier =
          () -> prng.nextDouble(1.0, UPPER_BOUND_FOR_ROUNDING_TEST);
      getEntropyCheckMode();
      checkRangeAndEntropy(numberSupplier, 1.0, UPPER_BOUND_FOR_ROUNDING_TEST);
    }

    @Test
    public void testNextDouble2InvalidBound() {
      try {
        createRng().nextDouble(3.5, 3.5);
        fail("Should have thrown IllegalArgumentException");
      } catch (IllegalArgumentException expected) {}
    }

    @Test public void testNextGaussian() {
      final RandomGenerator prng = createRng();
      // TODO: Find out the actual Shannon entropy of nextGaussian() and adjust the entropy count to
      // it in a wrapper function.
      final double origin = -Double.MAX_VALUE;
      getEntropyCheckMode();
      checkRangeAndEntropy((Supplier<? extends Number>) () -> prng.nextGaussian() + prng.nextGaussian(), origin, Double.MAX_VALUE);
    }

    @Test public void testNextBoolean() {
      final RandomGenerator prng = createRng();
      final Supplier<? extends Number> numberSupplier = () -> prng.nextBoolean() ? 0 : 1;
      getEntropyCheckMode();
      checkRangeAndEntropy(numberSupplier, 0, 2);
    }

}

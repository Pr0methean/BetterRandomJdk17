package io.github.pr0methean.newbetterrandom.reseedable;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.random.RandomGenerator;

import io.github.pr0methean.newbetterrandom.RandomGeneratorTest;
import io.github.pr0methean.newbetterrandom.producer.SeedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

abstract class ReseedableRandomGeneratorTest extends RandomGeneratorTest<ReseedableRandomGenerator> {

  private final byte[] TEST_SEED_BYTES = HexFormat.of().parseHex("00112233445566778899aabbccddeeff");
  private final long TEST_SEED_LONG = 0x0ff1ce0f1337c0deL;
  protected static final ThreadLocal<RandomGenerator> SEED_SOURCE_PRNG
      = ThreadLocal.withInitial(() -> RandomGenerator.of("L64X128MixRandom"));

  @Test
  public void testReseedWithBytes() {
    byte[] seed = new byte[16];
    SEED_SOURCE_PRNG.get().nextBytes(seed);
    ReseedableRandomGenerator notReseeded = createRng();
    byte[] bytesFromNotReseeded = new byte[128];
    notReseeded.nextBytes(bytesFromNotReseeded);
    ReseedableRandomGenerator reseeded = createRng();
    reseeded.reseed(seed);
    byte[] bytesFromReseeded = new byte[128];
    reseeded.nextBytes(bytesFromNotReseeded);
    assertFalse(Arrays.equals(bytesFromNotReseeded, bytesFromReseeded),
        "Same output with and without reseeding");
    ReseedableRandomGenerator usedThenReseeded = createRng();
    usedThenReseeded.nextBytes(new byte[16]);
    usedThenReseeded.reseed(seed);
    byte[] bytesFromUsedThenReseeded = new byte[128];
    usedThenReseeded.nextBytes(bytesFromUsedThenReseeded);
    assertArrayEquals(bytesFromReseeded, bytesFromUsedThenReseeded, "Not same output if used before reseeding");
  }

  @Test
  public void testReseedWithLong() {
    long seed = SEED_SOURCE_PRNG.get().nextLong();
    ReseedableRandomGenerator notReseeded = createRng();
    byte[] bytesFromNotReseeded = new byte[128];
    notReseeded.nextBytes(bytesFromNotReseeded);
    ReseedableRandomGenerator reseeded = createRng();
    reseeded.reseed(seed);
    byte[] bytesFromReseeded = new byte[128];
    reseeded.nextBytes(bytesFromNotReseeded);
    assertFalse(Arrays.equals(bytesFromNotReseeded, bytesFromReseeded),
        "Same output with and without reseeding");
    ReseedableRandomGenerator usedThenReseeded = createRng();
    usedThenReseeded.nextBytes(new byte[16]);
    usedThenReseeded.reseed(seed);
    byte[] bytesFromUsedThenReseeded = new byte[128];
    usedThenReseeded.nextBytes(bytesFromUsedThenReseeded);
    assertArrayEquals(bytesFromReseeded, bytesFromUsedThenReseeded, "Not same output if used before reseeding");
  }

  /**
   * Test that nextGaussian never returns a stale cached value.
   */
  @Test public void testRepeatability() throws SeedException {
    ReseedableRandomGenerator rng1 = createRng();
    ReseedableRandomGenerator rng2 = createRng();
    final byte[] seed = new byte[16];
    rng1.reseed(seed);
    rng2.reseed(seed);
    byte[] bytesFromRng1 = new byte[128];
    rng1.nextBytes(bytesFromRng1);
    byte[] bytesFromRng2 = new byte[128];
    rng2.nextBytes(bytesFromRng2);
    assertArrayEquals(bytesFromRng1, bytesFromRng2, "PRNGs not equivalent after reseeding with same seed");
  }

  @Test public void testSeedEntropyBits() {
    assertTrue(createRng().seedEntropyBits() > 0, "seedEntropyBits() should be positive");
  }

  @Test public void testDesiredSeedSizeBytes() {
    ReseedableRandomGenerator prng = createRng();
    int bytes = prng.desiredSeedSizeBytes();
    assertTrue(bytes > 0, "desiredSeedSizeBytes() should be positive");
    prng.reseed(new byte[bytes]);
  }
}
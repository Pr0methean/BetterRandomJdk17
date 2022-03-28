package io.github.pr0methean.newbetterrandom.reseedable;

import java.util.Arrays;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReseedByReplacingRandomGeneratorTest extends ReseedableRandomGeneratorTest {

  @Override
  protected ReseedableRandomGenerator createRng() {
    return new ReseedByReplacingRandomGenerator(RandomGeneratorFactory.getDefault());
  }

  @Override
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
}
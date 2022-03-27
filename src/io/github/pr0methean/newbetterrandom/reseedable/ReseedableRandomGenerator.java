package io.github.pr0methean.newbetterrandom.reseedable;

import java.util.random.RandomGenerator;

public interface ReseedableRandomGenerator extends RandomGenerator {

  void reseed(byte[] seed);

  void reseed(long seed);

  /**
   * The number of bits of entropy in this PRNG's state after reseeding with a truly random seed.
   * @return the entropy in bits
   */
  int seedEntropyBits();

  default int desiredSeedSizeBytes() {
    return (seedEntropyBits() + Byte.SIZE - 1) / Byte.SIZE;
  }
}

package io.github.pr0methean.newbetterrandom.reseedable;

import java.util.random.RandomGenerator;

import static java.lang.Byte.toUnsignedLong;
import static java.lang.Long.reverse;

public interface ReseedableRandomGenerator extends RandomGenerator {

  long SILVER_RATIO_64 = 0x6A09E667F3BCC909L;
  long GOLDEN_RATIO_64 = 0x9e3779b97f4a7c15L;

  void updateSeed(byte[] seed);

  void updateSeed(long seed);

  /**
   * The number of bits of entropy in this PRNG's state after reseeding with a truly random seed.
   * @return the entropy in bits
   */
  int seedEntropyBits();

  default int desiredSeedSizeBytes() {
    return (seedEntropyBits() + Byte.SIZE - 1) / Byte.SIZE;
  }

  static long bytesToLong(byte[] input, int outputBits) {
    if (outputBits == 0) {
      return 0;
    }
    long inputBits = (long) input.length * Byte.SIZE;
    if (inputBits == outputBits) {
      long result = 0;
      for (byte b : input) {
        result <<= Byte.SIZE;
        result |= toUnsignedLong(b);
      }
      return result;
    }
    long result = SILVER_RATIO_64;
    final int repeats = switch (input.length) {
      case 1 -> 8;
      case 2 -> 4;
      case 3 -> 3;
      case 4, 5, 6, 7 -> 2;
      default -> 1;
    };
    for (int i = 0; i < repeats; i++) {
      for (byte b : input) {
        // Mixing in i ensures we get different results for different size inputs
        long z = result + (b + (i << Byte.SIZE)) * GOLDEN_RATIO_64;
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        result = z ^ (z >>> 33);
      }
    }
    if (outputBits <= 16) {
      result = result ^ (result >> 16) ^ (result >> 32) ^ (result >> 48);
    } else if (outputBits <= 32) {
      result = result ^ reverse(result);
    }
    return outputBits == 64 ? result : result & ((1L << outputBits) - 1);
  }
}

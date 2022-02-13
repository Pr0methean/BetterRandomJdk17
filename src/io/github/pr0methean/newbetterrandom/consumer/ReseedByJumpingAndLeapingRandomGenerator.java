package io.github.pr0methean.newbetterrandom.consumer;

import java.util.function.Function;
import java.util.random.RandomGenerator;

import io.github.pr0methean.newbetterrandom.buffer.AtomicSeedByteRingBuffer;

import static java.lang.Integer.numberOfLeadingZeros;

public class ReseedByJumpingAndLeapingRandomGenerator<T extends RandomGenerator.LeapableGenerator>
    extends ReseedingRandomGenerator<T> {

  private final int maxJumps, maxLeaps;
  long jumpSeedMask, leapSeedMask, leapSeedShift;

  public ReseedByJumpingAndLeapingRandomGenerator(final Function<byte[], T> delegateFactory,
                                                  final AtomicSeedByteRingBuffer seedSource,
                                                  final int maxJumps,
                                                  final int maxLeaps) {
    super(delegateFactory, seedSource, findSeedSize(maxJumps, maxLeaps));
    this.maxJumps = maxJumps;
    this.maxLeaps = maxLeaps;
    jumpSeedMask = maxJumps - 1;
    leapSeedShift = Integer.SIZE - numberOfLeadingZeros(maxLeaps);
    leapSeedMask = (maxLeaps - 1L) << leapSeedShift;
  }

  private static int findSeedSize(int maxJumps, int maxLeaps) {
    checkPositivePowerOf2(maxJumps, "maxJumps");
    checkPositivePowerOf2(maxLeaps, "maxLeaps");
    return (Long.SIZE - numberOfLeadingZeros(maxJumps) - numberOfLeadingZeros(maxLeaps) + Byte.SIZE - 1) / Byte.SIZE;
  }

  private static void checkPositivePowerOf2(int maxJumps, final String parameterName) {
    if (maxJumps <= 0 || Integer.bitCount(maxJumps) != 1) {
      throw new IllegalArgumentException(parameterName + " should be positive power of 2");
    }
  }

  @Override
  protected void reseed() {
    // TODO
  }
}

package io.github.pr0methean.newbetterrandom.consumer;

import io.github.pr0methean.newbetterrandom.buffer.AtomicSeedByteRingBuffer;

import java.util.function.Function;
import java.util.random.RandomGenerator;

public class EntropyCountingReplacingRandomGenerator<T extends RandomGenerator>
      extends ReplacingRandomGenerator<T>
    implements EntropyCountingRandomGenerator {
  private static final long RANDOM_BITS_PER_FLOAT = 24;
  private static final long RANDOM_BITS_PER_DOUBLE = 53;
  public static final int LONG_TO_BYTE_SHIFT = (Long.SIZE - Byte.SIZE);

  private final long entropyWhenFresh;
  protected final long reseedingThreshold;
  private long entropy;

  public EntropyCountingReplacingRandomGenerator(final Function<byte[], T> delegateFactory,
      final AtomicSeedByteRingBuffer seedSource, final long reseedingThreshold, final int seedSize) {
    super(delegateFactory, seedSource, seedSize);
    entropy = (long) seedSize * Byte.SIZE;
    entropyWhenFresh = entropy;
    if (reseedingThreshold > entropyWhenFresh) {
      throw new IllegalArgumentException(
          String.format("Entropy will never be more than seed size of %d bits, which is lower than reseeding " +
              "threshold of %d bits", entropyWhenFresh, reseedingThreshold));
    }
    this.reseedingThreshold = reseedingThreshold;
  }

  @Override public boolean nextBoolean() {
    entropy--;
    return super.nextBoolean();
  }

  @Override public void nextBytes(final byte[] bytes) {
    for (int i = 0; i < bytes.length; i++) {
      entropy -= Byte.SIZE;
      bytes[i] = (byte) (super.nextLong() >> LONG_TO_BYTE_SHIFT);
    }
    super.nextBytes(bytes);
  }

  @Override public float nextFloat() {
    entropy -= RANDOM_BITS_PER_FLOAT;
    return super.nextFloat();
  }

  @Override public double nextDouble() {
    entropy -= RANDOM_BITS_PER_DOUBLE;
    return super.nextDouble();
  }

  @Override public int nextInt() {
    entropy -= Integer.SIZE;
    return super.nextInt();
  }

  @Override public long nextLong() {
    entropy -= Long.SIZE;
    return super.nextLong();
  }

  @Override
  protected void reseedIfImmediatelyPossible() {
    if (entropy < reseedingThreshold) {
      super.reseedIfImmediatelyPossible();
    }
  }

  @Override
  protected void setDelegate(T newDelegate) {
    super.setDelegate(newDelegate);
    entropy = entropyWhenFresh;
  }

  public long getEntropyBits() {
    return entropy;
  }
}

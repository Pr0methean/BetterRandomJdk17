package io.github.pr0methean.newbetterrandom.autoreseed;

import java.util.random.RandomGenerator;

import io.github.pr0methean.newbetterrandom.reseedable.ReseedableRandomGenerator;

public class EntropyCountingRandomGeneratorWrapper implements RandomGenerator {

  protected static final long RANDOM_BITS_PER_FLOAT = 24;
  protected static final long RANDOM_BITS_PER_DOUBLE = 53;
  /**
   * Rough estimate based on the fact that nextExponential makes only one nextLong call 98% of the time
   */
  protected static final long RANDOM_BITS_PER_EXPONENTIAL = Long.SIZE + 1;
  /**
   * Rough estimate based on the fact that nextGaussian makes only one nextLong call 98% of the time
   */
  protected static final long RANDOM_BITS_PER_GAUSSIAN = Long.SIZE + 1;

  protected static final long MAX_RANDOM_BITS_PER_CALL =
      Math.max(Math.max(RANDOM_BITS_PER_GAUSSIAN, RANDOM_BITS_PER_EXPONENTIAL), Long.SIZE);

  protected final ReseedableRandomGenerator delegate;
  protected long entropy;

  public EntropyCountingRandomGeneratorWrapper(ReseedableRandomGenerator delegate) {
    this(delegate, delegate.seedEntropyBits());
  }

  public EntropyCountingRandomGeneratorWrapper(ReseedableRandomGenerator delegate, long initialEntropy) {
    this.delegate = delegate;
    this.entropy = initialEntropy;
  }

  protected void debitEntropy(long amount) {
    entropy -= amount;
  }

  @Override public boolean nextBoolean() {
    debitEntropy(1);
    return delegate.nextBoolean();
  }

  @Override public void nextBytes(final byte[] bytes) {
    debitEntropy((long) bytes.length * Byte.SIZE);
    delegate.nextBytes(bytes);
  }

  @Override public float nextFloat() {
    debitEntropy(RANDOM_BITS_PER_FLOAT);
    return delegate.nextFloat();
  }

  @Override public double nextDouble() {
    debitEntropy(RANDOM_BITS_PER_DOUBLE);
    return delegate.nextDouble();
  }

  @Override
  public int nextInt(int bound) {
    debitEntropy(Integer.SIZE - Integer.numberOfLeadingZeros(bound - 1));
    return delegate.nextInt(bound);
  }

  @Override
  public int nextInt(int origin, int bound) {
    final int range = origin - bound;
    debitEntropy(range < 0 ? Integer.SIZE : Integer.SIZE - Integer.numberOfLeadingZeros(range - 1));
    return delegate.nextInt(origin, bound);
  }

  @Override
  public long nextLong(long bound) {
    debitEntropy(Long.SIZE - Long.numberOfLeadingZeros(bound - 1));
    return delegate.nextLong(bound);
  }

  @Override
  public long nextLong(long origin, long bound) {
    final long range = origin - bound;
    debitEntropy(range < 0 ? Long.SIZE : Long.SIZE - Long.numberOfLeadingZeros(range - 1));
    return delegate.nextLong(origin, bound);
  }

  @Override
  public double nextGaussian() {
    debitEntropy(RANDOM_BITS_PER_GAUSSIAN);
    return delegate.nextGaussian();
  }

  @Override
  public double nextExponential() {
    debitEntropy(RANDOM_BITS_PER_EXPONENTIAL);
    return delegate.nextExponential();
  }

  @Override public int nextInt() {
    debitEntropy(Integer.SIZE);
    return delegate.nextInt();
  }

  @Override public long nextLong() {
    debitEntropy(Long.SIZE);
    return delegate.nextLong();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return o instanceof EntropyCountingRandomGeneratorWrapper that
        && getClass() == that.getClass()
        && delegate.equals(that.delegate);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }
}

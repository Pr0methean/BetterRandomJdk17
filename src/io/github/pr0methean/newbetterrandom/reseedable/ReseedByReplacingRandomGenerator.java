package io.github.pr0methean.newbetterrandom.reseedable;

import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

public class ReseedByReplacingRandomGenerator implements ReseedableRandomGenerator {

  protected RandomGenerator delegate;
  protected final RandomGeneratorFactory<?> delegateFactory;
  protected final int seedEntropyBits;

  public ReseedByReplacingRandomGenerator(RandomGeneratorFactory<?> delegateFactory) {
    this(delegateFactory.create(), delegateFactory);
  }

  public ReseedByReplacingRandomGenerator(RandomGenerator delegate, RandomGeneratorFactory<?> delegateFactory) {
    this(delegate, delegateFactory, delegateFactory.stateBits());
  }

  public ReseedByReplacingRandomGenerator(RandomGenerator delegate, RandomGeneratorFactory<?> delegateFactory, int seedEntropyBits) {
    this.delegateFactory = delegateFactory;
    this.delegate = delegate;
    this.seedEntropyBits = seedEntropyBits;
  }

  @Override
  public void reseed(byte[] seed) {
    delegate = delegateFactory.create(seed);
  }

  @Override
  public void reseed(long seed) {
    delegate = delegateFactory.create(seed);
  }

  @Override
  public int seedEntropyBits() {
    return seedEntropyBits;
  }

  @Override
  public long nextLong() {
    return delegate.nextLong();
  }
}

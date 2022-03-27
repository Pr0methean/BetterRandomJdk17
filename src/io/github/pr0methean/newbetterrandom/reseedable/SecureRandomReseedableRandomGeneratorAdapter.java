package io.github.pr0methean.newbetterrandom.reseedable;

import java.security.SecureRandom;

public class SecureRandomReseedableRandomGeneratorAdapter implements ReseedableRandomGenerator {
  private final SecureRandom delegate;
  private final int seedEntropyBits;

  public SecureRandomReseedableRandomGeneratorAdapter(SecureRandom delegate, int seedEntropyBits) {
    this.delegate = delegate;
    this.seedEntropyBits = seedEntropyBits;
  }

  @Override
  public void reseed(byte[] seed) {
    delegate.setSeed(seed);
  }

  @Override
  public void reseed(long seed) {
    delegate.setSeed(seed);
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

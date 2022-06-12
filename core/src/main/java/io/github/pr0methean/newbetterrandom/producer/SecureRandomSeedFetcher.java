package io.github.pr0methean.newbetterrandom.producer;

import io.github.pr0methean.newbetterrandom.buffer.ByteQueue;

import java.security.SecureRandom;

public class SecureRandomSeedFetcher extends AbstractSeedFetcher {
  private final SecureRandom secureRandom;

  public SecureRandomSeedFetcher(ByteQueue destBuffer, int sourceReadSize,
                                 final SecureRandom secureRandom) {
    super(destBuffer, sourceReadSize);
    this.secureRandom = secureRandom;
  }

  @Override protected byte[] getSeedBytes() {
    return secureRandom.generateSeed(sourceReadSize);
  }
}

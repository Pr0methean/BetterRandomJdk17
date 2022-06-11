package io.github.pr0methean.newbetterrandom.reseedable;

public class ReseedByJumpingAndLeapingRandomGenerator implements ReseedableRandomGenerator {

  private final int jumpBits, leapBits;
  private final long jumpSeedMask, leapSeedMask;
  private final LeapableGenerator delegate;
  private final int seedBits;

  public ReseedByJumpingAndLeapingRandomGenerator(final LeapableGenerator delegate, final int jumpBits, final int leapBits) {
    if (leapBits < 0) {
      throw new IllegalArgumentException("leapBits can't be negative");
    }
    if (jumpBits < 0) {
      throw new IllegalArgumentException("jumpBits can't be negative");
    }
    if (leapBits + jumpBits > Long.SIZE) {
      throw new IllegalArgumentException("Can't consume more than " + Long.SIZE + " bits at once");
    }
    this.delegate = delegate;
    this.jumpBits = jumpBits;
    this.leapBits = leapBits;
    jumpSeedMask = (1L << jumpBits) - 1;
    final long leapSeedMaskUnshifted = (1L << leapBits) - 1;
    leapSeedMask = leapSeedMaskUnshifted << jumpBits;
    seedBits = jumpBits + leapBits;
  }

  @Override
  public void updateSeed(byte[] seed) {
    updateSeed(ReseedableRandomGenerator.bytesToLong(seed, seedBits));
  }

  @Override
  public void updateSeed(long seed) {
    long numJumps = (seed & jumpSeedMask);
    for (int i = 0; i < numJumps; i++) {
      delegate.jump();
    }
    long numLeaps = (seed & leapSeedMask) >>> jumpBits;
    if (numLeaps == 0 && numJumps == 0) {
      numLeaps = (leapSeedMask == -1) ? leapSeedMask : (leapSeedMask + 1);
    }
    numLeaps &= Integer.MAX_VALUE;
    for (int i = 0; i < numLeaps; i++) {
      delegate.leap();
    }
  }

  @Override
  public int seedEntropyBits() {
    return seedBits;
  }

  @Override
  public long nextLong() {
    return delegate.nextLong();
  }
}

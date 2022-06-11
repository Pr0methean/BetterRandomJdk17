package io.github.pr0methean.newbetterrandom.reseedable;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class SecureRandomReseedableRandomGeneratorAdapterTest extends ReseedableRandomGeneratorTest {

  @Override
  protected ReseedableRandomGenerator createRng() {
    try {
      return new SecureRandomReseedableRandomGeneratorAdapter(SecureRandom.getInstance("DRBG"), 128);
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }
}
package io.github.pr0methean.newbetterrandom.reseedable;

import java.util.random.RandomGeneratorFactory;

class ReseedByReplacingRandomGeneratorTest extends ReseedableRandomGeneratorTest {

  @Override
  protected ReseedableRandomGenerator createRng() {
    return new ReseedByReplacingRandomGenerator(RandomGeneratorFactory.getDefault());
  }
}
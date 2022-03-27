package io.github.pr0methean.newbetterrandom.reseedable;

import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ReseedByArbitraryJumpingRandomGeneratorTest extends ReseedableRandomGeneratorTest {

  private static RandomGeneratorFactory<RandomGenerator> GENERATOR_FACTORY;

  @BeforeAll
  public static void findFactory() {
    Optional<RandomGeneratorFactory<RandomGenerator>> generator
        = RandomGeneratorFactory.all()
        .filter(RandomGeneratorFactory::isArbitrarilyJumpable)
        .findFirst();
    assumeTrue(generator.isPresent());
    GENERATOR_FACTORY = generator.get();
  }

  @Override
  protected ReseedableRandomGenerator createRng() {
    return new ReseedByArbitraryJumpingRandomGenerator(
        (RandomGenerator.ArbitrarilyJumpableGenerator) GENERATOR_FACTORY.create(), 8);
  }
}
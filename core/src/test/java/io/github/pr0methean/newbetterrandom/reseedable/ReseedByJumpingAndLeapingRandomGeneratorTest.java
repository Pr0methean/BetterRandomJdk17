package io.github.pr0methean.newbetterrandom.reseedable;

import java.util.Optional;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import org.junit.jupiter.api.BeforeAll;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class ReseedByJumpingAndLeapingRandomGeneratorTest extends ReseedableRandomGeneratorTest {

  private static RandomGeneratorFactory<RandomGenerator> GENERATOR_FACTORY;

  @BeforeAll
  public static void findFactory() {
    Optional<RandomGeneratorFactory<RandomGenerator>> generator
        = RandomGeneratorFactory.all()
        .filter(RandomGeneratorFactory::isLeapable)
        .findFirst();
    assumeTrue(generator.isPresent(), "No leapable PRNG available");
    GENERATOR_FACTORY = generator.get();
  }

  @Override
  protected ReseedableRandomGenerator createRng() {
    return new ReseedByJumpingAndLeapingRandomGenerator((RandomGenerator.LeapableGenerator) GENERATOR_FACTORY.create(),
        20, 20);
  }
}
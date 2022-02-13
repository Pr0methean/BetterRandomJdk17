package io.github.pr0methean.newbetterrandom.producer;

import java.net.Proxy;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.net.ssl.SSLSocketFactory;

/**
 * Common configuration parameters for an instance of {@link WebSeedClient}. This class makes it
 * possible to add more parameters in the future without needing new constructor overloads in
 * {@link WebSeedClient} and all its subclasses.
 */
public record WebSeedClientConfiguration(@Nullable Proxy proxy,
                                         @Nullable SSLSocketFactory socketFactory, long retryDelayMs) {
  /**
   * Default configuration.
   */
  public static final WebSeedClientConfiguration DEFAULT = new WebSeedClientConfiguration(null, null, 0);
}

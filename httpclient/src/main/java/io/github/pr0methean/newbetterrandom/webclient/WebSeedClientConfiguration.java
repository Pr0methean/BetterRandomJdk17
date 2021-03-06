package io.github.pr0methean.newbetterrandom.webclient;

import java.net.Proxy;
import javax.annotation.Nullable;
import javax.net.ssl.SSLSocketFactory;

/**
 * Common configuration parameters for an instance of {@link WebSeedClient}. This class makes it
 * possible to add more parameters with zero/null defaults in the future without needing new constructor overloads in
 * {@link WebSeedClient} and all its subclasses.
 */
public record WebSeedClientConfiguration(@Nullable Proxy proxy,
                                         @Nullable SSLSocketFactory socketFactory,
                                         long retryDelayMs,
                                         int maxRetries) {
  /**
   * Default configuration.
   */
  public static final WebSeedClientConfiguration DEFAULT = new WebSeedClientConfiguration(null, null, 250, 5);
}

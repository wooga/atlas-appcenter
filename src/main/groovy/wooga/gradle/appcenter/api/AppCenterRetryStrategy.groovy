package wooga.gradle.appcenter.api

import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.client.ServiceUnavailableRetryStrategy
import org.apache.http.protocol.HttpContext
import org.apache.http.util.Args

class AppCenterRetryStrategy implements ServiceUnavailableRetryStrategy {
    /**
     * Maximum number of allowed retries if the server responds with a HTTP code
     * in our retry code list. Default value is 1.
     */
    private final int maxRetries;

    /**
     * Retry interval between subsequent requests, in milliseconds. Default
     * value is 1 second.
     */
    private final long retryInterval;

    AppCenterRetryStrategy(final Integer maxRetries, final Integer retryInterval) {
        super();
        Args.positive(maxRetries, "Max retries");
        Args.positive(retryInterval, "Retry interval");
        this.maxRetries = maxRetries;
        this.retryInterval = retryInterval;
    }

    AppCenterRetryStrategy() {
        this(30, 1000 * 10);
    }

    @Override
    boolean retryRequest(final HttpResponse response, final int executionCount, final HttpContext context) {
        return executionCount <= maxRetries &&
                (response.getStatusLine().getStatusCode() == HttpStatus.SC_SERVICE_UNAVAILABLE || response.getStatusLine().getStatusCode() == 429)
    }

    @Override
    long getRetryInterval() {
        return retryInterval;
    }
}

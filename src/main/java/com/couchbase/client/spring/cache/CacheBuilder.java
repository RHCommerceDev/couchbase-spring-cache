package com.couchbase.client.spring.cache;

import com.couchbase.client.java.Bucket;

import java.util.concurrent.TimeUnit;

/**
 * A builder for {@link CouchbaseCache} instance.
 *
 * @author Simon Baslé
 */
public class CacheBuilder {

  private static final int DEFAULT_TTL = 0;
  private static final int DEFAULT_INITIAL_LOCAL_CAPACITY = 100;
  private static final int DEFAULT_MAXIMUM_LOCAL_CAPACITY = 1000;

  private Bucket bucket;
  private int cacheExpiry;
  private int initialLocalCapacity;
  private int maximumLocalCapacity;

  protected CacheBuilder() {
    this.cacheExpiry = DEFAULT_TTL;
    this.initialLocalCapacity = DEFAULT_INITIAL_LOCAL_CAPACITY;
    this.maximumLocalCapacity = DEFAULT_MAXIMUM_LOCAL_CAPACITY;
  }

  /**
   * Create a new builder instance with the given {@link Bucket}.
   * @param bucket the bucket to use
   * @return a new builder
   */
  public static CacheBuilder newInstance(Bucket bucket) {
    return new CacheBuilder().withBucket(bucket);
  }

  /**
   * Give a bucket to the cache to be built.
   * @param bucket the bucket
   * @return this builder for chaining.
   */
  public CacheBuilder withBucket(Bucket bucket) {
    if (bucket == null) {
      throw new NullPointerException("A non-null Bucket is required for all cache builders");
    }
    this.bucket = bucket;
    return this;
  }

  /**
   * Give a default expiration (or TTL) to the cache to be built.
   *
   * This method will convert the given MS to seconds and call {@link #withExpiration(int)}
   * internally. If you use this method with an expiration less than 1000, it will be 0 and the document will not be
   * deleted automatically.
   *
   * @param expiration the expiration delay in milliseconds.
   * @return this builder for chaining.
   * @deprecated use {@link #withExpiration(int)} in seconds instead.
   */
  @Deprecated
  public CacheBuilder withExpirationInMillis(int expiration) {
    return withExpiration((int) TimeUnit.MILLISECONDS.toSeconds(expiration));
  }

  /**
   * Give a default expiration (or TTL) to the cache to be built in seconds.
   *
   * @param expiration the expiration delay in in seconds.
   * @return this builder for chaining purposes.
   */
  public CacheBuilder withExpiration(int expiration) {
    this.cacheExpiry = expiration;
    return this;
  }

  public CacheBuilder withInitialLocalCapacity(int initialLocalCapacity) {
    this.initialLocalCapacity = initialLocalCapacity;
    return this;
  }

  public CacheBuilder withMaximumLocalCapacity(int maximumLocalCapacity) {
    this.maximumLocalCapacity = maximumLocalCapacity;
    return this;
  }

  /**
   * Build a new {@link CouchbaseCache} with the specified name. If custom initial and maximum local Caffeine cache
   * capacities are desired for a named cache, you may use the alternate, colon-delimited name format of
   * [cache-name ':' initial-local-capacity ':' maximum-local-capacity] ('foo:10:100', e.g. excluding quotes).
   *
   * @param cacheName the name of the cache
   * @return a {@link CouchbaseCache} instance
   */
  public CouchbaseCache build(String cacheName) {
    String _cacheName = cacheName;
    int _initialLocalCapacity = this.initialLocalCapacity;
    int _maximumLocalCapacity = this.maximumLocalCapacity;

    if (cacheName.contains(":")) {
      String[] parts = cacheName.split(":");
      if (parts.length != 3) {
        throw new IllegalArgumentException("Configuration of named cache must be in the form " +
                "[ cache-name ':' initial-local-capacity ':' maximum-local-capacity ]");
      }
      _cacheName = parts[0];
      _initialLocalCapacity = Integer.parseInt(parts[1]);
      _maximumLocalCapacity = Integer.parseInt(parts[2]);
    }
    return new CouchbaseCache(_cacheName, this.bucket, this.cacheExpiry, _initialLocalCapacity, _maximumLocalCapacity);
  }

}

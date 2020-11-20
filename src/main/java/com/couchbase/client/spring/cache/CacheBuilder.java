package com.couchbase.client.spring.cache;

import com.couchbase.client.java.Bucket;
import com.rh.rhapsody.commons.deser.jackson.SafeObjectMapper;

import java.util.concurrent.TimeUnit;

/**
 * A builder for {@link CouchbaseCache} instance.
 *
 * @author Simon Baslé
 */
public class CacheBuilder {

  private static final int DEFAULT_TTL = 0;

  private Bucket bucket;
  private int cacheExpiry;
  private SafeObjectMapper objectMapper;

  protected CacheBuilder() {
    this.cacheExpiry = DEFAULT_TTL;
  }

  /**
   * Create a new builder instance with the given {@link Bucket}.
   * @param bucket the bucket to use
   * @return a new builder
   */
  public static CacheBuilder newInstance(Bucket bucket) {
    return new CacheBuilder().withBucket(bucket).withObjectMapper(SafeObjectMapper.Factory.buildRhapsodyStandard());
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

  public CacheBuilder withObjectMapper(SafeObjectMapper objectMapper) {
    if (objectMapper == null) {
      throw new NullPointerException("A non-null ObjectMapper is required for all cache builders.");
    }
    this.objectMapper = objectMapper;
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

  /**
   * Build a new {@link CouchbaseCache} with the specified name.
   * @param cacheName the name of the cache
   * @return a {@link CouchbaseCache} instance
   */
  public CouchbaseCache build(String cacheName) {
    return new CouchbaseCache(cacheName, this.bucket, this.cacheExpiry, this.objectMapper);
  }

}

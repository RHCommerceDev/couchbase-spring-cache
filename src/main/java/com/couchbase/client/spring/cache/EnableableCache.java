package com.couchbase.client.spring.cache;

import org.springframework.cache.Cache;

public interface EnableableCache extends Cache {
    void setEnabled(boolean enabled);
    boolean isEnabled();
}

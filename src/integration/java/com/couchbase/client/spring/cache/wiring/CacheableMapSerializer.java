package com.couchbase.client.spring.cache.wiring;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.core.GenericTypeResolver;

import java.io.IOException;
import java.io.Serializable;

public class CacheableMapSerializer <T extends Serializable> extends StdSerializer<CacheableMap<T>> {

    public CacheableMapSerializer() {
        this(null);
    }

    public CacheableMapSerializer(Class<CacheableMap<T>> t) {
        super(t);
    }

    @Override
    public void serialize(
            CacheableMap<T> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        jgen.writeStartObject();
        jgen.writeStringField("__class", value.clazz.getName());
        jgen.writeNumberField("ttl", value.cacheMapTtl);
        jgen.writeNumberField("sizeLimit", value.cacheMapSizeLimit);
        jgen.writeObjectField("map", value.cacheMap);
        jgen.writeEndObject();
    }
}

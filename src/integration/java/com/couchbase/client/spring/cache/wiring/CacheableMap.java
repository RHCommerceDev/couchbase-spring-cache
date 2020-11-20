package com.couchbase.client.spring.cache.wiring;

import com.couchbase.client.java.util.DigestUtils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.google.common.base.MoreObjects;
import com.rh.rhapsody.commons.deser.jackson.SafeObjectMapper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@JsonSerialize(using = CacheableMap.CacheableMapSerializer.class)
@JsonDeserialize(using = CacheableMap.CacheableMapDeserializer.class)
public class CacheableMap<T> implements Serializable {
    private static final int CACHE_MAP_SIZE_LIMIT_HARD = 5;
    private static final int CACHE_MAP_TTL_MS_LIMIT_HARD = 86400000;

    final Map<Object, Pair<Long, T>> cacheMap;
    final int cacheMapSizeLimit;
    final int cacheMapTtl;
    final Class clazz;

    public CacheableMap(final Class<T> clazz) {
        this.clazz = clazz;
        this.cacheMapTtl = CACHE_MAP_TTL_MS_LIMIT_HARD;
        this.cacheMapSizeLimit = CACHE_MAP_SIZE_LIMIT_HARD;
        this.cacheMap = new HashMap<>(CACHE_MAP_SIZE_LIMIT_HARD);
    }

    private CacheableMap(final Class<T> clazz, final int cacheMapSizeLimit, final int cacheMapTtl) {
        this.clazz = clazz;
        this.cacheMapTtl = cacheMapTtl;
        this.cacheMapSizeLimit = cacheMapSizeLimit;
        this.cacheMap = new HashMap<>(cacheMapSizeLimit);
    }

    public int size() {
        return this.cacheMap.size();
    }

    public CacheableMap<T> put(T value, Object... keyParts) {
        if (Objects.isNull(value)) {
            throw new IllegalArgumentException("value cannot be null.");
        } else if (ArrayUtils.isEmpty(keyParts)) {
            throw new IllegalArgumentException("keyParts cannot be null/empty.");
        } else {
            if (this.cacheMap.size() + 1 > this.cacheMapSizeLimit) {
                Optional<Map.Entry<Object, Pair<Long, T>>> oldestEntry = this.cacheMap.entrySet().stream().sorted(Comparator.comparing((t) -> {
                    return (Long)((Pair)t.getValue()).getLeft();
                })).findFirst();
                oldestEntry.ifPresent((t) -> {
                    this.cacheMap.remove(t.getKey());
                });
            }

            List<Map.Entry<Object, Pair<Long, T>>> expiredEntries = (List)this.cacheMap.entrySet().stream().filter((t) -> {
                return System.currentTimeMillis() - (Long)((Pair)t.getValue()).getLeft() > (long)this.cacheMapTtl;
            }).collect(Collectors.toList());
            expiredEntries.forEach((t) -> {
                this.cacheMap.remove(t.getKey(), t.getValue());
            });
            this.cacheMap.put(createKey(keyParts), Pair.of(System.currentTimeMillis(), value));
            return this;
        }
    }

    public Optional<T> get(Object... keyParts) {
        Optional<Pair<Long, T>> optionalPair = Optional.ofNullable(this.cacheMap.get(createKey(keyParts)));
        return optionalPair.isPresent() && System.currentTimeMillis() - (Long)((Pair<Long,T>)optionalPair.get()).getLeft() <= (long)this.cacheMapTtl ? Optional.of(((Pair<Long,T>)optionalPair.get()).getRight()) : Optional.empty();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CacheableMap)) {
            return false;
        }
        return cacheMap.equals(((CacheableMap<?>) other).cacheMap);
    }

    @Override
    public String toString() {
        return cacheMap.entrySet().stream().map(es -> es.getKey().toString() + ", " + es.getValue().toString())
                .reduce("", (partial, candidate) -> partial + candidate + "; ");
    }

    public static <T> CacheableMap<T> build(Class<T> clazz) {
        return new CacheableMap<T>(clazz, 5, 86400000);
    }

    public static <T> CacheableMap<T> build(Class<T> clazz, final int cacheMapSize, final int cacheMapTtl) {
        return new CacheableMap<T>(
                clazz,
                NumberUtils.min(new int[]{5, cacheMapSize}),
                NumberUtils.min(new int[]{86400000, cacheMapTtl})
        );
    }

    private static String createKey(Object... keyParts) {
        MoreObjects.ToStringHelper tsh = MoreObjects.toStringHelper("CacheableMap").omitNullValues();
        AtomicInteger index = new AtomicInteger(0);
        Arrays.asList(keyParts).stream().forEach((t) -> {
            tsh.add("param-" + index.getAndIncrement(), t);
        });
        return DigestUtils.digestSha1Hex(tsh.toString());
    }

    public static class CacheableMapDeserializer<T extends Serializable> extends StdDeserializer<CacheableMap<T>> {

        private static final SafeObjectMapper objectMapper = SafeObjectMapper.Factory.buildRhapsodyStandard();

        public CacheableMapDeserializer() {
            this(null);
        }

        public CacheableMapDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public CacheableMap<T> deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException {

            try {
                JsonNode node = jp.getCodec().readTree(jp);

                String className = node.get("__class").asText();
                int ttl = node.get("ttl").asInt();
                int sizeLimit = node.get("sizeLimit").asInt();

                Class<T> clazz = (Class<T>) Class.forName(className);

                CacheableMap<T> cacheableMap =
                        build(clazz, sizeLimit, ttl);

                final Map<Object, Pair<Long, T>> deserializedMap = new HashMap<>(sizeLimit);

                node.get("map").fields().forEachRemaining(mapNode -> {
                    String fieldName = mapNode.getKey();
                    JsonNode valueNode = mapNode.getValue();
                    Pair<Long, T> deserializedPair;
                    String timestampFieldName = valueNode.fieldNames().next();
                    Long timestamp = Long.parseLong(timestampFieldName);
                    if (String.class.equals(clazz)) {
                        deserializedPair = (Pair<Long, T>) Pair.of(
                                timestamp,
                                valueNode.get(timestampFieldName).asText()
                        );
                    } else {
                        deserializedPair = Pair.of(
                                timestamp,
                                objectMapper.readValue(valueNode.get(timestampFieldName).toString(), clazz)
                        );
                    }
                    deserializedMap.put(fieldName, deserializedPair);

                });

                cacheableMap.cacheMap.putAll(deserializedMap);

                return cacheableMap;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class CacheableMapSerializer <T extends Serializable> extends StdSerializer<CacheableMap<T>> {

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
}

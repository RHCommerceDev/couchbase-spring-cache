package com.couchbase.client.spring.cache.wiring;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.IntNode;
import com.rh.rhapsody.commons.deser.jackson.SafeObjectMapper;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class CacheableMapDeserializer<T extends Serializable> extends StdDeserializer<CacheableMap<T>> {

    SafeObjectMapper objectMapper = new SafeObjectMapper();

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
                    CacheableMap.build(clazz, sizeLimit, ttl);

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
                            objectMapper.readValue(valueNode.get(timestampFieldName).asText(), clazz)
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

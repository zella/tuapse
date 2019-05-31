package org.zella.tuapse.model.messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.zella.tuapse.providers.Json;

public class TypedMessage<T> {

    public final String peerId;
    public final T m;

    public TypedMessage(String peerId, T m) {
        this.peerId = peerId;
        this.m = m;
    }

    public String toJsonString() {
        try {
            return Json.mapper.writeValueAsString(JsonNodeFactory.instance.objectNode()
                    .put("peerId", peerId)
                    .put("data", Json.mapper.writeValueAsString(m)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

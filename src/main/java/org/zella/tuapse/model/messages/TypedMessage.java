package org.zella.tuapse.model.messages;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class TypedMessage<T> {

    private static final ObjectMapper mapper = new ObjectMapper();

    public final String peerId;
    public final T m;

    public TypedMessage(String peerId, T m) {
        this.peerId = peerId;
        this.m = m;
    }

    public String toJsonString() {
        try {
            return JsonNodeFactory.instance.objectNode()
                    .put("peerId", peerId)
                    .put("data", mapper.writeValueAsString(m)).asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

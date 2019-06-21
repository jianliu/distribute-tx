package com.jianliu.distributetx.serde;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * class Serializer
 *
 * @author jianliu
 * @since 2019/6/19
 */
public class JacksonSerializer implements Serializer {

    private static ObjectMapper objectMapper = new ObjectMapper();

    public <T> byte[] serialize(T object) throws JsonProcessingException {
        return objectMapper.writeValueAsBytes(object);
    }

    public <T> T deserialize(byte[] bytes, Class<T> clazz) throws IOException {
        return objectMapper.readValue(bytes, clazz);
    }

}

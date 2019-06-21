package com.jianliu.distributetx.serde;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;

/**
 * class Serializer
 *
 * @author jianliu
 * @since 2019/6/19
 */
public interface Serializer {

    <T> byte[] serialize(T object) throws JsonProcessingException;

    <T> T deserialize(byte[] bytes, Class<T> clazz) throws IOException;

}

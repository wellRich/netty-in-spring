package org.gyk.netty_server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * json序列化工具
 *
 * @author gyk
 * @version 1.0.0
 * @createTime 2022年02月21日 17:09:00
 */
@Component
@Slf4j
public class JSONHelper {

    @Resource
    private ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 对象转化为json
     *
     * @param o 对象不能为空
     * @return json字符串
     */
    public String toJSON(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            log.error("toJSON--->{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String toJSONWithPretty(Object o) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
        } catch (JsonProcessingException e) {
            log.error("toJSON--->{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * json转化为对象
     *
     * @param source json字符串
     * @param clazz  对象类
     * @param <T>
     * @return
     */
    public <T> T parseJSONObject(String source, Class<T> clazz) {
        try {
            return objectMapper.readValue(source, clazz);
        } catch (JsonProcessingException e) {
            log.error("parseJSONObject--->{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public <T> T parseJSONObject(String source, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(source, typeReference);
        } catch (JsonProcessingException e) {
            log.error("parseJSONObject--->{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 反序列化对象数组
     *
     * @param source 对象数组json字符串
     * @param clazz  类
     * @param <T>    泛型
     * @return
     */
    public <T> T[] parseJSONArray(String source, Class<T> clazz) {
        try {
            return objectMapper.readerForArrayOf(clazz).readValue(source);
        } catch (JsonProcessingException e) {
            log.error("parseJSONArray--->{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 反序列化对象列表
     *
     * @param source 对象数组json字符串
     * @param clazz  类
     * @param <T>    泛型
     * @return
     */
    public <T> List<T> pareJSONList(String source, Class<T> clazz) {
        try {
            return objectMapper.readerForListOf(clazz).readValue(source);
        } catch (JsonProcessingException e) {
            log.error("pareJSONList--->{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }


    /**
     * 将json字符串转化为jsonNode
     *
     * @param source js字符串
     * @return
     */
    public JsonNode parseJSON(String source) {
        try {
            return objectMapper.readTree(source);
        } catch (JsonProcessingException e) {
            log.error("parseJSON--->{}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // java对象转化为Node
    public ObjectNode parseObject(Object obj){
        return objectMapper.valueToTree(obj);
    }



}

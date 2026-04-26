package com.queueforge.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.data.domain.Page;

import java.io.IOException;

@SuppressWarnings("rawtypes")
public class PageSerializer extends JsonSerializer<Page> {
    @Override
    public void serialize(Page page, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeObjectField("content", page.getContent());
        gen.writeNumberField("totalPages", page.getTotalPages());
        gen.writeNumberField("totalElements", page.getTotalElements());
        gen.writeNumberField("size", page.getSize());
        gen.writeNumberField("number", page.getNumber());
        gen.writeNumberField("numberOfElements", page.getNumberOfElements());
        gen.writeBooleanField("first", page.isFirst());
        gen.writeBooleanField("last", page.isLast());
        gen.writeBooleanField("empty", page.isEmpty());
        gen.writeEndObject();
    }
}

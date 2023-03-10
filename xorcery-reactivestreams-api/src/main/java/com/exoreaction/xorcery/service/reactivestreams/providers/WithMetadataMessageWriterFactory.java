package com.exoreaction.xorcery.service.reactivestreams.providers;

import com.exoreaction.xorcery.service.reactivestreams.api.WithMetadata;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWorkers;
import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.function.Supplier;

public class WithMetadataMessageWriterFactory
        implements MessageWriter.Factory {

    private final ObjectMapper objectMapper;
    private Supplier<MessageWorkers> messageWorkers;

    public WithMetadataMessageWriterFactory(Supplier<MessageWorkers> messageWorkers) {
        objectMapper = new ObjectMapper();
        objectMapper.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.messageWorkers = messageWorkers;
    }

    @Override
    public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
        if (type.equals(WithMetadata.class)) {
            Class<?> eventType = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
            MessageWriter<?> eventWriter = messageWorkers.get().newWriter(eventType, eventType, mediaType);
            if (eventWriter != null) {
                return (MessageWriter<T>) new WithMetadataMessageWriter<>(eventWriter);
            }
        }
        return null;
    }

    class WithMetadataMessageWriter<T>
            implements MessageWriter<WithMetadata<T>> {

        private final MessageWriter<T> eventWriter;

        public WithMetadataMessageWriter(MessageWriter<T> eventReader) {

            this.eventWriter = eventReader;
        }

        @Override
        public void writeTo(WithMetadata<T> instance, OutputStream entityStream) throws IOException {
            objectMapper.writeValue(entityStream, instance.metadata().metadata());
            eventWriter.writeTo(instance.event(), entityStream);
        }
    }
}

package com.exoreaction.xorcery.service.reactivestreams.providers;

import com.exoreaction.xorcery.service.reactivestreams.spi.MessageWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;

public class ByteBufferMessageWriterFactory
        implements MessageWriter.Factory {

    @Override
    public <T> MessageWriter<T> newWriter(Class<?> type, Type genericType, String mediaType) {
        if (ByteBuffer.class.isAssignableFrom(type))
            return (MessageWriter<T>) new MessageWriterImplementation();
        else
            return null;
    }

    class MessageWriterImplementation
            implements MessageWriter<ByteBuffer> {
        @Override
        public void writeTo(ByteBuffer instance, OutputStream out) throws IOException {
            out.write(instance.array(), instance.position(), instance.limit() - instance.position());
        }
    }
}

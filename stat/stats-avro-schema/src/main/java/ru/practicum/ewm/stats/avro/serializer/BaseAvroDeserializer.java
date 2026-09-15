package ru.practicum.ewm.stats.avro.serializer;

import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.io.UncheckedIOException;

public abstract class BaseAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private final DatumReader<T> reader;

    protected BaseAvroDeserializer(Class<T> targetType) {
        try {
            T instance = targetType.getDeclaredConstructor().newInstance();
            this.reader = new SpecificDatumReader<>(instance.getSchema());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Не удалось получить схему Avro-класса " + targetType, e);
        }
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new UncheckedIOException("Не удалось десериализовать Avro-сообщение из топика " + topic, e);
        }
    }
}
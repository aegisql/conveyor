package com.aegisql.conveyor.persistence.converters;

import com.aegisql.conveyor.persistence.core.PersistenceException;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AvroToBytesConverter<O extends SpecificRecordBase> implements ObjectToByteArrayConverter<O> {

    private final Class<O> cls;

    public AvroToBytesConverter(Class<O> cls) {
        this.cls = cls;
    }

    @Override
    public byte[] toPersistence(O obj) {
        try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream())  {
            var binaryEncoder = EncoderFactory.get().binaryEncoder(outputStream, null);
            var datumWriter = new SpecificDatumWriter<>(cls);
            datumWriter.write(obj, binaryEncoder);
            binaryEncoder.flush();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public O fromPersistence(byte[] bytes) {
        try(ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);) {
            var binaryDecoder = DecoderFactory.get().binaryDecoder(inputStream, null);
            var datumReader = new SpecificDatumReader<>(cls);
            return datumReader.read(null, binaryDecoder);
        } catch (IOException e) {
            throw new PersistenceException(e);
        }
    }

    @Override
    public String conversionHint() {
        return "Avro<"+cls.getCanonicalName()+">:byte[]";
    }
}

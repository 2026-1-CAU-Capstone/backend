package com.jazzify.backend.shared.persistence.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.UUID;

@NullMarked
@Converter(autoApply = false)
public class UuidBinaryConverter implements AttributeConverter<UUID, byte[]> {

    @Override
    public byte @Nullable [] convertToDatabaseColumn(@Nullable UUID uuid) {
        if (uuid == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    @Override
    public @Nullable UUID convertToEntityAttribute(byte @Nullable [] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long mostSig = buffer.getLong();
        long leastSig = buffer.getLong();
        return new UUID(mostSig, leastSig);
    }
}


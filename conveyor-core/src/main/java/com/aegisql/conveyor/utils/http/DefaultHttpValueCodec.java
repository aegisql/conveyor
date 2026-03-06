package com.aegisql.conveyor.utils.http;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

final class DefaultHttpValueCodec implements HttpValueCodec {

    @Override
    public EncodedBody encodeBody(Object value) {
        if (value == null) {
            return EncodedBody.json("null");
        }
        if (value instanceof EncodedBody encodedBody) {
            return encodedBody;
        }
        if (value instanceof byte[] bytes) {
            return EncodedBody.binary(bytes);
        }
        if (value instanceof CharSequence || value instanceof Character) {
            String text = value.toString();
            if (SimpleJson.looksLikeJson(text)) {
                return new EncodedBody("application/json", text.getBytes(StandardCharsets.UTF_8));
            }
            return EncodedBody.text(text);
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Enum<?>) {
            return EncodedBody.json(SimpleJson.write(value));
        }
        return EncodedBody.json(SimpleJson.write(value));
    }

    @Override
    public String encodeQueryValue(Object value) {
        Objects.requireNonNull(value, "Query value must not be null");
        if (value instanceof CharSequence || value instanceof Character || value instanceof Number || value instanceof Boolean || value instanceof Enum<?>) {
            return value.toString();
        }
        return SimpleJson.write(value);
    }
}

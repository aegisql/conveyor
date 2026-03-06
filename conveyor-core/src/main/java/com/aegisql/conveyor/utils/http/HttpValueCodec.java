package com.aegisql.conveyor.utils.http;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public interface HttpValueCodec {

    EncodedBody encodeBody(Object value);

    String encodeQueryValue(Object value);

    record EncodedBody(String contentType, byte[] body) {
        public EncodedBody {
            contentType = Objects.requireNonNull(contentType, "Content type must be provided");
            body = Objects.requireNonNull(body, "Body bytes must be provided");
        }

        public static EncodedBody text(String text) {
            return new EncodedBody("text/plain; charset=UTF-8", text.getBytes(StandardCharsets.UTF_8));
        }

        public static EncodedBody json(String json) {
            return new EncodedBody("application/json", json.getBytes(StandardCharsets.UTF_8));
        }

        public static EncodedBody binary(byte[] bytes) {
            return new EncodedBody("application/octet-stream", bytes);
        }
    }
}

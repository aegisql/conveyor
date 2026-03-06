package com.aegisql.conveyor.utils.http;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public interface ConveyorServiceAuthentication {

    default CompletableFuture<Void> prepare(ConveyorServiceClient client) {
        return CompletableFuture.completedFuture(null);
    }

    default void apply(HttpRequest.Builder builder) {
        // default no-op
    }

    default String auditUserId() {
        return null;
    }

    default String auditAuthMode() {
        return "none";
    }

    static ConveyorServiceAuthentication none() {
        return NoneAuthentication.INSTANCE;
    }

    static ConveyorServiceAuthentication basic(String username, String password) {
        return new BasicAuthentication(username, password);
    }

    static ConveyorServiceAuthentication bearer(String token) {
        return new BearerAuthentication(token);
    }

    static ConveyorServiceAuthentication cookie(String cookieHeader) {
        return new CookieAuthentication(cookieHeader);
    }

    static ConveyorServiceAuthentication session(String username, String password) {
        return new SessionAuthentication(username, password);
    }
}

enum NoneAuthentication implements ConveyorServiceAuthentication {
    INSTANCE
}

record BasicAuthentication(String username, String password) implements ConveyorServiceAuthentication {

    BasicAuthentication {
        Objects.requireNonNull(username, "Username must be provided");
        Objects.requireNonNull(password, "Password must be provided");
    }

    @Override
    public void apply(HttpRequest.Builder builder) {
        String token = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        builder.header("Authorization", "Basic " + token);
    }

    @Override
    public String auditUserId() {
        return username;
    }

    @Override
    public String auditAuthMode() {
        return "basic";
    }
}

record BearerAuthentication(String token) implements ConveyorServiceAuthentication {

    BearerAuthentication {
        Objects.requireNonNull(token, "Bearer token must be provided");
    }

    @Override
    public void apply(HttpRequest.Builder builder) {
        builder.header("Authorization", "Bearer " + token);
    }

    @Override
    public String auditAuthMode() {
        return "bearer";
    }
}

record CookieAuthentication(String cookieHeader) implements ConveyorServiceAuthentication {

    CookieAuthentication {
        Objects.requireNonNull(cookieHeader, "Cookie header must be provided");
    }

    @Override
    public void apply(HttpRequest.Builder builder) {
        builder.header("Cookie", cookieHeader);
    }

    @Override
    public String auditAuthMode() {
        return "cookie";
    }
}

final class SessionAuthentication implements ConveyorServiceAuthentication {

    private final String username;
    private final String password;
    private boolean authenticated;

    SessionAuthentication(String username, String password) {
        this.username = Objects.requireNonNull(username, "Username must be provided");
        this.password = Objects.requireNonNull(password, "Password must be provided");
    }

    @Override
    public CompletableFuture<Void> prepare(ConveyorServiceClient client) {
        if (authenticated) {
            return CompletableFuture.completedFuture(null);
        }
        return client.login(username, password).thenRun(() -> authenticated = true);
    }

    @Override
    public String auditUserId() {
        return username;
    }

    @Override
    public String auditAuthMode() {
        return "session";
    }
}

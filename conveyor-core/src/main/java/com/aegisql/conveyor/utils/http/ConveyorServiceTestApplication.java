package com.aegisql.conveyor.utils.http;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConveyorServiceTestApplication {

    private static final String USAGE = """
            Usage:
              ConveyorServiceTestApplication [conveyor] [id]
              ConveyorServiceTestApplication [options]

            Options:
              --base-url <url>       Service base URL (default: http://localhost:8080)
              --conveyor <name>      Conveyor name (default: collector)
              --id <value>           Single ID for the fixed 3-step flow
              --file <path>          Pipe-delimited input file
              --shuffle              Shuffle file rows before sending
              --ttl <value>          Default ttl query parameter
              --request-ttl <value>  Default requestTTL query parameter
              --auth-mode <mode>     none | session | basic | bearer | cookie
              --user <name>          Username for session/basic auth
              --password <value>     Password for session/basic auth
              --token <value>        Bearer token for bearer auth
              --cookie <value>       Cookie header for cookie auth
            """;

    private ConveyorServiceTestApplication() {
    }

    public static void main(String[] args) throws Exception {
        Options options = Options.parse(args);
        ConveyorServiceClient client = ConveyorServiceClient.builder(options.baseUrl)
                .authentication(options.authentication())
                .build();

        if (options.file != null) {
            replayFile(client, options);
            return;
        }

        String id = options.id != null ? options.id : Long.toString(System.currentTimeMillis() / 1000L);
        sendFixedSequence(client, options.conveyor, id, options.defaultProperties());
    }

    private static void sendFixedSequence(ConveyorServiceClient client, String conveyor, String id, Map<String, Object> defaults) {
        placePart(client, conveyor, id, "USER", "{\"name\":\"John D\"}", defaults);
        placePart(client, conveyor, id, "ADDRESS", "{\"zip_code\":\"11111\"}", defaults);
        placePart(client, conveyor, id, "DONE", "{}", defaults);
        System.out.println("Sent 3 part-loader messages to conveyor '" + conveyor + "' with ID=" + id + ".");
    }

    private static void replayFile(ConveyorServiceClient client, Options options) throws IOException {
        List<String> lines = Files.readAllLines(options.file);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Input file is empty: " + options.file);
        }
        Header header = Header.parse(lines.getFirst());
        List<Row> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank()) {
                continue;
            }
            rows.add(header.parseRow(line));
        }
        if (options.shuffle) {
            Collections.shuffle(rows);
        }
        for (Row row : rows) {
            Map<String, Object> mergedProperties = new LinkedHashMap<>(options.defaultProperties());
            mergedProperties.putAll(row.properties());
            if (row.partRequest()) {
                placePart(client, row.conveyor(), row.id(), row.label(), row.body(), mergedProperties);
            } else {
                placeStaticPart(client, row.conveyor(), row.label(), row.body(), mergedProperties);
            }
        }
    }

    private static void placePart(
            ConveyorServiceClient client,
            String conveyor,
            String id,
            String label,
            String body,
            Map<String, Object> properties
    ) {
        boolean accepted = client.part(conveyor)
                .id(id)
                .label(label)
                .addProperties(properties)
                .value(body)
                .place()
                .join();
        System.out.println("part " + conveyor + " " + id + " " + label + " -> " + accepted);
    }

    private static void placeStaticPart(
            ConveyorServiceClient client,
            String conveyor,
            String label,
            String body,
            Map<String, Object> properties
    ) {
        var loader = client.staticPart(conveyor)
                .label(label)
                .addProperties(properties);
        if (properties.containsKey("delete") && Boolean.parseBoolean(String.valueOf(properties.get("delete")))) {
            loader = loader.delete();
        } else {
            loader = loader.value(body);
        }
        boolean accepted = loader.place().join();
        System.out.println("static-part " + conveyor + " " + label + " -> " + accepted);
    }

    private record Options(
            String baseUrl,
            String conveyor,
            String id,
            Path file,
            boolean shuffle,
            String ttl,
            String requestTtl,
            String authMode,
            String username,
            String password,
            String token,
            String cookie
    ) {

        private static Options parse(String[] args) {
            Map<String, String> named = new LinkedHashMap<>();
            List<String> positional = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.startsWith("--")) {
                    if ("--shuffle".equals(arg)) {
                        named.put("shuffle", "true");
                        continue;
                    }
                    if ("--help".equals(arg)) {
                        printUsageAndExit();
                    }
                    if (i + 1 >= args.length) {
                        throw new IllegalArgumentException("Missing value for " + arg);
                    }
                    named.put(arg.substring(2), args[++i]);
                } else {
                    positional.add(arg);
                }
            }

            String baseUrl = named.getOrDefault("base-url", env("BASE_URL", "http://localhost:8080"));
            String conveyor = named.getOrDefault("conveyor",
                    positional.size() > 0 ? positional.get(0) : env("CONVEYOR", "collector"));
            String id = named.getOrDefault("id",
                    positional.size() > 1 ? positional.get(1) : env("ID", null));
            Path file = named.containsKey("file") ? Path.of(named.get("file")) : null;
            boolean shuffle = Boolean.parseBoolean(named.getOrDefault("shuffle", "false"));
            String ttl = named.getOrDefault("ttl", env("TTL", "1 SECONDS"));
            String requestTtl = named.getOrDefault("request-ttl", env("REQUEST_TTL", "100"));
            String authMode = named.getOrDefault("auth-mode", env("AUTH_MODE", "session"));
            String username = named.getOrDefault("user", env("REST_USER", "rest"));
            String password = named.getOrDefault("password", env("REST_PASSWORD", "rest"));
            String token = named.getOrDefault("token", env("BEARER_TOKEN", null));
            String cookie = named.getOrDefault("cookie", env("SESSION_COOKIE", null));
            return new Options(baseUrl, conveyor, id, file, shuffle, ttl, requestTtl, authMode, username, password, token, cookie);
        }

        private static String env(String key, String defaultValue) {
            String value = System.getenv(key);
            return value == null || value.isBlank() ? defaultValue : value;
        }

        private Map<String, Object> defaultProperties() {
            Map<String, Object> properties = new LinkedHashMap<>();
            if (ttl != null) {
                properties.put("ttl", ttl);
            }
            if (requestTtl != null) {
                properties.put("requestTTL", requestTtl);
            }
            return properties;
        }

        private ConveyorServiceAuthentication authentication() {
            return switch (authMode.toLowerCase()) {
                case "none" -> ConveyorServiceAuthentication.none();
                case "basic" -> ConveyorServiceAuthentication.basic(username, password);
                case "bearer" -> ConveyorServiceAuthentication.bearer(require(token, "token"));
                case "cookie" -> ConveyorServiceAuthentication.cookie(require(cookie, "cookie"));
                case "session" -> ConveyorServiceAuthentication.session(username, password);
                default -> throw new IllegalArgumentException("Unsupported auth mode: " + authMode);
            };
        }

        private String require(String value, String name) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Missing required " + name + " for auth mode " + authMode);
            }
            return value;
        }

        private static void printUsageAndExit() {
            System.out.println(usageText());
            System.exit(0);
        }

        private static String usageText() {
            return USAGE;
        }
    }

    private record Header(boolean partHeader, List<String> columns) {

        private static Header parse(String line) {
            List<String> columns = split(line);
            if (columns.size() < 3) {
                throw new IllegalArgumentException("Header must contain at least CONVEYOR_NAME|LABEL|BODY");
            }
            boolean partHeader = columns.size() >= 4 && "ID".equals(columns.get(1));
            return new Header(partHeader, columns);
        }

        private Row parseRow(String line) {
            List<String> values = split(line);
            if (values.size() < columns.size()) {
                throw new IllegalArgumentException("Invalid row, expected " + columns.size() + " columns: " + line);
            }
            String conveyor = values.getFirst();
            if (partHeader) {
                String id = values.get(1);
                String label = values.get(2);
                String body = values.get(3);
                Map<String, Object> properties = extraProperties(values, 4);
                return new Row(conveyor, id, label, body, !id.isBlank(), properties);
            }
            String label = values.get(1);
            String body = values.get(2);
            Map<String, Object> properties = extraProperties(values, 3);
            return new Row(conveyor, null, label, body, false, properties);
        }

        private Map<String, Object> extraProperties(List<String> values, int fromIndex) {
            Map<String, Object> properties = new LinkedHashMap<>();
            for (int i = fromIndex; i < columns.size(); i++) {
                String name = columns.get(i);
                String value = values.get(i);
                if (!value.isBlank()) {
                    properties.put(name, value);
                }
            }
            return properties;
        }

        private static List<String> split(String line) {
            String[] parts = line.split("\\|", -1);
            List<String> values = new ArrayList<>(parts.length);
            Collections.addAll(values, parts);
            return values;
        }
    }

    private record Row(
            String conveyor,
            String id,
            String label,
            String body,
            boolean partRequest,
            Map<String, Object> properties
    ) {
    }
}

package com.aegisql.conveyor.persistence.jdbc;

import com.aegisql.conveyor.persistence.core.Field;
import com.aegisql.conveyor.persistence.core.PersistenceException;
import com.aegisql.conveyor.persistence.jdbc.builders.JdbcPersistenceBuilder;
import com.aegisql.conveyor.persistence.jdbc.init.JdbcInitializationScriptOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * Standalone JDBC persistence initializer.
 */
public class InitConveyorPersistence {

	private static final Set<String> BUILT_IN_TYPES = Set.of(
			"derby",
			"derby-client",
			"derby-memory",
			"mysql",
			"mariadb",
			"oracle",
			"sqlserver",
			"postgres",
			"sqlite",
			"sqlite-memory"
	);

	public static void main(String[] args) throws Exception {
		int exitCode = run(args, System.out, System.err);
		if (exitCode != 0) {
			System.exit(exitCode);
		}
	}

	static int run(String[] args, PrintStream out, PrintStream err) throws Exception {
		Objects.requireNonNull(args, "args must not be null");
		Objects.requireNonNull(out, "out must not be null");
		Objects.requireNonNull(err, "err must not be null");
		try {
			InitRequest request = parse(args);
			if (request.help) {
				out.print(usage());
				return 0;
			}
			validate(request);
			JdbcPersistenceBuilder<?> builder = buildBuilder(request);
			if ("init".equals(request.mode)) {
				builder.autoInit(true);
				builder.init();
				out.println("Initialization completed for " + request.type + " persistence.");
				return 0;
			}

			String script = builder.initializationScript(
					new JdbcInitializationScriptOptions(request.includeCleanup, request.outputFileName())
			);
			if (request.outputFile != null) {
				Files.writeString(request.outputFile, script, StandardCharsets.UTF_8);
				out.println("Initialization script written to " + request.outputFile);
			} else {
				out.print(script);
			}
			return 0;
		} catch (Exception e) {
			err.println("InitConveyorPersistence error: " + e.getMessage());
			return 1;
		}
	}

	private static JdbcPersistenceBuilder<?> buildBuilder(InitRequest request) throws Exception {
		Class<?> keyClass = Class.forName(request.keyClass);
		JdbcPersistenceBuilder<?> builder = JdbcPersistenceBuilder.presetInitializer(request.type, keyClass);
		if (request.host != null) {
			builder = builder.host(request.host);
		}
		if (request.port != null) {
			builder = builder.port(request.port);
		}
		if (request.database != null) {
			builder = builder.database(request.database);
		}
		if (request.schema != null) {
			builder = builder.schema(request.schema);
		}
		if (request.partTable != null) {
			builder = builder.partTable(request.partTable);
		}
		if (request.completedLogTable != null) {
			builder = builder.completedLogTable(request.completedLogTable);
		}
		if (request.user != null) {
			builder = builder.user(request.user);
		}
		if (request.password != null) {
			builder = builder.password(request.password);
		}
		if (!request.properties.isEmpty()) {
			Properties properties = new Properties();
			request.properties.forEach(properties::put);
			builder = builder.properties(properties);
		}
		for (FieldSpec field : request.additionalFields) {
			builder = builder.addField(Class.forName(field.type), field.name);
		}
		for (List<String> uniqueField : request.uniqueFields) {
			builder = builder.addUniqueFields(uniqueField);
		}
		return builder.autoInit(false);
	}

	private static void validate(InitRequest request) {
		if (request.type == null || request.type.isBlank()) {
			throw new PersistenceException("Missing required option --type");
		}
		if (!BUILT_IN_TYPES.contains(request.type)) {
			throw new PersistenceException("Unsupported type '" + request.type + "'. Built-in types only: " + BUILT_IN_TYPES);
		}
		if (request.keyClass == null || request.keyClass.isBlank()) {
			throw new PersistenceException("Missing required option --key-class");
		}
		if (!"script".equals(request.mode) && !"init".equals(request.mode)) {
			throw new PersistenceException("Unsupported mode '" + request.mode + "'. Use script or init.");
		}
	}

	private static InitRequest parse(String[] args) throws Exception {
		InitRequest request = new InitRequest();
		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			switch (arg) {
				case "--help", "-h" -> request.help = true;
				case "--config" -> merge(loadYaml(nextValue(args, ++i, arg)), request);
				case "--mode" -> request.mode = normalize(nextValue(args, ++i, arg));
				case "--type" -> request.type = normalize(nextValue(args, ++i, arg));
				case "--key-class" -> request.keyClass = nextValue(args, ++i, arg);
				case "--host" -> request.host = nextValue(args, ++i, arg);
				case "--port" -> request.port = Integer.parseInt(nextValue(args, ++i, arg));
				case "--database" -> request.database = nextValue(args, ++i, arg);
				case "--schema" -> request.schema = nextValue(args, ++i, arg);
				case "--part-table" -> request.partTable = nextValue(args, ++i, arg);
				case "--completed-log-table" -> request.completedLogTable = nextValue(args, ++i, arg);
				case "--user" -> request.user = nextValue(args, ++i, arg);
				case "--password" -> request.password = nextValue(args, ++i, arg);
				case "--output" -> request.outputFile = Path.of(nextValue(args, ++i, arg));
				case "--include-cleanup" -> request.includeCleanup = true;
				case "--property" -> applyProperty(nextValue(args, ++i, arg), request);
				case "--field" -> applyField(nextValue(args, ++i, arg), request);
				case "--unique-field" -> applyUniqueField(nextValue(args, ++i, arg), request);
				default -> throw new PersistenceException("Unknown argument: " + arg);
			}
		}
		return request;
	}

	@SuppressWarnings("unchecked")
	private static InitRequest loadYaml(String file) throws Exception {
		Yaml yaml = new Yaml();
		try (var reader = Files.newBufferedReader(Path.of(file), StandardCharsets.UTF_8)) {
			Object loaded = yaml.load(reader);
			if (!(loaded instanceof Map<?, ?> rawMap)) {
				throw new PersistenceException("YAML config must be a map of initialization properties");
			}
			Map<String, Object> map = new LinkedHashMap<>();
			rawMap.forEach((k, v) -> map.put(String.valueOf(k), v));
			InitRequest request = new InitRequest();
			request.mode = normalize(stringValue(map, "mode", "script"));
			request.type = normalize(stringValue(map, "type", null));
			request.keyClass = stringValue(map, "keyClass", stringValue(map, "key-class", null));
			request.host = stringValue(map, "host", null);
			request.port = intValue(map, "port");
			request.database = stringValue(map, "database", null);
			request.schema = stringValue(map, "schema", null);
			request.partTable = stringValue(map, "partTable", stringValue(map, "part-table", null));
			request.completedLogTable = stringValue(map, "completedLogTable", stringValue(map, "completed-log-table", null));
			request.user = stringValue(map, "user", null);
			request.password = stringValue(map, "password", null);
			request.includeCleanup = boolValue(map, "includeCleanup", false) || boolValue(map, "include-cleanup", false);
			String output = stringValue(map, "output", stringValue(map, "outputFile", stringValue(map, "output-file", null)));
			if (output != null && !output.isBlank()) {
				request.outputFile = Path.of(output);
			}

			Object properties = map.get("properties");
			if (properties instanceof Map<?, ?> propertyMap) {
				propertyMap.forEach((k, v) -> request.properties.put(String.valueOf(k), String.valueOf(v)));
			}

			Object fields = firstNonNull(map.get("additionalFields"), map.get("additional-fields"));
			if (fields instanceof Iterable<?> iterable) {
				for (Object item : iterable) {
					if (item instanceof Map<?, ?> fieldMap) {
						String type = String.valueOf(fieldMap.get("type"));
						String name = String.valueOf(fieldMap.get("name"));
						request.additionalFields.add(new FieldSpec(type, name));
					}
				}
			}

			Object uniqueFields = firstNonNull(map.get("uniqueFields"), map.get("unique-fields"));
			if (uniqueFields instanceof Iterable<?> iterable) {
				for (Object item : iterable) {
					if (item instanceof Iterable<?> fieldNames) {
						List<String> names = new ArrayList<>();
						for (Object name : fieldNames) {
							names.add(String.valueOf(name));
						}
						request.uniqueFields.add(names);
					}
				}
			}
			return request;
		}
	}

	private static void merge(InitRequest source, InitRequest target) {
		target.mode = source.mode != null ? source.mode : target.mode;
		target.type = source.type != null ? source.type : target.type;
		target.keyClass = source.keyClass != null ? source.keyClass : target.keyClass;
		target.host = source.host != null ? source.host : target.host;
		target.port = source.port != null ? source.port : target.port;
		target.database = source.database != null ? source.database : target.database;
		target.schema = source.schema != null ? source.schema : target.schema;
		target.partTable = source.partTable != null ? source.partTable : target.partTable;
		target.completedLogTable = source.completedLogTable != null ? source.completedLogTable : target.completedLogTable;
		target.user = source.user != null ? source.user : target.user;
		target.password = source.password != null ? source.password : target.password;
		target.outputFile = source.outputFile != null ? source.outputFile : target.outputFile;
		target.includeCleanup = source.includeCleanup || target.includeCleanup;
		target.properties.putAll(source.properties);
		target.additionalFields.addAll(source.additionalFields);
		target.uniqueFields.addAll(source.uniqueFields);
	}

	private static void applyProperty(String property, InitRequest request) {
		String[] parts = property.split("=", 2);
		if (parts.length != 2) {
			throw new PersistenceException("Property must be key=value: " + property);
		}
		request.properties.put(parts[0].trim(), parts[1].trim());
	}

	private static void applyField(String field, InitRequest request) {
		String[] parts = field.split(",", 2);
		if (parts.length != 2) {
			throw new PersistenceException("Field must be type,name: " + field);
		}
		request.additionalFields.add(new FieldSpec(parts[0].trim(), parts[1].trim()));
	}

	private static void applyUniqueField(String field, InitRequest request) {
		String[] parts = field.split(",");
		List<String> uniqueField = new ArrayList<>(parts.length);
		for (String part : parts) {
			uniqueField.add(part.trim());
		}
		request.uniqueFields.add(uniqueField);
	}

	private static String nextValue(String[] args, int index, String option) {
		if (index >= args.length) {
			throw new PersistenceException("Missing value for " + option);
		}
		return args[index];
	}

	private static String stringValue(Map<String, Object> map, String key, String defaultValue) {
		Object value = map.get(key);
		return value == null ? defaultValue : String.valueOf(value);
	}

	private static Integer intValue(Map<String, Object> map, String key) {
		Object value = map.get(key);
		return value == null ? null : Integer.valueOf(String.valueOf(value));
	}

	private static boolean boolValue(Map<String, Object> map, String key, boolean defaultValue) {
		Object value = map.get(key);
		return value == null ? defaultValue : Boolean.parseBoolean(String.valueOf(value));
	}

	private static Object firstNonNull(Object first, Object second) {
		return first != null ? first : second;
	}

	private static String normalize(String value) {
		return value == null ? null : value.toLowerCase(Locale.ROOT);
	}

	private static String usage() {
		return """
				Usage:
				  InitConveyorPersistence --mode script|init --type <engine> --key-class <class> [options]
				  InitConveyorPersistence --config <file.yml> [overrides]

				Options:
				  --mode <script|init>           Default is script.
				  --type <engine>                Built-in engine type only.
				  --key-class <fqcn>             Key class used for CART_KEY DDL.
				  --host <host>
				  --port <port>
				  --database <name>
				  --schema <name>
				  --part-table <name>
				  --completed-log-table <name>
				  --user <name>
				  --password <value>
				  --property <key=value>         Repeatable.
				  --field <type,name>            Repeatable additional field.
				  --unique-field <a,b,c>         Repeatable unique index definition.
				  --include-cleanup              Include commented cleanup SQL in script mode.
				  --output <file>                Write script to file instead of stdout.
				  --config <file.yml>            Load settings from YAML.
				  --help                         Show this message.
				""";
	}

	private static final class InitRequest {
		boolean help;
		String mode = "script";
		String type;
		String keyClass;
		String host;
		Integer port;
		String database;
		String schema;
		String partTable;
		String completedLogTable;
		String user;
		String password;
		boolean includeCleanup;
		Path outputFile;
		Map<String, String> properties = new LinkedHashMap<>();
		List<FieldSpec> additionalFields = new ArrayList<>();
		List<List<String>> uniqueFields = new ArrayList<>();

		String outputFileName() {
			return outputFile == null ? JdbcInitializationScriptOptions.DEFAULT_SCRIPT_FILE : outputFile.getFileName().toString();
		}
	}

	private record FieldSpec(String type, String name) {}
}

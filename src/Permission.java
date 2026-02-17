import java.util.Locale;

public record Permission(String name, String resource, String description) {

    public Permission(String name, String resource, String description) {
        if (description == null || description.isEmpty())
            throw new IllegalArgumentException("Описание не должно быть пустым!");

        this.name = name.toUpperCase(Locale.ROOT).replace(" ", "");
        this.resource = resource.toLowerCase(Locale.ROOT);
        this.description = description;
    }

    public String format() {
        return String.format("%s on %s: %s", name, resource, description);
    }

    public boolean matches(String namePattern, String resourcePattern) {
        if (namePattern == null || resourcePattern == null) {
            return false;
        }

        String normalizedNamePattern = namePattern.toUpperCase(Locale.ROOT).replace(" ", "");
        String normalizedResourcePattern = resourcePattern.toLowerCase(Locale.ROOT);

        return this.name.contains(normalizedNamePattern) &&
                this.resource.contains(normalizedResourcePattern);
    }
}

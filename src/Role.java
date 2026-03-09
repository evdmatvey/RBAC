import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Role {
    private final String id;
    private final String name;
    private final String description;
    private Set<Permission> permissions = new HashSet<Permission>();

    public Role(String name, String description) {
        this.id = "role_" + UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
    }

    public Set<Permission> getPermissions() {
        return Set.copyOf(this.permissions);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }

    public boolean hasPermission(Permission permission) {
        return this.permissions.contains(permission);
    }

    public boolean hasPermission (String permissionName, String resource) {
        if (permissionName == null || resource == null)
            return false;

        return this.permissions.stream().anyMatch(p -> p.matches(permissionName, resource));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Role role = (Role) o;
        return id.equals(role.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Role{")
                .append("id='").append(id).append('\'')
                .append(", name='").append(name).append('\'')
                .append(", description='").append(description).append('\'')
                .append(", permissions=");

        if (permissions.isEmpty()) {
            sb.append("[]");
        } else {
            sb.append("[\n");
            for (Permission permission : permissions) {
                sb.append("    ").append(permission).append("\n");
            }
            sb.append("  ]");
        }

        sb.append('}');
        return sb.toString();
    }

    public String format() {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Role: %s [ID: %s]\n", name, id));

        sb.append(String.format("Description: %s\n", description));

        sb.append(String.format("Permissions (%d):\n", permissions.size()));

        if (permissions.isEmpty()) {
            sb.append("  No permissions assigned\n");
        } else {
            for (Permission permission : permissions) {
                sb.append("  - ").append(permission.format()).append("\n");
            }
        }

        return sb.toString();
    }
}
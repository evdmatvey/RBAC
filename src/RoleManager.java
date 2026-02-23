import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RoleManager implements Repository<Role> {
    private final Map<String, Role> rolesById = new HashMap<>();
    private final Map<String, Role> rolesByName = new HashMap<>();

    @Override
    public void add(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        if (rolesByName.containsKey(role.getName())) {
            throw new IllegalArgumentException("Role with name " + role.getName() + " already exists");
        }
        rolesById.put(role.getId(), role);
        rolesByName.put(role.getName(), role);
    }

    @Override
    public boolean remove(Role role) {
        if (role == null) {
            return false;
        }
        Role removed = rolesById.remove(role.getId());
        if (removed != null) {
            rolesByName.remove(removed.getName());
            return true;
        }
        return false;
    }

    @Override
    public Optional<Role> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(rolesById.get(id));
    }

    @Override
    public List<Role> findAll() {
        return new ArrayList<>(rolesById.values());
    }

    @Override
    public int count() {
        return rolesById.size();
    }

    @Override
    public void clear() {
        rolesById.clear();
        rolesByName.clear();
    }

    public Optional<Role> findByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(rolesByName.get(name));
    }

    public List<Role> findByFilter(RoleFilter filter) {
        if (filter == null) {
            return findAll();
        }
        return rolesById.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        Stream<Role> stream = rolesById.values().stream();

        if (filter != null) {
            stream = stream.filter(filter::test);
        }

        if (sorter != null) {
            stream = stream.sorted(sorter);
        }

        return stream.collect(Collectors.toList());
    }

    public boolean exists(String name) {
        if (name == null) {
            return false;
        }
        return rolesByName.containsKey(name);
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        if (roleName == null || permission == null) {
            throw new IllegalArgumentException("Role name and permission cannot be null");
        }

        Role role = rolesByName.get(roleName);
        if (role == null) {
            throw new NoSuchElementException("Role not found: " + roleName);
        }

        role.addPermission(permission);
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        if (roleName == null || permission == null) {
            throw new IllegalArgumentException("Role name and permission cannot be null");
        }

        Role role = rolesByName.get(roleName);
        if (role == null) {
            throw new NoSuchElementException("Role not found: " + roleName);
        }

        role.removePermission(permission);
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        if (permissionName == null || resource == null) {
            return Collections.emptyList();
        }

        RoleFilter filter = RoleFilters.hasPermission(permissionName, resource);
        return findByFilter(filter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RoleManager that = (RoleManager) o;
        return rolesById.equals(that.rolesById);
    }

    @Override
    public int hashCode() {
        return rolesById.hashCode();
    }
}
package repositories;

import entities.User;
import filters.UserFilter;
import filters.UserFilters;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserManager implements Repository<User> {
    private final Map<String, User> users = new HashMap<>();

    @Override
    public void add(User user) {
        if (user == null) {
            throw new IllegalArgumentException("entities.User cannot be null");
        }
        if (users.containsKey(user.username())) {
            throw new IllegalArgumentException("entities.User with username " + user.username() + " already exists");
        }
        validateUser(user);
        users.put(user.username(), user);
    }

    @Override
    public boolean remove(User user) {
        if (user == null) {
            return false;
        }
        return users.remove(user.username()) != null;
    }

    @Override
    public Optional<User> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public int count() {
        return users.size();
    }

    @Override
    public void clear() {
        users.clear();
    }

    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        UserFilter filter = UserFilters.byUsername(username);
        return users.values().stream()
                .filter(filter::test)
                .findFirst();
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        UserFilter filter = UserFilters.byEmail(email);
        return users.values().stream()
                .filter(filter::test)
                .findFirst();
    }

    public List<User> findByFilter(UserFilter filter) {
        if (filter == null) {
            return findAll();
        }
        return users.values().stream()
                .filter(filter::test)
                .collect(Collectors.toList());
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        Stream<User> stream = users.values().stream();

        if (filter != null) {
            stream = stream.filter(filter::test);
        }

        if (sorter != null) {
            stream = stream.sorted(sorter);
        }

        return stream.collect(Collectors.toList());
    }

    public boolean exists(String username) {
        if (username == null) {
            return false;
        }
        return users.containsKey(username);
    }

    public void update(String username, String newFullName, String newEmail) {
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }

        User existing = users.get(username);
        if (existing == null) {
            throw new NoSuchElementException("entities.User not found: " + username);
        }

        String fullName = newFullName != null ? newFullName : existing.fullName();
        String email = newEmail != null ? newEmail : existing.email();

        User updated = User.create(username, fullName, email);

        if (!existing.email().equals(email)) {
            boolean emailExists = users.values().stream()
                    .anyMatch(u -> u.email().equals(email) && !u.username().equals(username));

            if (emailExists) {
                throw new IllegalArgumentException("Email already in use: " + email);
            }
        }

        users.put(username, updated);
    }

    private void validateUser(User user) {
        boolean emailExists = users.values().stream()
                .anyMatch(u -> u.email().equals(user.email()) && !u.username().equals(user.username()));

        if (emailExists) {
            throw new IllegalArgumentException("Email already in use: " + user.email());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserManager that = (UserManager) o;
        return users.equals(that.users);
    }

    @Override
    public int hashCode() {
        return users.hashCode();
    }
}
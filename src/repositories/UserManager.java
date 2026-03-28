package repositories;

import entities.User;
import filters.UserFilter;
import filters.UserFilters;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserManager implements Repository<User> {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void add(User user) {
        if (user == null) {
            throw new IllegalArgumentException("entities.User cannot be null");
        }

        lock.writeLock().lock();
        try {
            if (users.containsKey(user.username())) {
                throw new IllegalArgumentException("entities.User with username " + user.username() + " already exists");
            }
            validateUser(user);
            users.put(user.username(), user);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(User user) {
        if (user == null) {
            return false;
        }
        lock.writeLock().lock();
        try {
            return users.remove(user.username()) != null;
        } finally {
            lock.writeLock().unlock();
        }
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
        lock.readLock().lock();
        try {
            return new ArrayList<>(users.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> findAllParallel() {
        lock.readLock().lock();
        try {
            return users.values().parallelStream().collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return users.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            users.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(users.get(username));
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }

        lock.readLock().lock();
        try {
            UserFilter filter = UserFilters.byEmail(email);
            return users.values().stream()
                    .filter(filter::test)
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> findByFilter(UserFilter filter) {
        if (filter == null) {
            return findAll();
        }

        lock.readLock().lock();
        try {
            return users.values().stream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> findByFilterParallel(Predicate<User> filter) {
        if (filter == null) {
            return findAllParallel();
        }

        lock.readLock().lock();
        try {
            return users.values().parallelStream()
                    .filter(filter)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> findByFilterParallel(UserFilter filter) {
        if (filter == null) {
            return findAllParallel();
        }

        lock.readLock().lock();
        try {
            return users.values().parallelStream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        lock.readLock().lock();
        try {
            Stream<User> stream = users.values().stream();

            if (filter != null) {
                stream = stream.filter(filter::test);
            }

            if (sorter != null) {
                stream = stream.sorted(sorter);
            }

            return stream.collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
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

        lock.writeLock().lock();
        try {
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
        } finally {
            lock.writeLock().unlock();
        }
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
        lock.readLock().lock();
        try {
            return users.equals(that.users);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int hashCode() {
        lock.readLock().lock();
        try {
            return users.hashCode();
        } finally {
            lock.readLock().unlock();
        }
    }
}
package repositories;

import filters.AssignmentFilter;
import entities.*;
import filters.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AssignmentManager implements Repository<RoleAssignment> {
    private final Map<String, RoleAssignment> assignments = new ConcurrentHashMap<>();
    private final UserManager userManager;
    private final RoleManager roleManager;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public AssignmentManager(UserManager userManager, RoleManager roleManager) {
        this.userManager = userManager;
        this.roleManager = roleManager;
    }

    @Override
    public void add(RoleAssignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment cannot be null");
        }

        User user = assignment.user();
        Role role = assignment.role();

        if (userManager.findByUsername(user.username()).isEmpty()) {
            throw new IllegalArgumentException("entities.User does not exist: " + user.username());
        }

        if (roleManager.findByName(role.getName()).isEmpty()) {
            throw new IllegalArgumentException("entities.Role does not exist: " + role.getName());
        }

        lock.writeLock().lock();
        try {
            boolean hasActiveAssignment = assignments.values().stream()
                    .filter(a -> a.user().equals(user) && a.role().equals(role))
                    .anyMatch(RoleAssignment::isActive);

            if (hasActiveAssignment) {
                throw new IllegalArgumentException("entities.User already has active assignment for role: " + role.getName());
            }

            assignments.put(assignment.assignmentId(), assignment);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        if (assignment == null) {
            return false;
        }
        lock.writeLock().lock();
        try {
            return assignments.remove(assignment.assignmentId()) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<RoleAssignment> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(assignments.get(id));
    }

    @Override
    public List<RoleAssignment> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(assignments.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int count() {
        lock.readLock().lock();
        try {
            return assignments.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            assignments.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<RoleAssignment> findByUser(User user) {
        if (user == null) {
            return Collections.emptyList();
        }
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(a -> a.user().equals(user))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findByRole(Role role) {
        if (role == null) {
            return Collections.emptyList();
        }
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(a -> a.role().equals(role))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        if (filter == null) {
            return findAll();
        }
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(filter::test)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter, Comparator<RoleAssignment> sorter) {
        lock.readLock().lock();
        try {
            Stream<RoleAssignment> stream = assignments.values().stream();

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

    public List<RoleAssignment> getActiveAssignments() {
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(RoleAssignment::isActive)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> getExpiredAssignments() {
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(a -> !a.isActive())
                    .filter(a -> a instanceof TemporaryAssignment)
                    .filter(a -> ((TemporaryAssignment) a).isExpired())
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean userHasRole(User user, Role role) {
        if (user == null || role == null) {
            return false;
        }
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(RoleAssignment::isActive)
                    .filter(a -> a.user().equals(user))
                    .anyMatch(a -> a.role().equals(role));
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        if (user == null || permissionName == null || resource == null) {
            return false;
        }
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(RoleAssignment::isActive)
                    .filter(a -> a.user().equals(user))
                    .map(RoleAssignment::role)
                    .anyMatch(role -> role.hasPermission(permissionName, resource));
        } finally {
            lock.readLock().unlock();
        }
    }

    public Set<Permission> getUserPermissions(User user) {
        if (user == null) {
            return Collections.emptySet();
        }
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(RoleAssignment::isActive)
                    .filter(a -> a.user().equals(user))
                    .map(RoleAssignment::role)
                    .flatMap(role -> role.getPermissions().stream())
                    .collect(Collectors.toSet());
        } finally {
            lock.readLock().unlock();
        }
    }

    public Set<Role> getUserRoles(User user) {
        if (user == null) {
            return Collections.emptySet();
        }
        lock.readLock().lock();
        try {
            return assignments.values().stream()
                    .filter(RoleAssignment::isActive)
                    .filter(a -> a.user().equals(user))
                    .map(RoleAssignment::role)
                    .collect(Collectors.toSet());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void revokeAssignment(String assignmentId) {
        if (assignmentId == null) {
            throw new IllegalArgumentException("Assignment ID cannot be null");
        }

        lock.writeLock().lock();
        try {
            RoleAssignment assignment = assignments.get(assignmentId);
            if (assignment == null) {
                throw new NoSuchElementException("Assignment not found: " + assignmentId);
            }

            if (assignment instanceof PermanentAssignment) {
                ((PermanentAssignment) assignment).revoke();
            } else if (assignment instanceof TemporaryAssignment) {
                assignments.remove(assignmentId);
            } else {
                assignments.remove(assignmentId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        if (assignmentId == null || newExpirationDate == null) {
            throw new IllegalArgumentException("Assignment ID and new expiration date cannot be null");
        }

        lock.writeLock().lock();
        try {
            RoleAssignment assignment = assignments.get(assignmentId);
            if (assignment == null) {
                throw new NoSuchElementException("Assignment not found: " + assignmentId);
            }

            if (!(assignment instanceof TemporaryAssignment)) {
                throw new IllegalArgumentException("Assignment is not temporary: " + assignmentId);
            }

            ((TemporaryAssignment) assignment).extend(newExpirationDate);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssignmentManager that = (AssignmentManager) o;
        lock.readLock().lock();
        try {
            return assignments.equals(that.assignments);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int hashCode() {
        lock.readLock().lock();
        try {
            return assignments.hashCode();
        } finally {
            lock.readLock().unlock();
        }
    }
}
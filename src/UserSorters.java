import java.util.Comparator;

public class UserSorters {

    public static Comparator<User> byUsername() {
        return (u1, u2) -> u1.username().compareTo(u2.username());
    }

    public static Comparator<User> byFullName() {
        return (u1, u2) -> u1.fullName().compareTo(u2.fullName());
    }

    public static Comparator<User> byEmail() {
        return (u1, u2) -> u1.email().compareTo(u2.email());
    }
}
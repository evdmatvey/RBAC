import java.util.Comparator;

public class RoleSorters {

    public static Comparator<Role> byName() {
        return (r1, r2) -> r1.getName().compareTo(r2.getName());
    }

    public static Comparator<Role> byPermissionCount() {
        return (r1, r2) -> {
            int count1 = r1.getPermissions().size();
            int count2 = r2.getPermissions().size();
            return Integer.compare(count1, count2);
        };
    }
}
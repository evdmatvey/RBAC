void main() {
    User user = new User("username", "full name", "email@mail.ru");
    User user2 = new User("operator", "full name", "email@mail.ru");
    Role role = new Role("admin", "admin permissions");
    Permission p1 = new Permission("read", "users", "can read users");
    Permission p2 = new Permission("delete", "users", "can delete users");
    role.addPermission(p1);
    role.addPermission(p2);
    AssignmentMetadata am = AssignmentMetadata.now(user2.username(), "with some reason");


    TemporaryAssignment ta = new TemporaryAssignment(user, role, am);
    ta.extend(LocalDate.parse("2040-02-20").atStartOfDay().toString());
    System.out.println(ta.summary());
    ta.extend(LocalDate.parse("2010-02-20").atStartOfDay().toString());
    System.out.println(ta.summary());

    PermanentAssignment pa = new PermanentAssignment(user, role, am);
    System.out.println(pa.summary());
    pa.revoke();
    System.out.println(pa.summary());
}

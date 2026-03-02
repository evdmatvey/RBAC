public record User(String username, String fullName, String email) {
    public static User create(String username, String fullName, String email) {
        ValidationUtils.requireNonEmpty(username, "username");
        ValidationUtils.requireNonEmpty(fullName, "fullName");
        ValidationUtils.requireNonEmpty(email, "email");

        if (!ValidationUtils.isValidUsername(username))
            throw new IllegalArgumentException("Некорректный формат имени пользователя!");

        if (!ValidationUtils.isValidEmail(email))
            throw new IllegalArgumentException("Некорректный формат email!");

        return new User(username, fullName, email);
    }

    public String format() {
        return String.format("%s (%s) <%s>", username, fullName, email);
    }
}

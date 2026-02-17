public record User(String username, String fullName, String email) {
    public static User create(String username, String fullName, String email) {
        if (username == null || fullName == null || email == null)
            throw new IllegalArgumentException("Поля пользователя не должны быть null!");

        if (username.isEmpty() || fullName.isEmpty() || email.isEmpty())
            throw new IllegalArgumentException("Поля пользователя не должны быть пустыми!");

        if (username.length() < 3 || username.length() > 20)
            throw new IllegalArgumentException("Длина имени пользователя должна быть от 3 до 20 символов!");

        if (!username.matches("^[a-zA-Z0-9_]+$"))
            throw new IllegalArgumentException("Недопустимые символы в имени пользователя!");

        if (!email.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-z]{2,}$"))
            throw new IllegalArgumentException("Некорректный формат email!");

        return new User(username, fullName, email);
    }

    public String format() {
        return String.format("%s (%s) <%s>", username, fullName, email);
    }
}

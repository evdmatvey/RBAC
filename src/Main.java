void main() {
    try {
        User u1 = User.validate("rj332jjklwj", "Ivan Ivan", "ivanrj332@mail.ru");
        User u2 = User.validate("rj", "Ivan Ivan", "ivanrj332@.ru");
    } catch (Exception e) {
        IO.println(e.getMessage());
    }
}

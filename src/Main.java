void main() {
    try {
        User u1 = User.validate("1", "Ivan Ivan", "ivanrj332@mail.ru");

        System.out.println(u1.format());
    } catch (Exception e) {
        IO.println(e.getMessage());
    }
}

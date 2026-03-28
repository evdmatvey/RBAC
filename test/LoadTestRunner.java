import utils.LoadTest;

public class LoadTestRunner {
    public static void main(String[] args) {
        try {
            LoadTest loadTest = new LoadTest();
            loadTest.runLoadTest();
        } catch (InterruptedException e) {
            System.err.println("Load test interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Error running load test: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Read {
    public static void main(String[] args) throws Exception {
        System.out.println("=== BillingServiceImpl ===");
        List<String> b = Files.readAllLines(Paths.get("src/main/java/com/example/Billing/application/service/impl/BillingServiceImpl.java"));
        for (int i : new int[]{218, 294, 307}) {
            System.out.println((i+1) + ": " + b.get(i));
        }
        
        System.out.println("\n=== CostEstimationServiceImpl ===");
        List<String> c = Files.readAllLines(Paths.get("src/main/java/com/example/Billing/application/service/impl/CostEstimationServiceImpl.java"));
        for (int i : new int[]{52, 59, 60, 66, 73, 79, 96, 97, 132, 139, 146}) {
            System.out.println((i+1) + ": " + c.get(i));
        }

        System.out.println("\n=== StripePaymentServiceImpl ===");
        List<String> s = Files.readAllLines(Paths.get("src/main/java/com/example/Billing/application/service/impl/StripePaymentServiceImpl.java"));
        for (int i : new int[]{116, 171, 172, 224, 344, 345, 448, 461, 482}) {
            System.out.println((i+1) + ": " + s.get(i));
        }
    }
}

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryManager {

    // LOGIC ERROR: HashMap is not thread-safe. Concurrent access will crash or corrupt data.
    private Map<String, Integer> inventory = new HashMap<>();
    private List<String> transactionLog = new ArrayList<>();

    public InventoryManager() {
        inventory.put("Widget", 100);
        inventory.put("Gadget", 50);
    }

    // BAD PRACTICE: Synchronizing the entire method is a performance bottleneck.
    public synchronized void addItem(String item, int count) {
        if (inventory.containsKey(item)) {
            // EDGE CASE: Integer Overflow. If count is MAX_INT, adding 1 makes it negative.
            int current = inventory.get(item);
            int newVal = current + count; 
            
            // Logic Error: This check happens AFTER the overflow might have occurred
            if (newVal < 0) { 
                System.out.println("Error: Inventory overflow suspected.");
                return;
            }
            inventory.put(item, newVal);
        } else {
            inventory.put(item, count);
        }
        addToLog("Added " + count + " " + item);
    }

    public boolean purchaseItem(String item, int quantity) {
        // CONCURRENCY BUG: 'check-then-act' race condition.
        // Two threads can pass the "if" check simultaneously before updating.
        if (inventory.containsKey(item) && inventory.get(item) >= quantity) {
            
            // Artificial delay to increase chance of Race Condition during testing
            try { Thread.sleep(50); } catch (InterruptedException e) {}

            int current = inventory.get(item);
            inventory.put(item, current - quantity);
            addToLog("Purchased " + quantity + " " + item);
            return true;
        }
        return false;
    }

    private void addToLog(String message) {
        // PERFORMANCE: String concatenation in loop/frequent calls generates high GC pressure
        transactionLog.add(System.currentTimeMillis() + ": " + message);
        
        // MEMORY LEAK: The log grows indefinitely. No cleanup mechanism.
        if (transactionLog.size() > 100000) {
            System.out.println("Log is getting large...");
        }
    }

    public void processBulkOrders(List<String> items) {
        // LOGIC ERROR: Modifying a list while iterating over it (ConcurrentModificationException)
        for (String item : items) {
            if (item.equals("BANNED_ITEM")) {
                items.remove(item); // This will crash the loop
            } else {
                purchaseItem(item, 1);
            }
        }
    }

    public static void main(String[] args) {
        InventoryManager manager = new InventoryManager();

        // SIMULATION: Multi-threaded stress test
        Runnable customer = () -> {
            for (int i = 0; i < 10; i++) {
                manager.purchaseItem("Widget", 5);
            }
        };

        Thread t1 = new Thread(customer);
        Thread t2 = new Thread(customer);
        Thread t3 = new Thread(customer);

        t1.start();
        t2.start();
        t3.start();
    }
}

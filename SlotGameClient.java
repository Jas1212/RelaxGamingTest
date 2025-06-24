import java.util.*;
import java.awt.Point;
import java.util.Set;

/**
 * @class SlotGameClient
 * @brief Entry point for the slot game client, allowing a user to bet and play rounds of a grid-based slot game.
 *
 * This class interacts with the user via the console, manages player balance,
 * runs the game loop, processes bets and winnings, and handles optional gamble logic.
 *
 * @author Jaskaran Heldestad
 * @date 2025-06-24
 * @version 1.0
 */
public class SlotGameClient {

    /**
     * @brief Main method to run the slot game.
     * 
     * Handles balance input, game rounds, betting, win calculation, optional gamble feature, and replay logic.
     * 
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        double balance = 0;

        // Prompt user for initial balance
        while (true) {
            System.out.print("Enter your starting balance in EUR: ");
            balance = scanner.nextDouble();
            if (balance > 0) break;
            System.out.println("Balance must be positive.");
        }

        boolean continuePlaying = true;

        // Main game loop
        while (continuePlaying && balance > 0) {
            double betAmount = 0;

            // Prompt user for bet amount
            while (true) {
                System.out.print("Your current balance is " + balance + " EUR. How much do you want to bet this round? ");
                betAmount = scanner.nextDouble();
                if (betAmount > 0 && betAmount <= balance) break;
                System.out.println("Invalid bet. Must be positive and no more than your current balance.");
            }

            balance -= betAmount;
            System.out.println("\n--- NEW ROUND ---");
            System.out.println("Betting: " + betAmount + " EUR");

            // Initialize game grid and result tracking
            SlotGameServer.SlotGrid slotGrid = new SlotGameServer.SlotGrid();
            SlotGameServer.GameResult gameResult = new SlotGameServer.GameResult();

            // Generate and display initial grid
            slotGrid.generateInitialGrid();
            System.out.println("Initial Grid:");
            slotGrid.printGrid();

            int round = 1;

            // Repeat while there are winning clusters
            while (true) {
                List<Set<Point>> wins = slotGrid.findWinningClusters();
                if (wins.isEmpty()) break;

                SlotGameServer.RoundResult roundResult = new SlotGameServer.RoundResult(round);
                double roundWin = 0;

                // Calculate payouts for each cluster
                for (Set<Point> cluster : wins) {
                    roundWin += slotGrid.calculatePayout(cluster);
                }

                gameResult.totalWin += roundWin;

                // Destroy winning clusters and apply game mechanics
                List<String> destroyed = slotGrid.destroyClusters(wins);
                roundResult.destroyedSymbols.addAll(destroyed);

                slotGrid.applyAvalanche();
                slotGrid.refillGrid();
                roundResult.gridSnapshot = slotGrid.snapshotGrid();
                gameResult.logRound(roundResult);

                System.out.println("\nGrid after Round " + round + ":");
                slotGrid.printGrid();
                round++;
            }

            // Show final grid after all cascades
            System.out.println("\nFinal grid after all rounds:");
            slotGrid.printGrid();

            System.out.println("Total win from gamble: " + gameResult.totalWin + " EUR");

            // Update balance with winnings
            balance += gameResult.totalWin;
            System.out.println("Updated balance: " + balance + " EUR");

            // Ask user if they want to play again
            if (balance <= 0) {
                System.out.println("No balance left. Game over.");
                break;
            }

            System.out.print("Play another round? (y/n): ");
            String again = scanner.next();
            continuePlaying = again.equalsIgnoreCase("y");
        }

        System.out.println("\nFinal balance: " + balance + " EUR");
        scanner.close();
    }
}

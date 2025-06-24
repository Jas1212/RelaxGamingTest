import java.awt.Point;
import java.util.*;

/**
 * @class SlotGameServer
 * @brief Contains the core logic for a grid-based slot machine game, including symbol management,
 * grid generation, cluster detection, payout calculation, and cascading behavior.
 *
 * This server logic is intended to be used by a client such as SlotGameClient for a full game loop.
 * 
 * @author Your Name
 * @date 2025-06-24
 * @version 1.0
 */
public class SlotGameServer {

    /**
     * @enum Symbol
     * @brief Defines all possible symbols that may appear in the slot grid.
     */
    public enum Symbol {
        H1, H2, H3, H4, L5, L6, L7, L8, WR, BLOCKER, EMPTY;

        @Override
        public String toString() {
            if (this == BLOCKER) return "##";
            if (this == EMPTY) return "  ";
            return super.toString();
        }
    }

    /**
     * @class RoundResult
     * @brief Stores the outcome of a single round within a game session.
     */
    public static class RoundResult {
        int roundNumber;
        List<String> destroyedSymbols = new ArrayList<>();
        Symbol[][] gridSnapshot;

        /**
         * @brief Constructor for a round result.
         * @param roundNumber The round number in the current game.
         */
        public RoundResult(int roundNumber) {
            this.roundNumber = roundNumber;
        }
    }

    /**
     * @class GameResult
     * @brief Holds aggregated results from a full game session, including all rounds and total winnings.
     */
    public static class GameResult {
        List<RoundResult> rounds = new ArrayList<>();
        double totalWin = 0;
        boolean gambleWon = false;

        /**
         * @brief Logs a completed round into the overall game result.
         * @param r The result of the completed round.
         */
        public void logRound(RoundResult r) {
            rounds.add(r);
        }
    }

    /**
     * @class SlotGrid
     * @brief Handles grid creation, random symbol generation, win detection, avalanching, and refilling logic.
     */
    public static class SlotGrid {
        Symbol[][] grid = new Symbol[8][8];
        Map<Symbol, Integer> symbolWeights = new HashMap<>();
        Random random = new Random();

        /**
         * @brief Constructor initializes the symbol weights.
         */
        public SlotGrid() {
            for (Symbol s : Symbol.values()) {
                if (s != Symbol.EMPTY) symbolWeights.put(s, 100);
            }
        }

        /**
         * @brief Generates a random 8x8 symbol grid.
         */
        public void generateInitialGrid() {
            for (int col = 0; col < 8; col++) {
                for (int row = 0; row < 8; row++) {
                    grid[row][col] = getRandomSymbol();
                }
            }
        }

        /**
         * @brief Returns a random symbol based on weighted probabilities.
         * @return A randomly chosen Symbol.
         */
        private Symbol getRandomSymbol() {
            int totalWeight = symbolWeights.values().stream().mapToInt(i -> i).sum();
            int rand = random.nextInt(totalWeight);
            int cumulative = 0;
            for (Map.Entry<Symbol, Integer> entry : symbolWeights.entrySet()) {
                cumulative += entry.getValue();
                if (rand < cumulative) return entry.getKey();
            }
            return Symbol.H1;
        }

        /**
         * @brief Finds all clusters of 5 or more connected, matching symbols.
         * @return A list of clusters, each represented as a set of Points.
         */
        public List<Set<Point>> findWinningClusters() {
            boolean[][] visited = new boolean[8][8];
            List<Set<Point>> clusters = new ArrayList<>();

            for (int r = 0; r < 8; r++) {
                for (int c = 0; c < 8; c++) {
                    if (!visited[r][c] && isPartOfCluster(r, c)) {
                        Set<Point> cluster = new HashSet<>();
                        dfs(r, c, grid[r][c], visited, cluster);
                        if (cluster.size() >= 5) {
                            clusters.add(cluster);
                        }
                    }
                }
            }
            return clusters;
        }

        /**
         * @brief Checks if a given grid cell can be part of a cluster.
         * @param r Row index
         * @param c Column index
         * @return true if the cell is a valid cluster member
         */
        private boolean isPartOfCluster(int r, int c) {
            Symbol s = grid[r][c];
            return s != Symbol.BLOCKER && s != Symbol.EMPTY;
        }

        /**
         * @brief Depth-first search to find connected matching symbols or wilds (WR).
         */
        private void dfs(int r, int c, Symbol target, boolean[][] visited, Set<Point> cluster) {
            if (target == Symbol.WR) return;
            if (r < 0 || c < 0 || r >= 8 || c >= 8 || visited[r][c]) return;
            Symbol current = grid[r][c];
            if (current != target && current != Symbol.WR) return;
            visited[r][c] = true;
            cluster.add(new Point(r, c));
            dfs(r + 1, c, target, visited, cluster);
            dfs(r - 1, c, target, visited, cluster);
            dfs(r, c + 1, target, visited, cluster);
            dfs(r, c - 1, target, visited, cluster);
        }

        /**
         * @brief Destroys symbols in all winning clusters and adjacent blockers.
         * @param clusters A list of winning clusters
         * @return A list of destroyed symbols (with their positions)
         */
        public List<String> destroyClusters(List<Set<Point>> clusters) {
            List<String> destroyed = new ArrayList<>();
            for (Set<Point> cluster : clusters) {
                for (Point p : cluster) {
                    destroyed.add(grid[p.x][p.y] + " at (" + p.x + "," + p.y + ")");
                    grid[p.x][p.y] = Symbol.EMPTY;
                    for (int[] d : new int[][]{{0, 1}, {1, 0}, {0, -1}, {-1, 0}}) {
                        int nx = p.x + d[0], ny = p.y + d[1];
                        if (nx >= 0 && ny >= 0 && nx < 8 && ny < 8 && grid[nx][ny] == Symbol.BLOCKER) {
                            destroyed.add("## at (" + nx + "," + ny + ")");
                            grid[nx][ny] = Symbol.EMPTY;
                        }
                    }
                }
            }
            return destroyed;
        }

        /**
         * @brief Causes all symbols to fall down to fill empty spaces (like gravity).
         */
        public void applyAvalanche() {
            for (int col = 0; col < 8; col++) {
                int emptyRow = 7;
                for (int row = 7; row >= 0; row--) {
                    if (grid[row][col] != Symbol.EMPTY) {
                        grid[emptyRow--][col] = grid[row][col];
                    }
                }
                while (emptyRow >= 0) {
                    grid[emptyRow--][col] = Symbol.EMPTY;
                }
            }
        }

        /**
         * @brief Refills empty grid spaces with new random symbols.
         */
        public void refillGrid() {
            for (int col = 0; col < 8; col++) {
                for (int row = 0; row < 8; row++) {
                    if (grid[row][col] == Symbol.EMPTY) {
                        grid[row][col] = getRandomSymbol();
                    }
                }
            }
        }

        /**
         * @brief Returns a copy of the current grid for result logging.
         * @return A snapshot of the grid.
         */
        public Symbol[][] snapshotGrid() {
            Symbol[][] snapshot = new Symbol[8][8];
            for (int r = 0; r < 8; r++) {
                System.arraycopy(grid[r], 0, snapshot[r], 0, 8);
            }
            return snapshot;
        }

        /**
         * @brief Calculates the payout for a given cluster of matching symbols.
         * @param cluster The cluster of matching symbols.
         * @return The payout amount in EUR.
         */
        public double calculatePayout(Set<Point> cluster) {
            Symbol base = null;
            for (Point p : cluster) {
                if (grid[p.x][p.y] != Symbol.WR) {
                    base = grid[p.x][p.y];
                    break;
                }
            }
            if (base == null || base == Symbol.BLOCKER || base == Symbol.WR) return 0;

            int size = cluster.size();
            int tier = (size <= 8) ? 0 : (size <= 12) ? 1 : (size <= 16) ? 2 : (size <= 20) ? 3 : 4;

            int[][] payoutTable = {
                {5, 6, 7, 8, 10},
                {4, 5, 6, 7, 9},
                {4, 5, 6, 7, 9},
                {3, 4, 5, 6, 7},
                {1, 2, 3, 4, 5},
                {1, 2, 3, 4, 5},
                {1, 2, 3, 4, 5},
                {1, 2, 3, 4, 5},
            };

            int symbolIndex = base.ordinal();
            return payoutTable[symbolIndex][tier];
        }

        /**
         * @brief Prints the current grid to the console.
         */
        public void printGrid() {
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    System.out.print(grid[row][col]);
                    if (col < 7) System.out.print("\t");
                }
                System.out.println();
            }
        }
    }
}

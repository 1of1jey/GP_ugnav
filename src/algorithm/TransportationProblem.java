package algorithm;

import java.util.Arrays;

public final class TransportationProblem {
    // Returns allocation matrix using Northwest Corner Method
    public static int[][] northwestCorner(int[] supply, int[] demand) {
        int m = supply.length, n = demand.length;
        int[][] alloc = new int[m][n];
        int i = 0, j = 0;
        int[] s = Arrays.copyOf(supply, m);
        int[] d = Arrays.copyOf(demand, n);
        while (i < m && j < n) {
            int x = Math.min(s[i], d[j]);
            alloc[i][j] = x;
            s[i] -= x;
            d[j] -= x;
            if (s[i] == 0) i++;
            if (d[j] == 0) j++;
        }
        return alloc;
    }

    // Vogel Approximation Method: returns allocation matrix for cost matrix
    public static int[][] vogelApproximation(int[] supply, int[] demand, int[][] cost) {
        int m = supply.length, n = demand.length;
        int[] s = Arrays.copyOf(supply, m);
        int[] d = Arrays.copyOf(demand, n);
        int[][] alloc = new int[m][n];
        boolean[] rowDone = new boolean[m];
        boolean[] colDone = new boolean[n];
        int remainingRows = m, remainingCols = n;

        while (remainingRows > 0 && remainingCols > 0) {
            int bestRow = -1, bestRowPen = -1;
            int bestCol = -1, bestColPen = -1;

            // Row penalties
            for (int i = 0; i < m; i++) {
                if (rowDone[i] || s[i] == 0) continue;
                int min1 = Integer.MAX_VALUE, min2 = Integer.MAX_VALUE;
                for (int j = 0; j < n; j++) {
                    if (colDone[j] || d[j] == 0) continue;
                    int c = cost[i][j];
                    if (c < min1) {
                        min2 = min1;
                        min1 = c;
                    } else if (c < min2) {
                        min2 = c;
                    }
                }
                int pen = (min2 == Integer.MAX_VALUE) ? min1 : (min2 - min1);
                if (pen > bestRowPen) {
                    bestRowPen = pen;
                    bestRow = i;
                }
            }

            // Column penalties
            for (int j = 0; j < n; j++) {
                if (colDone[j] || d[j] == 0) continue;
                int min1 = Integer.MAX_VALUE, min2 = Integer.MAX_VALUE;
                for (int i = 0; i < m; i++) {
                    if (rowDone[i] || s[i] == 0) continue;
                    int c = cost[i][j];
                    if (c < min1) {
                        min2 = min1;
                        min1 = c;
                    } else if (c < min2) {
                        min2 = c;
                    }
                }
                int pen = (min2 == Integer.MAX_VALUE) ? min1 : (min2 - min1);
                if (pen > bestColPen) {
                    bestColPen = pen;
                    bestCol = j;
                }
            }

            boolean pickRow = bestRowPen >= bestColPen;
            if (bestRow == -1 && bestCol == -1) break;

            if (pickRow) {
                int i = bestRow;
                int bestJ = -1, bestC = Integer.MAX_VALUE;
                for (int j = 0; j < n; j++) {
                    if (colDone[j] || d[j] == 0) continue;
                    if (cost[i][j] < bestC) {
                        bestC = cost[i][j];
                        bestJ = j;
                    }
                }
                int x = Math.min(s[i], d[bestJ]);
                alloc[i][bestJ] += x;
                s[i] -= x;
                d[bestJ] -= x;
                if (s[i] == 0) {
                    rowDone[i] = true;
                    remainingRows--;
                }
                if (d[bestJ] == 0) {
                    colDone[bestJ] = true;
                    remainingCols--;
                }
            } else {
                int j = bestCol;
                int bestI = -1, bestC = Integer.MAX_VALUE;
                for (int i = 0; i < m; i++) {
                    if (rowDone[i] || s[i] == 0) continue;
                    if (cost[i][j] < bestC) {
                        bestC = cost[i][j];
                        bestI = i;
                    }
                }
                int x = Math.min(s[bestI], d[j]);
                alloc[bestI][j] += x;
                s[bestI] -= x;
                d[j] -= x;
                if (s[bestI] == 0) {
                    rowDone[bestI] = true;
                    remainingRows--;
                }
                if (d[j] == 0) {
                    colDone[j] = true;
                    remainingCols--;
                }
            }
        }
        return alloc;
    }
}



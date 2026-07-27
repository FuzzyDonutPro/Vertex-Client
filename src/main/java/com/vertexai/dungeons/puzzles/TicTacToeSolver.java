package com.vertexai.dungeons.puzzles;

import net.minecraft.client.Minecraft;

/**
 * Solves the Tic-Tac-Toe puzzle using a Minimax algorithm to ensure a win or draw against the game.
 */
public class TicTacToeSolver implements PuzzleSolver {

    private boolean solved = false;
    private int[] board = new int[9]; // 0 = empty, 1 = player (X), -1 = opponent (O)
    
    // Timer to prevent clicking too fast and getting flagged
    private int clickDelay = 0; 

    @Override
    public boolean isSolved() {
        return solved;
    }

    @Override
    public void onTick(Minecraft mc) {
        if (solved) return;
        
        if (clickDelay > 0) {
            clickDelay--;
            return;
        }

        // 1. Scan the board (read Item Frames on the wall)
        scanBoard(mc);

        // 2. Check if we won, drew, or lost
        int state = evaluateBoard();
        if (state != 0 || isBoardFull()) {
            solved = true;
            return;
        }

        // 3. Calculate the best move using Minimax
        int bestMove = findBestMove();

        // 4. Execute the move (Pathfind and Click)
        if (bestMove != -1) {
            clickButton(mc, bestMove);
            clickDelay = 20; // 1 second delay between clicks
        }
    }

    private void scanBoard(Minecraft mc) {
        // Placeholder for raycasting the 3x3 Item Frames on the wall
        // In reality, this iterates over entities of type ItemFrame in the room
        // and updates the 'board' array based on the item inside (X, O, or Empty).
    }

    private void clickButton(Minecraft mc, int index) {
        // Placeholder for pathfinding to the button corresponding to 'index'
        // Uses the PathFinder and HumanAimSimulator to click it.
        board[index] = 1; // Assume we clicked it
    }

    private int findBestMove() {
        int bestVal = -1000;
        int bestMove = -1;

        for (int i = 0; i < 9; i++) {
            if (board[i] == 0) {
                board[i] = 1; // Make move
                int moveVal = minimax(0, false);
                board[i] = 0; // Undo move

                if (moveVal > bestVal) {
                    bestMove = i;
                    bestVal = moveVal;
                }
            }
        }
        return bestMove;
    }

    private int minimax(int depth, boolean isMax) {
        int score = evaluateBoard();

        if (score == 10) return score - depth;
        if (score == -10) return score + depth;
        if (isBoardFull()) return 0;

        if (isMax) {
            int best = -1000;
            for (int i = 0; i < 9; i++) {
                if (board[i] == 0) {
                    board[i] = 1;
                    best = Math.max(best, minimax(depth + 1, false));
                    board[i] = 0;
                }
            }
            return best;
        } else {
            int best = 1000;
            for (int i = 0; i < 9; i++) {
                if (board[i] == 0) {
                    board[i] = -1;
                    best = Math.min(best, minimax(depth + 1, true));
                    board[i] = 0;
                }
            }
            return best;
        }
    }

    private int evaluateBoard() {
        int[][] winLines = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // Rows
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // Cols
            {0, 4, 8}, {2, 4, 6}             // Diagonals
        };

        for (int[] line : winLines) {
            if (board[line[0]] != 0 && board[line[0]] == board[line[1]] && board[line[1]] == board[line[2]]) {
                if (board[line[0]] == 1) return 10;
                else if (board[line[0]] == -1) return -10;
            }
        }
        return 0;
    }

    private boolean isBoardFull() {
        for (int i = 0; i < 9; i++) {
            if (board[i] == 0) return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return "Tic-Tac-Toe Solver";
    }
}

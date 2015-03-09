package nl.tue.s2id90.group11;

import java.util.List;
import nl.tue.s2id90.draughts.DraughtsState;
import nl.tue.s2id90.draughts.player.DraughtsPlayer;
import nl.tue.s2id90.game.GameState;
import org10x10.dam.game.Move;

/**
 * An alpha-beta player
 * @author Rob
 * @author Dennis
 */
public class AlphaBetaPlayer extends DraughtsPlayer {
    private boolean shouldStop = false;
    private int lastScore = 0;

    // Bonus for piece position; index 0 = home, index 9 = other side.
    private final static int[] ROW_BONUS = {
        // Staying home is preferable to moving one forward, in order to defend
        // against incoming pieces from the opponent.
        10,
        // Otherwise, use Fibonacci to prefer moving forward over moving one by one.
        1,
        2,
        3,
        5,
        8,
        13,
        21,
        34,
        // Note: The game engine should automatically convert this piece to a king,
        // so a piece at position 9 is not possible. Nevertheless, define a value
        // in case the engine has a bug.
        3000
    };
    
    @Override
    /** @return a move**/
    public Move getMove(DraughtsState state) {
        Move bestMove = state.getMoves().get(0);
        int reachedDepth = 0;
        try {
            int maxDepth = 1;
            List<Move> moves = state.getMoves();
            // Find best move using iterative deepening
            while (maxDepth < 200) {
                bestMove = getBestMove(state, maxDepth, moves);
                reachedDepth = maxDepth;
                maxDepth++;
            }
        } catch (AIStoppedException ex) {
        }
        System.out.println("Reached depth: " + reachedDepth);
        return bestMove;
    }

    @Override
    public Integer getValue() {
        return lastScore;
    }
    
    // Return best move for this state. This function executes the first
    //      step of the alphabeta search. By splitting the first step from
    //      the other we can more easily optimize this step and we don't have
    //      to return the best move on all depths
    Move getBestMove(GameState state, int maxDepth, List<Move> moves) throws AIStoppedException {
        Move bestMove = null;
        int bestScore = 0;
        if (state.isWhiteToMove()) {
            // Find best move for white player (highest alpha-beta score)
            int maxScore = Integer.MIN_VALUE;
            for (Move move : moves) {
                state.doMove(move);
                int score = alphaBeta(new GameNode(state), maxDepth - 1,
                        maxScore, Integer.MAX_VALUE);
                if (score > maxScore) {
                    maxScore = score;
                    bestMove = move;
                    bestScore = maxScore;
                }
                state.undoMove(move);
            }
        } else {
            // Find best move for black player (lowest alpha-beta score)
            int minScore = Integer.MAX_VALUE;
            for (Move move : moves) {
                state.doMove(move);
                int score = alphaBeta(new GameNode(state), maxDepth - 1,
                        Integer.MIN_VALUE, minScore);
                if (score < minScore) {
                    minScore = score;
                    bestMove = move;
                    bestScore = minScore;
                }
                state.undoMove(move);
            }
        }
        lastScore = bestScore; // Store best score for use in getValue()
        // Move bestMove to the first position so it gets evaluated first on the next iteration
        if (moves.get(0) != bestMove) {
            moves.remove(bestMove);
            moves.add(0, bestMove);
        }
        return bestMove;
    }
    
    @Override
    public void stop() {
        shouldStop = true;
    }
    
    // Standard alpha-beta algorithm with stop-check to enable stopping
    // when deepening iteratively
    // White maximizes score, black minimizes
    int alphaBeta(GameNode node, int remainingDepth, int alpha, int beta) throws AIStoppedException {
        GameState state = node.getGameState();
        if (remainingDepth == 0 || state.isEndState()) {
            return evaluate((DraughtsState) state);
        }
        if (shouldStop) {
            shouldStop = false;
            throw new AIStoppedException();
        }
        List<Move> moves = state.getMoves();
        if (state.isWhiteToMove()) { // Maximizing player
            for (Move move : moves) {
                state.doMove(move);
                alpha = Math.max(alpha, alphaBeta(new GameNode(state), remainingDepth - 1, alpha, beta));
                state.undoMove(move);
                if (beta <= alpha) {
                    break;
                }
            }
            return alpha;
        } else { // Minimizing player
            for (Move move : moves) {
                state.doMove(move);
                beta = Math.min(beta, alphaBeta(new GameNode(state), remainingDepth - 1, alpha, beta));
                state.undoMove(move);
                if (beta <= alpha) {
                    break;
                }
            }
            return beta;
        }
    }

    final static private int SCORE_WHITE_WIN = Integer.MAX_VALUE - 1;
    final static private int SCORE_BLACK_WIN = Integer.MIN_VALUE + 1;
    
    int evaluate(DraughtsState ds) {
        // Check if someone won
        if (ds.isEndState()) {
            if (ds.isWhiteToMove()) {
                return SCORE_BLACK_WIN;
            } else {
                return SCORE_WHITE_WIN;
            }
        }

        // Calculate score of all pieces that are on the board
        int whitePieceScore = 0;
        int blackPieceScore = 0;
        int[] pieces = ds.getPieces();
        for (int i = 0; i < pieces.length; ++i) {
            int piece = pieces[i];
            // The 10x10 board has 10 rows, and 5 possible pieces per row.
            // Pieces that are close to the other end are preferable,
            // because they are more likely to become a king.
            // "The other end" = row 0 for white,
            // "The other end" = row 9 for black.
            int row = (int)Math.floor((i - 1) / 5);

            switch (piece) {
                case DraughtsState.WHITEKING:
                    whitePieceScore += 3000;
                    // Getting a king home is preferred over keeping a king at the end.
                    whitePieceScore += row;
                    break;
                case DraughtsState.WHITEPIECE:
                    whitePieceScore += 1000;
                    whitePieceScore += ROW_BONUS[9 - row];
                    break;
                case DraughtsState.BLACKKING:
                    blackPieceScore += 3000;
                    // Getting a king home is preferred over keeping a king at the end.
                    blackPieceScore += 9 - row;
                    break;
                case DraughtsState.BLACKPIECE:
                    blackPieceScore += 1000;
                    blackPieceScore += ROW_BONUS[row];
                    break;
            }
        }

        int pieceScore = whitePieceScore - blackPieceScore;
        
        
        int totalScore = pieceScore;
        return totalScore;
    }

    private static class AIStoppedException extends Exception {

        public AIStoppedException() {
        }
    }
}

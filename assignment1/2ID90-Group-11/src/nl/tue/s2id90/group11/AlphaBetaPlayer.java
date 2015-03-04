package nl.tue.s2id90.group11;

import java.util.List;
import nl.tue.s2id90.draughts.DraughtsState;
import nl.tue.s2id90.draughts.player.DraughtsPlayer;
import nl.tue.s2id90.game.GameState;
import org10x10.dam.game.Move;

/**
 * An alpha-beta player
 * @author dennis
 */
public class AlphaBetaPlayer extends DraughtsPlayer {
    private boolean shouldStop = false;
    private int lastScore = 0;
    
    @Override
    /** @return a random move **/
    public Move getMove(DraughtsState state) {
        Move bestMove = state.getMoves().get(0);
        int reachedDepth = 0;
        try {
            int maxDepth = 1;
            List<Move> moves = state.getMoves();
            while (maxDepth < 200) {
                bestMove = getBestMove(state, maxDepth, moves);
                reachedDepth = maxDepth;
                maxDepth++;
            }
        } catch (AIStoppedException ex) {
            System.out.println("Reached depth: " + reachedDepth);
            
        }
        return bestMove;
    }

    @Override
    public Integer getValue() {
        return lastScore;
    }
    
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
        for (int piece : pieces) {
            switch (piece) {
                case DraughtsState.WHITEKING:
                    whitePieceScore += 3;
                    break;
                case DraughtsState.WHITEPIECE:
                    whitePieceScore += 1;
                    break;
                case DraughtsState.BLACKKING:
                    blackPieceScore += 3;
                    break;
                case DraughtsState.BLACKPIECE:
                    blackPieceScore += 1;
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

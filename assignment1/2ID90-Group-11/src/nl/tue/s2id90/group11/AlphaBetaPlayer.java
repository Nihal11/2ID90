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
    private boolean shouldStop;
    private int lastScore = 0;
    
    @Override
    /** @return a random move **/
    public Move getMove(DraughtsState s) {
        Move bestMove = s.getMoves().get(0);
        try {
            shouldStop = false;
            int maxDepth = 1;
            while (maxDepth < 200) {
                bestMove = getBestMove(s, maxDepth);
                System.out.println("Depth: " + maxDepth);
                maxDepth++;
            }
        } catch (AIStoppedException ex) {
            
        }
        return bestMove;
    }

    @Override
    public Integer getValue() {
        return lastScore;
    }
    
    Move getBestMove(GameState state, int maxDepth) throws AIStoppedException {
        Move bestMove = null;
        List<Move> moves = state.getMoves();
        int bestScore = 0;
        if (state.isWhiteToMove()) {
            // Find best move for white player (highest alpha-beta score)
            int maxScore = Integer.MIN_VALUE;
            for (Move move : moves) {
                state.doMove(move);
                int score = alphaBeta(new GameNode(state), maxDepth - 1,
                        Integer.MIN_VALUE, Integer.MAX_VALUE);
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
                        Integer.MIN_VALUE, Integer.MAX_VALUE);
                if (score < minScore) {
                    minScore = score;
                    bestMove = move;
                    bestScore = minScore;
                }
                state.undoMove(move);
            }
        }
        lastScore = bestScore; // Store best score for use in getValue()
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
    
    int evaluate(DraughtsState ds) {
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
                    blackPieceScore -= 3;
                    break;
                case DraughtsState.BLACKPIECE:
                    blackPieceScore -= 1;
                    break;
            }
        }
        int pieceScore = whitePieceScore + blackPieceScore;
        
        // Check if someone won or if there is a draw
        if (ds.isEndState()) {
            if (whitePieceScore == 0) { // Black win
                return Integer.MIN_VALUE + 1;
            } else if (blackPieceScore == 0) { // White win
                return Integer.MAX_VALUE - 1;
            } else { // Draw
                return 0;
            }
        }
        
        int totalScore = pieceScore;
        return totalScore;
    }

    private static class AIStoppedException extends Exception {

        public AIStoppedException() {
        }
    }
}

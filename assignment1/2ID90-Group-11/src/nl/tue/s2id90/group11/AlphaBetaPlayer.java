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
    private final static int maxDepth = 8;
    
    @Override
    /** @return a random move **/
    public Move getMove(DraughtsState s) {
        return getBestMove(s);
    }

    @Override
    public Integer getValue() {
        return 0;
    }
    
    Move getBestMove(GameState state) {
        Move bestMove = null;
        List<Move> moves = state.getMoves();
        if (state.isWhiteToMove()) {
            int maxScore = Integer.MIN_VALUE;
            for (Move move : moves) {
                state.doMove(move);
                int score = alphaBeta(new GameNode(state), maxDepth - 1,
                        Integer.MIN_VALUE, Integer.MAX_VALUE);
                System.out.println(score);
                if (score > maxScore) {
                    maxScore = score;
                    bestMove = move;
                }
                state.undoMove(move);
            }
        } else {
            int minScore = Integer.MAX_VALUE;
            for (Move move : moves) {
                state.doMove(move);
                int score = alphaBeta(new GameNode(state), maxDepth - 1,
                        Integer.MIN_VALUE, Integer.MAX_VALUE);
                System.out.println(score);
                if (score < minScore) {
                    minScore = score;
                    bestMove = move;
                }
                state.undoMove(move);
            }
        }
        return bestMove;
    }
    
    // TODO: Add stop() method as described in assignment1.pdf->section 2
    // White maximizes score, black minimizes
    int alphaBeta(GameNode node, int remainingDepth, int alpha, int beta) {
        GameState state = node.getGameState();
        if (remainingDepth == 0 || state.isEndState()) {
            return evaluate((DraughtsState) state);
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
        int pieceScore = 0;
        int[] pieces = ds.getPieces();
        for (int piece : pieces) {
            switch (piece) {
                case DraughtsState.WHITEKING:
                    pieceScore += 3;
                    break;
                case DraughtsState.WHITEPIECE:
                    pieceScore += 1;
                    break;
                case DraughtsState.BLACKKING:
                    pieceScore -= 3;
                    break;
                case DraughtsState.BLACKPIECE:
                    pieceScore -= 1;
                    break;
            }
        }
        
        int totalScore = pieceScore;
        return totalScore;
    }
}

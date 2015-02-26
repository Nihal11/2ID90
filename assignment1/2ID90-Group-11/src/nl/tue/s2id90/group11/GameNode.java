package nl.tue.s2id90.group11;

import nl.tue.s2id90.game.GameState;
import org10x10.dam.game.Move;

/**
 *
 * @author dennis
 */
public class GameNode {
    private GameState gameState;
    private Move bestMove;
    
    GameNode(GameState gameState) {
        this.gameState = gameState;
    }
    
    GameState getGameState() {
        return this.gameState;
    }
    
    void setBestMove(Move bestMove) {
        this.bestMove = bestMove;
    }
    
    Move getBestMove() {
        return this.bestMove;
    }
}

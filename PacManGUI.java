import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

/* Main GUI Class */
public class PacManGUI {
    public static void main(String[] args) {
        // GUI Swing thread
        javax.swing.SwingUtilities.invokeLater(() -> {
            // Show welcome message and get starting level
            String welcome = "Welcome to the Pac-Man game! Can you complete all levels? 😊\n"
                    + "Enter 1-4 to start at that level, or 0 to start from level 1 and play all levels.";
            String input = JOptionPane.showInputDialog(null, welcome, "Pac-Man", JOptionPane.PLAIN_MESSAGE);
            int start = 1;
            try {
                //convert input into a start level
                int v = Integer.parseInt((input == null || input.trim().isEmpty()) ? "0" : input.trim());
                if (v < 0) v = 0;
                if (v > 4) v = 4;
                start = (v == 0) ? 1 : v;
            } catch (Exception e) {
                start = 1;
            }
            // Create the main game window

            JFrame frame = new JFrame("Pac-Man GUI");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            // Create game model and panel


            PMGameModel game = new PMGameModel(start);
            PMPanel panel = new PMPanel(game);
            //add menu and panel to frame

            frame.setJMenuBar(createMenu(frame, game, panel));
            frame.add(panel);
            frame.setSize(520, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            panel.requestFocusInWindow();
        });
    }
    //create new game and quit menu items

    private static JMenuBar createMenu(JFrame frame, PMGameModel game, PMPanel panel) {
        JMenuBar mb = new JMenuBar();
        JMenu menu = new JMenu("Game");
        JMenuItem newGame = new JMenuItem("New");
        JMenuItem quit = new JMenuItem("Quit");
        newGame.addActionListener(e -> {
            //restart game at any level
            String input = JOptionPane.showInputDialog(frame,
                    "Enter start level (1-4) or 0 to start from level 1:", "New Game", JOptionPane.PLAIN_MESSAGE);
            int start = 1;
            try {
                int v = Integer.parseInt((input == null || input.trim().isEmpty()) ? "0" : input.trim());
                if (v < 0) v = 0;
                if (v > 4) v = 4;
                start = (v == 0) ? 1 : v;
            } catch (Exception ex) {
                start = 1;
            }
            //start game at selected level
            game.startAt(start);
            panel.repaint();
            panel.requestFocusInWindow();
        });
        //Quit option to exit game
        quit.addActionListener(e -> System.exit(0));
        //add items to menu bar
        menu.add(newGame);
        menu.addSeparator();
        menu.add(quit);
        mb.add(menu);
        return mb;
    }
    //game playing logic like movement, collisions, level progression
}


class PMGameModel {
    public final int size = 10; //grid is 10x10
    private String[][] grid;
    public PMPlayer player;
    public PMGhost[] ghosts;
    public int level; //current level
    public int score;
    private final int maxLevel = 4;
    private final int[] pelletsPerLevel = {4, 8, 12, 16}; //pellets per level is defined here
    public int pelletsEaten;
    public int invincibleTimer;
    private Random rand = new Random(); //random generator/

    public PMGameModel(int startLevel) {
        startAt(startLevel);
    }

    public int getMaxLevel() { return maxLevel; }
    //start game at specified level

    public void startAt(int startLevel) {
        if (startLevel < 1) startLevel = 1;
        if (startLevel > maxLevel) startLevel = maxLevel;
        this.level = startLevel;
        this.score = 0;
        this.invincibleTimer = 0;
        initLevel();
    }

    public void initLevel() {
        grid = new String[size][size];
        for (int r = 0; r < size; r++) for (int c = 0; c < size; c++) grid[r][c] = "-";

        // fixed obstacles
        grid[2][4] = "#"; grid[1][7] = "#"; grid[5][5] = "#"; grid[7][2] = "#"; grid[8][8] = "#";

        // pellets
        int totalPellets = pelletsPerLevel[level - 1];
        pelletsEaten = 0;
        for (int k = 0; k < totalPellets; k++) {
            int r, c;
            do { r = rand.nextInt(size); c = rand.nextInt(size); } while (!grid[r][c].equals("-"));
            grid[r][c] = ".";
        }

        // two power ups
        for (int i = 0; i < 2; i++) {
            int r, c;
            do { r = rand.nextInt(size); c = rand.nextInt(size); } while (!grid[r][c].equals("-"));
            grid[r][c] = "I";
        }

        // player start bottom left
        player = new PMPlayer(size - 1, 0);
        grid[player.row][player.col] = "P";

        // two ghosts
        ghosts = new PMGhost[2];
        for (int g = 0; g < 2; g++) {
            int r, c;
            do { r = rand.nextInt(size); c = rand.nextInt(size); } while (!grid[r][c].equals("-"));
            ghosts[g] = new PMGhost(r, c);
            grid[r][c] = "G";
        }
    }

    public String[][] getGrid() { return grid; }
    public int getTotalPelletsThisLevel() { return pelletsPerLevel[level - 1]; }

    // Move player returns false when game should end (player died or completed all levels)
    public boolean movePlayer(char move) {
        int newRow = player.row;
        int newCol = player.col;
        if (move == 'w') newRow--;
        else if (move == 's') newRow++;
        else if (move == 'a') newCol--;
        else if (move == 'd') newCol++;

        // invalid move: ignore
        if (newRow < 0 || newRow >= size || newCol < 0 || newCol >= size) return true;
        if (grid[newRow][newCol].equals("#")) return true;

        
        grid[player.row][player.col] = "-";

        // if player moves into ghost
        if (grid[newRow][newCol].equals("G")) {
            PMGhost ghit = ghostAt(newRow, newCol);
            if (invincibleTimer > 0) {
                if (ghit != null) { ghit.alive = false; }
                grid[newRow][newCol] = "-";
            } else {
                return false; // dead
            }
        } else {
            // pellet
            if (grid[newRow][newCol].equals(".")) {
                score++; pelletsEaten++; grid[newRow][newCol] = "-";
            }
            // power up
            if (grid[newRow][newCol].equals("I")) {
                invincibleTimer = 5; grid[newRow][newCol] = "-";
            }
        }

        player.row = newRow; player.col = newCol;
        grid[player.row][player.col] = "P";

        // If player finished level advance immediately BEFORE ghosts move
        if (pelletsEaten == getTotalPelletsThisLevel()) {
            level++;
            if (level > maxLevel) {
                // finished all levels
                return false;
            }
            initLevel();
            return true;
        }

        // move ghosts
        moveGhosts();

        // check collisions after ghosts moved
        for (PMGhost g : ghosts) {
            if (g == null || !g.alive) continue;
            if (g.row == player.row && g.col == player.col) {
                if (invincibleTimer > 0) {
                    g.alive = false;
                    grid[g.row][g.col] = "P";
                } else {
                    return false;
                }
            }
        }

        if (invincibleTimer > 0) invincibleTimer--;
        return true;
    }

    private PMGhost ghostAt(int r, int c) {
        for (PMGhost g : ghosts) if (g != null && g.alive && g.row == r && g.col == c) return g;
        return null;
    }
    //ghost movement logic based on level

    private void moveGhosts() {
        for (PMGhost g : ghosts) {
            if (g == null || !g.alive) continue;
            if (grid[g.row][g.col].equals("G")) grid[g.row][g.col] = "-";

            int newR = g.row, newC = g.col;

            if (level == 1) {
                // random 50% chance to move
                if (rand.nextInt(100) < 50) {
                    int dir = rand.nextInt(4);
                    if (dir == 0 && g.row > 0) newR = g.row - 1;
                    else if (dir == 1 && g.row < size - 1) newR = g.row + 1;
                    else if (dir == 2 && g.col > 0) newC = g.col - 1;
                    else if (dir == 3 && g.col < size - 1) newC = g.col + 1;
                }
            } else {
                // move toward player
                if (player.row < g.row) newR = g.row - 1;
                else if (player.row > g.row) newR = g.row + 1;
                else if (player.col < g.col) newC = g.col - 1;
                else if (player.col > g.col) newC = g.col + 1;
            }

            // Validate: bounds, walls, power-ups, pellets and don't step on other ghosts
            if (newR < 0 || newR >= size || newC < 0 || newC >= size) { newR = g.row; newC = g.col; }
            String cell = grid[newR][newC];
            if (cell.equals("#") || cell.equals("I") || cell.equals(".")) { newR = g.row; newC = g.col; }

            boolean collideOther = false;
            for (PMGhost o : ghosts) {
                if (o != g && o != null && o.alive && o.row == newR && o.col == newC) { collideOther = true; break; }
            }
            if (collideOther) { newR = g.row; newC = g.col; }

            g.row = newR; g.col = newC;

            if (grid[g.row][g.col].equals("P")) {
                // collision handled by caller
            } else {
                grid[g.row][g.col] = "G";
            }
        }
    }
}

/* Entities */
abstract class PMEntity { public int row, col; public PMEntity(int r, int c) { row = r; col = c; } }
class PMPlayer extends PMEntity { public PMPlayer(int r, int c) { super(r, c); } }
class PMGhost extends PMEntity { public boolean alive = true; public PMGhost(int r, int c) { super(r, c); } }

/* Panel & UI here is the game we can see */
class PMPanel extends JPanel {
    private PMGameModel game;

    public PMPanel(PMGameModel g) {
        this.game = g;
        setBackground(Color.BLACK);
        setFocusable(true);
        //keyboard controls

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                char key = Character.toLowerCase(e.getKeyChar());
                if (key == 'q') System.exit(0); // quit game
                if (key == 'w' || key == 'a' || key == 's' || key == 'd') { // movement keys
                    boolean alive = game.movePlayer(key);
                    repaint();
                    if (!alive) {
                        if (game.level > game.getMaxLevel()) {
                            JOptionPane.showMessageDialog(PMPanel.this, "You cleared all levels! Final Score: " + game.score);
                            System.exit(0); // exit game
                        } else {
                            JOptionPane.showMessageDialog(PMPanel.this, "A ghost caught you! Final Score: " + game.score);
                            int res = JOptionPane.showConfirmDialog(PMPanel.this, "Restart at level 1?", "Game Over", JOptionPane.YES_NO_OPTION);
                            if (res == JOptionPane.YES_OPTION) {
                                game.startAt(1); // restart at level 1
                                repaint();
                            } else System.exit(0);
                        }
                    }
                }
            }
        });
    }
    //drawing the game grid

    @Override
    protected void paintComponent(Graphics g0) { // draw the game grid
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0; 
        String[][] grid = game.getGrid();
        int cell = Math.min(getWidth(), getHeight() - 80) / game.size;
        int offsetX = (getWidth() - cell * game.size) / 2;
        int offsetY = 40;

        g.setColor(Color.DARK_GRAY); // background for grid
        g.fillRect(offsetX - 2, offsetY - 2, cell * game.size + 4, cell * game.size + 4);

        for (int r = 0; r < game.size; r++) { // rows
            for (int c = 0; c < game.size; c++) {
                int x = offsetX + c * cell;
                int y = offsetY + r * cell;
                String s = grid[r][c];
                g.setColor(Color.BLACK);
                g.fillRect(x, y, cell, cell);

                switch (s) {
                    case "-": // empty space
                        g.setColor(new Color(30, 30, 30));
                        g.fillRect(x + 2, y + 2, cell - 4, cell - 4);
                        break;
                    case "#": // obstacle
                        g.setColor(Color.RED);
                        g.fillRect(x + 2, y + 2, cell - 4, cell - 4);
                        break;
                    case ".": // pellet
                        g.setColor(Color.WHITE);
                        g.fillOval(x + cell / 2 - 3, y + cell / 2 - 3, 6, 6);
                        break;
                    case "I": // invincibility
                        g.setColor(Color.CYAN);
                        g.fillRect(x + cell / 4, y + cell / 4, cell / 2, cell / 2);
                        break;
                    case "P": {
                        // Pac-Man coming to life:
                        
                        int px = x + 4;
                        int py = y + 4;
                        int pw = cell - 8;
                        if (pw < 4) pw = 4;
                        int ph = pw;

                        // body
                        g.setColor(new Color(255, 204, 0)); // yellow
                        g.fillOval(px, py, pw, ph);

                        
                        Stroke oldStroke = g.getStroke();
                        int strokeW = cell / 12;
                        if (strokeW < 1) strokeW = 1;
                        g.setStroke(new BasicStroke(strokeW));
                        g.setColor(new Color(255, 140, 0)); // orange
                        g.drawOval(px, py, pw, ph);
                        g.setStroke(oldStroke);

                        // eyes
                        int eyeW = cell / 6;
                        if (eyeW < 2) eyeW = 2;
                        int eyeH = eyeW;
                        int leftEyeX = x + cell / 3 - eyeW / 2;
                        int rightEyeX = x + (cell * 2) / 3 - eyeW / 2;
                        int eyeY = y + cell / 3 - eyeH / 2;
                        g.setColor(Color.WHITE);
                        g.fillOval(leftEyeX, eyeY, eyeW, eyeH);
                        g.fillOval(rightEyeX, eyeY, eyeW, eyeH);
                        int pupilW = eyeW / 2;
                        if (pupilW < 1) pupilW = 1;
                        int pupilH = eyeH / 2;
                        g.setColor(Color.BLACK);
                        g.fillOval(leftEyeX + eyeW / 2 - pupilW / 2, eyeY + eyeH / 2 - pupilH / 2, pupilW, pupilH);
                        g.fillOval(rightEyeX + eyeW / 2 - pupilW / 2, eyeY + eyeH / 2 - pupilH / 2, pupilW, pupilH);

                        
                        int centerX = px + pw / 2; 
                        int centerY = py + ph / 2;
                        int mouthStartX = centerX;
                        int mouthEndX = px + pw - (pw / 8);
                        int mouthY = centerY;
                        int mouthStroke = cell / 16;
                        if (mouthStroke < 1) mouthStroke = 1;
                        Stroke prev = g.getStroke();
                        g.setStroke(new BasicStroke(mouthStroke)); // mouth thickness for pacmans mouth
                        g.setColor(Color.BLACK);
                        g.drawLine(mouthStartX, mouthY, mouthEndX, mouthY);
                        g.setStroke(prev);

                        
                        break;
                    }
                    // draw ghost now
                    case "G":
                        g.setColor(Color.WHITE);
                        g.fillOval(x + 4, y + 4, cell - 8, cell - 8); 
                        g.setColor(Color.GRAY); g.setStroke(new BasicStroke(3)); 
                        g.drawOval(x + 4, y + 4, cell - 8, cell - 8);
                }
            }
        }
        // Draw status text

        g.setColor(Color.WHITE);
        g.drawString("Level: " + game.level + "   Score: " + game.score + "   Invincible: " + game.invincibleTimer, 10, 15);
        g.drawString("Controls: w = up, s = down, a = left, d = right, q = quit", 10, 30);
    }
}
//end of code. thank you!
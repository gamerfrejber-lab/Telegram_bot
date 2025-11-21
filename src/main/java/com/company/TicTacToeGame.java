package com.company;

import java.util.Random;

public class TicTacToeGame {
    private final char[][] b = new char[3][3];
    private final Random rnd = new Random();

    public TicTacToeGame() {
        for (int i=0;i<3;i++) for (int j=0;j<3;j++) b[i][j] = '-';
    }

    public boolean makeUserMove(int r, int c) {
        if (r<0 || r>2 || c<0 || c>2) return false;
        if (b[r][c] == '-') {
            b[r][c] = 'X';
            return true;
        }
        return false;
    }

    public void botMove() {
        int tries = 0;
        while (tries < 9) {
            int r = rnd.nextInt(3);
            int c = rnd.nextInt(3);
            if (b[r][c] == '-') {
                b[r][c] = 'O';
                return;
            }
            tries++;
        }
    }

    public boolean checkWin(char p) {
        for (int i=0;i<3;i++) {
            if (b[i][0]==p && b[i][1]==p && b[i][2]==p) return true;
            if (b[0][i]==p && b[1][i]==p && b[2][i]==p) return true;
        }
        if (b[0][0]==p && b[1][1]==p && b[2][2]==p) return true;
        if (b[0][2]==p && b[1][1]==p && b[2][0]==p) return true;
        return false;
    }

    public boolean isDraw() {
        for (int i=0;i<3;i++) for (int j=0;j<3;j++) if (b[i][j]=='-') return false;
        return true;
    }

    public String getBoard() {
        StringBuilder sb = new StringBuilder();
        sb.append("```\n");
        for (int i=0;i<3;i++) {
            for (int j=0;j<3;j++) sb.append(b[i][j]).append(' ');
            sb.append('\n');
        }
        sb.append("```\n");
        return sb.toString();
    }
}

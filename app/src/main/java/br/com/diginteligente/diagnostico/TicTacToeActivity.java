package br.com.diginteligente.diagnostico;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TicTacToeActivity extends Activity {
    private final char[] board = new char[9];
    private final Button[] cells = new Button[9];
    private TextView status;
    private boolean machine = true;
    private boolean ended;
    private int wins;

    @Override protected void onCreate(Bundle state){ super.onCreate(state); wins=getSharedPreferences("dig_games",MODE_PRIVATE).getInt("ttt_wins",0); build(); reset(); }
    private void build(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(20),dp(16),dp(16)); root.setBackgroundColor(Color.rgb(245,248,250));
        TextView title=new TextView(this); title.setText("Jogo da velha"); title.setTextSize(26); title.setTextColor(Color.rgb(29,48,67)); root.addView(title);
        status=new TextView(this); status.setTextSize(16); status.setPadding(0,dp(8),0,dp(12)); root.addView(status);
        GridLayout grid=new GridLayout(this); grid.setColumnCount(3); grid.setRowCount(3);
        for(int i=0;i<9;i++){ final int p=i; Button b=new Button(this); b.setTextSize(34); b.setOnClickListener(v->play(p)); cells[i]=b; GridLayout.LayoutParams lp=new GridLayout.LayoutParams(); lp.width=0; lp.height=dp(100); lp.columnSpec=GridLayout.spec(i%3,1f); grid.addView(b,lp); }
        root.addView(grid);
        Button mode=new Button(this); mode.setText("Modo: contra a maquina"); mode.setAllCaps(false); mode.setOnClickListener(v->{machine=!machine; mode.setText(machine?"Modo: contra a maquina":"Modo: dois jogadores"); reset();}); root.addView(mode);
        Button again=new Button(this); again.setText("Nova partida"); again.setAllCaps(false); again.setOnClickListener(v->reset()); root.addView(again);
        setContentView(root);
    }
    private void reset(){ for(int i=0;i<9;i++){board[i]=' ';cells[i].setText("");cells[i].setEnabled(true);} ended=false; status.setText(machine?"Sua vez | nivel da maquina "+level():"Vez do jogador X"); }
    private void play(int p){ if(ended||board[p]!=' ')return; move(p,'X'); if(finish('X'))return; if(full()){draw();return;} if(machine){cells[0].postDelayed(()->{int ai=chooseMove();move(ai,'O');if(!finish('O')){if(full())draw();else status.setText("Sua vez | nivel "+level());}},280);}else status.setText("Vez do jogador O"); }
    private void move(int p,char mark){board[p]=mark;cells[p].setText(String.valueOf(mark));cells[p].setScaleX(.2f);cells[p].setScaleY(.2f);cells[p].animate().scaleX(1).scaleY(1).setDuration(260).setInterpolator(new OvershootInterpolator()).start(); if(!machine&&mark=='O'){if(!finish('O')){if(full())draw();else status.setText("Vez do jogador X");}}}
    private int chooseMove(){ List<Integer> open=new ArrayList<>();for(int i=0;i<9;i++)if(board[i]==' ')open.add(i); int level=level(); if(level>=2){int win=findWinning('O');if(win>=0)return win;} if(level>=3){int block=findWinning('X');if(block>=0)return block;} if(level>=4&&board[4]==' ')return 4; if(level>=5){int[] corners={0,2,6,8};for(int c:corners)if(board[c]==' ')return c;} return open.get(new Random().nextInt(open.size())); }
    private int findWinning(char m){for(int i=0;i<9;i++)if(board[i]==' '){board[i]=m;boolean w=winner(m);board[i]=' ';if(w)return i;}return-1;}
    private boolean finish(char m){if(!winner(m))return false;ended=true;for(Button b:cells)b.setEnabled(false);if(m=='X'&&machine){wins++;int points=10+level()*2;getSharedPreferences("dig_games",MODE_PRIVATE).edit().putInt("ttt_wins",wins).putInt("total_points",getSharedPreferences("dig_games",MODE_PRIVATE).getInt("total_points",0)+points).apply();status.setText("Vitoria! +"+points+" pontos");}else status.setText(m=='O'&&machine?"A maquina venceu":"Jogador "+m+" venceu");return true;}
    private void draw(){ended=true;status.setText("Empate");}
    private boolean full(){for(char c:board)if(c==' ')return false;return true;}
    private boolean winner(char m){int[][]l={{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}};for(int[]x:l)if(board[x[0]]==m&&board[x[1]]==m&&board[x[2]]==m)return true;return false;}
    private int level(){return Math.min(5,1+wins/2);}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}

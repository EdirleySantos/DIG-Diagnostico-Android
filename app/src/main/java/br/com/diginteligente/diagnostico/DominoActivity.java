package br.com.diginteligente.diagnostico;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DominoActivity extends Activity {
    private final List<Tile> player = new ArrayList<>(), cpu = new ArrayList<>(), stock = new ArrayList<>(), chain = new ArrayList<>();
    private LinearLayout chainView;
    private GridLayout handView;
    private TextView status, score;
    private int left = -1, right = -1;
    private boolean playerTurn, ended;

    @Override protected void onCreate(Bundle state) { super.onCreate(state); buildScreen(); newRound(); }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(12),dp(16),dp(12),dp(12)); root.setBackgroundColor(0xff0d614f);
        TextView title = label("DOMINO 3D",25,Color.WHITE,true); root.addView(title);
        score = label("",13,0xffd8fff3,false); root.addView(score);
        status = label("",16,Color.WHITE,true); status.setPadding(0,dp(8),0,dp(8)); root.addView(status);
        HorizontalScrollView scroll = new HorizontalScrollView(this); scroll.setFillViewport(true); scroll.setBackground(round(0xff08483c,10,0xff52b89b));
        chainView = new LinearLayout(this); chainView.setGravity(Gravity.CENTER_VERTICAL); chainView.setPadding(dp(12),dp(18),dp(12),dp(18)); scroll.addView(chainView,new ViewGroup.LayoutParams(-2,dp(116))); root.addView(scroll,new LinearLayout.LayoutParams(-1,dp(116)));
        TextView your = label("SUAS PECAS",12,0xffd8fff3,true); your.setPadding(0,dp(12),0,dp(6)); root.addView(your);
        handView = new GridLayout(this); handView.setColumnCount(4); root.addView(handView,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions = new LinearLayout(this);
        Button draw = action("Comprar"); draw.setOnClickListener(v->drawForPlayer());
        Button pass = action("Passar"); pass.setOnClickListener(v->passPlayer());
        Button restart = action("Nova rodada"); restart.setOnClickListener(v->newRound());
        actions.addView(draw,new LinearLayout.LayoutParams(0,-2,1)); actions.addView(pass,new LinearLayout.LayoutParams(0,-2,1)); actions.addView(restart,new LinearLayout.LayoutParams(0,-2,1)); root.addView(actions);
        setContentView(root);
    }

    private void newRound() {
        player.clear();cpu.clear();stock.clear();chain.clear();ended=false;left=right=-1;
        List<Tile> deck=new ArrayList<>();for(int a=0;a<=6;a++)for(int b=a;b<=6;b++)deck.add(new Tile(a,b));Collections.shuffle(deck);
        for(int i=0;i<7;i++){player.add(deck.remove(0));cpu.add(deck.remove(0));}stock.addAll(deck);
        Tile starter=bestDouble(player);boolean cpuStarts=false;if(starter==null){starter=bestDouble(cpu);cpuStarts=starter!=null;}if(starter==null){starter=player.get(0);} 
        if(cpuStarts){cpu.remove(starter);placeFirst(starter);playerTurn=true;}else{player.remove(starter);placeFirst(starter);playerTurn=false;}
        render(); if(!playerTurn)status.postDelayed(this::cpuPlay,650);
    }

    private Tile bestDouble(List<Tile> hand){return hand.stream().filter(t->t.a==t.b).max(Comparator.comparingInt(t->t.a)).orElse(null);}
    private void placeFirst(Tile t){chain.add(t);left=t.a;right=t.b;}

    private void render(){
        chainView.removeAllViews();for(Tile t:chain){TextView v=tileView(t,false);chainView.addView(v,new LinearLayout.LayoutParams(dp(70),dp(82)));}
        handView.removeAllViews();for(Tile t:new ArrayList<>(player)){TextView v=tileView(t,true);v.setOnClickListener(x->playPlayer(t,v));GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(82);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(3),dp(3),dp(3),dp(3));handView.addView(v,lp);}
        int total=getSharedPreferences("dig_games",MODE_PRIVATE).getInt("total_points",0);score.setText("Voce: "+player.size()+" pecas   |   Maquina: "+cpu.size()+"   |   Monte: "+stock.size()+"   |   Geral: "+total+" pts");
        if(!ended)status.setText(playerTurn?"Sua vez: toque em uma peca compativel":"A maquina esta pensando...");
    }

    private TextView tileView(Tile t,boolean hand){TextView v=label(t.a+"  |  "+t.b,20,0xff17212b,true);v.setGravity(Gravity.CENTER);v.setBackground(round(hand?0xfffff7dc:Color.WHITE,8,0xffd2b762));v.setElevation(dp(7));v.setPadding(dp(3),dp(4),dp(3),dp(4));return v;}

    private void playPlayer(Tile tile,View view){if(!playerTurn||ended)return;if(!canPlay(tile)){shake(view);status.setText("Essa peca nao combina com "+left+" ou "+right);return;}player.remove(tile);place(tile);animate(view);playerTurn=false;afterMove(true);}
    private boolean canPlay(Tile t){return left<0||t.a==left||t.b==left||t.a==right||t.b==right;}
    private void place(Tile t){
        if(chain.isEmpty()){placeFirst(t);return;}
        if(t.a==left){chain.add(0,new Tile(t.b,t.a));left=t.b;}
        else if(t.b==left){chain.add(0,t);left=t.a;}
        else if(t.a==right){chain.add(t);right=t.b;}
        else {chain.add(new Tile(t.b,t.a));right=t.a;}
    }
    private void afterMove(boolean fromPlayer){render();if(checkEnd())return;if(fromPlayer)status.postDelayed(this::cpuPlay,650);}

    private void cpuPlay(){if(ended)return;Tile choice=cpu.stream().filter(this::canPlay).max(Comparator.comparingInt(t->t.a+t.b+(t.a==t.b?5:0))).orElse(null);while(choice==null&&!stock.isEmpty()){cpu.add(stock.remove(0));choice=cpu.stream().filter(this::canPlay).max(Comparator.comparingInt(t->t.a+t.b)).orElse(null);}if(choice!=null){cpu.remove(choice);place(choice);}playerTurn=true;render();if(checkEnd())return;if(choice==null&&!hasMove(player)&&stock.isEmpty())finishBlocked();}
    private void drawForPlayer(){if(!playerTurn||ended)return;if(stock.isEmpty()){status.setText("O monte esta vazio");return;}Tile t=stock.remove(0);player.add(t);render();status.setText(canPlay(t)?"Voce comprou uma peca que pode jogar":"Peca comprada; compre novamente ou passe");}
    private void passPlayer(){if(!playerTurn||ended)return;if(hasMove(player)){status.setText("Voce ainda possui uma jogada valida");return;}if(!stock.isEmpty()){status.setText("Ainda existem pecas no monte para comprar");return;}playerTurn=false;render();status.postDelayed(this::cpuPlay,500);}
    private boolean hasMove(List<Tile> hand){for(Tile t:hand)if(canPlay(t))return true;return false;}
    private boolean checkEnd(){if(player.isEmpty()){finish(true,"Voce bateu primeiro!");return true;}if(cpu.isEmpty()){finish(false,"A maquina bateu primeiro.");return true;}if(stock.isEmpty()&&!hasMove(player)&&!hasMove(cpu)){finishBlocked();return true;}return false;}
    private void finishBlocked(){int p=sum(player),c=sum(cpu);finish(p<c,p==c?"Rodada bloqueada e empatada.":p<c?"Rodada bloqueada: voce tem menos pontos.":"Rodada bloqueada: a maquina tem menos pontos.");}
    private int sum(List<Tile> hand){int n=0;for(Tile t:hand)n+=t.a+t.b;return n;}
    private void finish(boolean win,String message){ended=true;int points=win?20+sum(cpu):0;if(win){int old=getSharedPreferences("dig_games",MODE_PRIVATE).getInt("total_points",0);getSharedPreferences("dig_games",MODE_PRIVATE).edit().putInt("total_points",old+points).apply();}render();status.setText(message+(win?" +"+points+" pontos":""));new AlertDialog.Builder(this).setTitle(win?"Vitoria":"Fim da rodada").setMessage(status.getText()).setNegativeButton("Sair",(d,w)->finish()).setPositiveButton("Jogar novamente",(d,w)->newRound()).show();}
    private void shake(View v){v.animate().translationX(dp(8)).setDuration(70).withEndAction(()->v.animate().translationX(-dp(8)).setDuration(70).withEndAction(()->v.animate().translationX(0).setDuration(70))).start();}
    private void animate(View v){v.animate().scaleX(1.18f).scaleY(1.18f).rotationY(180).setDuration(230).setInterpolator(new OvershootInterpolator()).start();}
    private Button action(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);return b;}
    private TextView label(String s,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private GradientDrawable round(int fill,int radius,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(stroke!=0)d.setStroke(dp(2),stroke);return d;}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    private static class Tile{final int a,b;Tile(int a,int b){this.a=a;this.b=b;}}
}

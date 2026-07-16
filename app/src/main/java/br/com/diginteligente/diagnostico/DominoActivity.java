package br.com.diginteligente.diagnostico;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
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
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);TextView title = label("DOMINO",25,Color.WHITE,true);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));Button help=action("?");help.setOnClickListener(v->showTutorial());header.addView(help,new LinearLayout.LayoutParams(dp(52),dp(48)));root.addView(header);
        score = label("",13,0xffd8fff3,false); root.addView(score);
        status = label("",16,Color.WHITE,true); status.setPadding(0,dp(8),0,dp(8)); root.addView(status);
        LinearLayout opponent=new LinearLayout(this);opponent.setGravity(Gravity.CENTER);TextView avatar=label("CPU",13,Color.WHITE,true);avatar.setGravity(Gravity.CENTER);avatar.setBackground(round(0xffd95c59,30,Color.WHITE));opponent.addView(avatar,new LinearLayout.LayoutParams(dp(54),dp(54)));TextView back=label("  Pecas fechadas",14,0xffd8fff3,false);opponent.addView(back);root.addView(opponent);
        HorizontalScrollView scroll = new HorizontalScrollView(this); scroll.setFillViewport(true); scroll.setBackground(round(0xff063b31,10,0xff52b89b));
        chainView = new LinearLayout(this); chainView.setGravity(Gravity.CENTER_VERTICAL); chainView.setPadding(dp(12),dp(18),dp(12),dp(18)); scroll.addView(chainView,new ViewGroup.LayoutParams(-2,dp(116))); root.addView(scroll,new LinearLayout.LayoutParams(-1,dp(116)));
        TextView your = label("SUAS PECAS",12,0xffd8fff3,true); your.setPadding(0,dp(12),0,dp(6)); root.addView(your);
        handView = new GridLayout(this); handView.setColumnCount(4); root.addView(handView,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions = new LinearLayout(this);
        Button draw = action("Comprar"); draw.setOnClickListener(v->drawForPlayer());
        Button pass = action("Passar"); pass.setOnClickListener(v->passPlayer());
        Button restart = action("Nova rodada"); restart.setOnClickListener(v->newRound());
        actions.addView(draw,new LinearLayout.LayoutParams(0,-2,1)); actions.addView(pass,new LinearLayout.LayoutParams(0,-2,1)); actions.addView(restart,new LinearLayout.LayoutParams(0,-2,1)); root.addView(actions);
        setContentView(root); if(!getSharedPreferences("dig_games",MODE_PRIVATE).getBoolean("domino_tutorial",false))root.postDelayed(this::showTutorial,350);
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
        chainView.removeAllViews();for(Tile t:chain){View v=tileView(t,false);chainView.addView(v,new LinearLayout.LayoutParams(dp(66),dp(84)));}
        handView.removeAllViews();for(Tile t:new ArrayList<>(player)){View v=tileView(t,true);v.setOnClickListener(x->playPlayer(t,v));GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=dp(86);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(3),dp(3),dp(3),dp(3));handView.addView(v,lp);}
        int total=getSharedPreferences("dig_games",MODE_PRIVATE).getInt("total_points",0);score.setText("Voce: "+player.size()+" pecas   |   Maquina: "+cpu.size()+"   |   Monte: "+stock.size()+"   |   Geral: "+total+" pts");
        if(!ended)status.setText(playerTurn?"Sua vez: toque em uma peca compativel":"A maquina esta pensando...");
    }

    private View tileView(Tile t,boolean hand){DominoTileView v=new DominoTileView(t,hand);v.setElevation(dp(8));return v;}

    private void showTutorial(){getSharedPreferences("dig_games",MODE_PRIVATE).edit().putBoolean("domino_tutorial",true).apply();new AlertDialog.Builder(this).setTitle("Como jogar Domino").setMessage("Toque em uma peca da sua mao para encaixar em uma das pontas da sequencia.\n\nSe nao tiver jogada, compre no monte. Quando o monte acabar e nao houver encaixe, passe a vez.\n\nVence quem terminar as pecas primeiro; em jogo bloqueado, ganha quem tiver a menor soma.").setPositiveButton("Entendi",null).show();}

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
    private float dp(float n){return n*getResources().getDisplayMetrics().density;}
    private static class Tile{final int a,b;Tile(int a,int b){this.a=a;this.b=b;}}
    private class DominoTileView extends View{
        private final Tile tile;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final boolean hand;
        DominoTileView(Tile tile,boolean hand){super(DominoActivity.this);this.tile=tile;this.hand=hand;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();paint.setShadowLayer(dp(3),0,dp(3),0x66000000);paint.setColor(hand?0xfffffff8:Color.WHITE);c.drawRoundRect(new RectF(dp(3),dp(3),w-dp(3),h-dp(6)),dp(8),dp(8),paint);paint.clearShadowLayer();paint.setColor(0xff25282b);paint.setStrokeWidth(dp(2));c.drawLine(dp(8),h/2,w-dp(8),h/2,paint);drawPips(c,tile.a,w/2,h/4,Math.min(w,h/2)*.30f);drawPips(c,tile.b,w/2,h*.75f,Math.min(w,h/2)*.30f);}
        private void drawPips(Canvas c,int value,float cx,float cy,float spread){paint.setColor(0xff17191b);float r=dp(3.2f);float[][]pos={{0,0},{-1,-1},{1,1},{-1,1},{1,-1},{-1,0},{1,0}};int[][]map={{},{0},{1,2},{1,0,2},{1,2,3,4},{1,2,3,4,0},{1,2,3,4,5,6}};for(int index:map[value])c.drawCircle(cx+pos[index][0]*spread,cy+pos[index][1]*spread,r,paint);}
    }
}

package br.com.diginteligente.diagnostico;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Random;

public class TableGameActivity extends Activity {
    private final Random random=new Random(); private LinearLayout root; private TextView board,status; private int[]dice=new int[5]; private int rolls,round,total,player,dealer;
    @Override protected void onCreate(Bundle b){super.onCreate(b);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(24),dp(18),dp(18));root.setBackgroundColor(0xfff5f7f9);setContentView(root);if("blackjack".equals(getIntent().getStringExtra("mode")))blackjack();else dice();}
    private void base(String title){root.removeAllViews();TextView t=text(title,27,0xff1d3043);root.addView(t);board=text("",38,0xff087e6d);board.setGravity(Gravity.CENTER);board.setPadding(0,dp(35),0,dp(30));root.addView(board);status=text("",16,Color.DKGRAY);root.addView(status);}
    private void dice(){base("General | etapa "+(round+1)+"/5");Button roll=new Button(this);roll.setText("Lancar dados");roll.setOnClickListener(v->{if(rolls>=3)return;int sum=0;for(int i=0;i<5;i++){dice[i]=1+random.nextInt(6);sum+=dice[i];}rolls++;board.setText(dice[0]+"  "+dice[1]+"  "+dice[2]+"  "+dice[3]+"  "+dice[4]);status.setText("Jogada "+rolls+"/3 | soma "+sum);if(rolls==3){total+=sum;round++;if(round>=5){award(Math.max(5,total/5));status.setText("Torneio concluido: "+total+" pontos nos dados");roll.setText("Novo torneio");roll.setOnClickListener(x->{round=0;total=0;rolls=0;dice();});}else{roll.setText("Confirmar etapa");roll.setOnClickListener(x->{rolls=0;dice();});}}});root.addView(roll);}
    private void blackjack(){base("Blackjack");player=card()+card();dealer=card()+card();showBlackjack(false);Button hit=new Button(this);hit.setText("Pedir carta");Button stand=new Button(this);stand.setText("Parar");hit.setOnClickListener(v->{player+=card();if(player>=21)finishBlackjack(hit,stand);else showBlackjack(false);});stand.setOnClickListener(v->{while(dealer<17)dealer+=card();finishBlackjack(hit,stand);});root.addView(hit);root.addView(stand);}
    private void showBlackjack(boolean reveal){board.setText("Voce: "+player+"\nBanca: "+(reveal?dealer:"?"));status.setText("Chegue a 21 sem ultrapassar");}
    private void finishBlackjack(Button a,Button b){showBlackjack(true);boolean win=player<=21&&(dealer>21||player>dealer);status.setText(win?"Vitoria! +15 pontos":player==dealer?"Empate":"A banca venceu");if(win)award(15);a.setEnabled(false);b.setText("Nova rodada");b.setOnClickListener(v->blackjack());}
    private int card(){return 1+random.nextInt(10);} private void award(int p){int old=getSharedPreferences("dig_games",MODE_PRIVATE).getInt("total_points",0);getSharedPreferences("dig_games",MODE_PRIVATE).edit().putInt("total_points",old+p).apply();}
    private TextView text(String s,int z,int c){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(c);t.setPadding(0,dp(6),0,dp(8));return t;}private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}

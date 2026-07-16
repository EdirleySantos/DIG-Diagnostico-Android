package br.com.diginteligente.diagnostico;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import java.util.Arrays;
import java.util.Random;

public class RaceActivity extends Activity {
    @Override protected void onCreate(Bundle state){super.onCreate(state);setContentView(new RaceView());}
    private class RaceView extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG); private final float[] progress=new float[10]; private final float[] lane=new float[10]; private final float[] speed=new float[10]; private final int[] colors={0xffe53935,0xff1e88e5,0xff43a047,0xffffb300,0xff8e24aa,0xff00acc1,0xfff4511e,0xff6d4c41,0xff3949ab,0xff7cb342};
        private final Random random=new Random(); private long last; private boolean running=true; private int track; private float steer; private int lapTarget=3;
        RaceView(){super(RaceActivity.this);setBackgroundColor(0xff16352f);for(int i=0;i<10;i++){lane[i]=(i%5-2)*.16f;speed[i]=.055f+random.nextFloat()*.018f;}speed[0]=.064f;last=System.nanoTime();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight();drawTrack(c,w,h);drawHud(c,w);for(int i=9;i>=0;i--)drawCar(c,i,w,h);if(running){long now=System.nanoTime();float dt=Math.min(.04f,(now-last)/1_000_000_000f);last=now;update(dt);postInvalidateOnAnimation();}}
        private void drawTrack(Canvas c,float w,float h){p.setColor(track==0?0xff454b50:track==1?0xff514941:0xff3f4854);c.drawRect(w*.14f,0,w*.86f,h,p);p.setColor(Color.WHITE);p.setStrokeWidth(4);for(int y=0;y<h;y+=80)c.drawLine(w*.5f,y,w*.5f,y+35,p);p.setColor(0xffd5d8da);c.drawRect(w*.13f,0,w*.15f,h,p);c.drawRect(w*.85f,0,w*.87f,h,p);}
        private void drawHud(Canvas c,float w){p.setColor(0xdd10242b);c.drawRoundRect(12,12,w-12,92,14,14,p);p.setColor(Color.WHITE);p.setTextSize(28);c.drawText("Pista "+(track+1)+" | volta "+Math.min(lapTarget,(int)progress[0]+1)+"/"+lapTarget,28,48,p);p.setTextSize(20);c.drawText("Toque esquerda/direita para mudar de faixa",28,78,p);}
        private void drawCar(Canvas c,int i,float w,float h){float phase=progress[i]%1f;float y=h-(phase*h*.82f)-80;float curve=track==1?(float)Math.sin(progress[i]*7)*w*.08f:track==2?(float)Math.sin(progress[i]*13)*w*.1f:0;float x=w*.5f+lane[i]*w+curve;if(i==0)x+=steer*w*.18f;p.setColor(colors[i]);c.drawRoundRect(x-22,y-34,x+22,y+34,10,10,p);p.setColor(0xffbde6f5);c.drawRect(x-15,y-20,x+15,y-7,p);p.setColor(Color.WHITE);p.setTextSize(16);c.drawText(String.valueOf(i+1),x-5,y+10,p);}
        private void update(float dt){for(int i=0;i<10;i++){float boost=i==0?1f:1f+random.nextFloat()*.02f;progress[i]+=speed[i]*boost*dt*10;}if(progress[0]>=lapTarget)finish();else{for(int i=1;i<10;i++)if(progress[i]>=lapTarget){speed[i]=0;}}}
        private void finish(){running=false;Integer[] order=new Integer[10];for(int i=0;i<10;i++)order[i]=i;Arrays.sort(order,(a,b)->Float.compare(progress[b],progress[a]));int place=1;for(int i=0;i<10;i++)if(order[i]==0)place=i+1;int[]points={25,18,15,12,10,8,6,4,2,1};int gained=points[place-1];int old=getSharedPreferences("dig_games",MODE_PRIVATE).getInt("total_points",0);getSharedPreferences("dig_games",MODE_PRIVATE).edit().putInt("total_points",old+gained).putInt("race_track",(track+1)%3).apply();final int pos=place;post(()->new AlertDialog.Builder(RaceActivity.this).setTitle("Corrida concluida").setMessage("Voce terminou em "+pos+"o lugar e ganhou "+gained+" pontos.").setNegativeButton("Sair",(d,w)->RaceActivity.this.finish()).setPositiveButton("Proxima pista",(d,w)->restart()).show());}
        private void restart(){track=(track+1)%3;for(int i=0;i<10;i++){progress[i]=0;speed[i]=.055f+random.nextFloat()*.018f;}speed[0]=.064f;steer=0;running=true;last=System.nanoTime();invalidate();}
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE){steer=e.getX()<getWidth()/2?Math.max(-1,steer-.35f):Math.min(1,steer+.35f);return true;}return true;}
    }
}

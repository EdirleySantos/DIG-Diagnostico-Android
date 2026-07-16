package br.com.diginteligente.diagnostico;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import net.objecthunter.exp4j.ExpressionBuilder;

public class CalculatorActivity extends ImmersiveActivity {
    private EditText expression;
    private TextView result;
    private TextView history;
    private boolean justCalculated;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setBackgroundColor(Color.rgb(255,246,232));
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16), dp(12), dp(16), dp(24)); root.setBackgroundColor(Color.rgb(255,246,232));
        TextView title = new TextView(this); title.setText("Calculadora inteligente"); title.setTextSize(27); title.setTextColor(Color.rgb(94,49,126)); title.setPadding(dp(4),0,0,dp(12)); root.addView(title);
        expression = new EditText(this); expression.setTextSize(24); expression.setHint("Digite um calculo"); expression.setInputType(InputType.TYPE_CLASS_TEXT); expression.setBackground(round(Color.WHITE,12)); expression.setPadding(dp(14),dp(10),dp(14),dp(10)); root.addView(expression);
        result = new TextView(this); result.setText("0"); result.setTextSize(38); result.setGravity(Gravity.END); result.setTextColor(Color.rgb(220,70,91)); result.setBackground(round(Color.rgb(255,226,232),12)); result.setPadding(dp(12),dp(12),dp(12),dp(12)); LinearLayout.LayoutParams resultLp=new LinearLayout.LayoutParams(-1,-2); resultLp.setMargins(0,dp(10),0,dp(10)); root.addView(result,resultLp);
        GridLayout grid = new GridLayout(this); grid.setColumnCount(4);
        String[] keys={"7","8","9","/","4","5","6","*","1","2","3","-","0",".","(","+","sin(","cos(","sqrt(",")","C","<-","pi","="};
        for(String key:keys){ Button b=new Button(this); b.setText(key); b.setTextSize(17); b.setTypeface(null,android.graphics.Typeface.BOLD); b.setTextColor(buttonTextColor(key)); b.setBackground(round(buttonColor(key),10)); b.setElevation(dp(7)); b.setTranslationZ(dp(3)); b.setStateListAnimator(null); b.setOnTouchListener((v,event)->{if(event.getAction()==MotionEvent.ACTION_DOWN){v.animate().translationY(dp(5)).translationZ(0).setDuration(70).start();}else if(event.getAction()==MotionEvent.ACTION_UP||event.getAction()==MotionEvent.ACTION_CANCEL){v.animate().translationY(0).translationZ(dp(3)).setDuration(120).start();}return false;}); b.setOnClickListener(v->press(key)); GridLayout.LayoutParams lp=new GridLayout.LayoutParams(); lp.width=0;lp.height=dp(60);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(4),dp(4),dp(4),dp(5));grid.addView(b,lp); }
        root.addView(grid,new LinearLayout.LayoutParams(-1,-2));
        TextView historyTitle=new TextView(this);historyTitle.setText("HISTORICO");historyTitle.setTextSize(12);historyTitle.setTextColor(0xff67417d);historyTitle.setPadding(dp(4),dp(14),0,dp(5));root.addView(historyTitle);
        history = new TextView(this); history.setTextSize(13); history.setTextColor(Color.DKGRAY); history.setPadding(dp(12),dp(10),dp(12),dp(10)); history.setBackground(round(Color.WHITE,10)); history.setText(getSharedPreferences("dig_games",MODE_PRIVATE).getString("calc_history","Historico vazio")); root.addView(history);
        Button clearHistory=new Button(this);clearHistory.setText("Apagar historico");clearHistory.setAllCaps(false);clearHistory.setTextColor(Color.WHITE);clearHistory.setBackground(round(0xffd84f68,10));clearHistory.setElevation(dp(5));clearHistory.setOnClickListener(v->confirmClearHistory());LinearLayout.LayoutParams clearLp=new LinearLayout.LayoutParams(-1,dp(52));clearLp.setMargins(0,dp(8),0,0);root.addView(clearHistory,clearLp);
        scroll.addView(root,new ScrollView.LayoutParams(-1,-2));
        scroll.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom()+dp(10));return insets;});
        setContentView(scroll);scroll.requestApplyInsets();
    }

    private void press(String key){
        if("C".equals(key)){ expression.setText(""); result.setText("0"); justCalculated=false; return; }
        if("<-".equals(key)){ String s=expression.getText().toString(); if(!s.isEmpty()) expression.setText(s.substring(0,s.length()-1)); return; }
        if("=".equals(key)){ if(justCalculated||expression.getText().toString().trim().isEmpty()){result.setText("0");justCalculated=false;}else calculate(); return; }
        if(justCalculated){result.setText("0");justCalculated=false;}
        expression.append("pi".equals(key)?String.valueOf(Math.PI):key);
    }
    private void calculate(){
        try { String source=expression.getText().toString(); double value=new ExpressionBuilder(source).build().evaluate(); String answer=Math.rint(value)==value?String.valueOf((long)value):String.format(java.util.Locale.ROOT,"%.8f",value).replaceAll("0+$",""); result.setText(answer); expression.setText(""); justCalculated=true; String old=getSharedPreferences("dig_games",MODE_PRIVATE).getString("calc_history",""); String next=source+" = "+answer+"\n"+old; if(next.length()>800)next=next.substring(0,800); getSharedPreferences("dig_games",MODE_PRIVATE).edit().putString("calc_history",next).apply(); history.setText(next); }
        catch(Exception e){ result.setText("Calculo invalido"); }
    }
    private int buttonColor(String key){if(key.matches("[0-9.]"))return 0xff62cce0;if("+-*/".contains(key)&&key.length()==1)return 0xffffa45b;if("=".equals(key))return 0xff45b978;if("C".equals(key)||"<-".equals(key))return 0xffff7184;if("(".equals(key)||")".equals(key))return 0xffffd166;return 0xff9b8bea;}
    private int buttonTextColor(String key){return "=".equals(key)||"C".equals(key)||"<-".equals(key)||key.startsWith("sin")||key.startsWith("cos")||key.startsWith("sqrt")||"pi".equals(key)?Color.WHITE:0xff24313a;}
    private void confirmClearHistory(){new android.app.AlertDialog.Builder(this).setTitle("Apagar historico?").setMessage("Todos os calculos salvos nesta tela serao removidos.").setNegativeButton("Cancelar",null).setPositiveButton("Apagar",(d,w)->{getSharedPreferences("dig_games",MODE_PRIVATE).edit().remove("calc_history").apply();history.setText("Historico vazio");}).show();}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
}

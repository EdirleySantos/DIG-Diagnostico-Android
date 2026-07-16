package br.com.diginteligente.diagnostico;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import net.objecthunter.exp4j.ExpressionBuilder;

public class CalculatorActivity extends Activity {
    private EditText expression;
    private TextView result;
    private TextView history;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16), dp(20), dp(16), dp(14)); root.setBackgroundColor(Color.rgb(255,246,232));
        TextView title = new TextView(this); title.setText("Calculadora inteligente"); title.setTextSize(27); title.setTextColor(Color.rgb(94,49,126)); title.setPadding(dp(4),0,0,dp(12)); root.addView(title);
        expression = new EditText(this); expression.setTextSize(24); expression.setHint("Digite um calculo"); expression.setInputType(InputType.TYPE_CLASS_TEXT); expression.setBackground(round(Color.WHITE,12)); expression.setPadding(dp(14),dp(10),dp(14),dp(10)); root.addView(expression);
        result = new TextView(this); result.setText("0"); result.setTextSize(38); result.setGravity(Gravity.END); result.setTextColor(Color.rgb(220,70,91)); result.setBackground(round(Color.rgb(255,226,232),12)); result.setPadding(dp(12),dp(12),dp(12),dp(12)); LinearLayout.LayoutParams resultLp=new LinearLayout.LayoutParams(-1,-2); resultLp.setMargins(0,dp(10),0,dp(10)); root.addView(result,resultLp);
        GridLayout grid = new GridLayout(this); grid.setColumnCount(4);
        String[] keys={"7","8","9","/","4","5","6","*","1","2","3","-","0",".","(","+","sin(","cos(","sqrt(",")","C","<-","pi","="};
        int[] colors={0xfff7c948,0xff55c2da,0xffff7b89,0xff8f7ee7}; int index=0;
        for(String key:keys){ Button b=new Button(this); b.setText(key); b.setTextSize(17); b.setTextColor(Color.rgb(35,38,48)); b.setBackground(round(colors[index++%colors.length],10)); b.setOnClickListener(v->press(key)); GridLayout.LayoutParams lp=new GridLayout.LayoutParams(); lp.width=0;lp.height=dp(56);lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(dp(2),dp(2),dp(2),dp(2));grid.addView(b,lp); }
        root.addView(grid,new LinearLayout.LayoutParams(-1,-2));
        history = new TextView(this); history.setTextSize(13); history.setTextColor(Color.DKGRAY); history.setPadding(0,dp(12),0,0); history.setText(getSharedPreferences("dig_games",MODE_PRIVATE).getString("calc_history","Historico vazio")); root.addView(history);
        setContentView(root);
    }

    private void press(String key){
        if("C".equals(key)){ expression.setText(""); result.setText("0"); return; }
        if("<-".equals(key)){ String s=expression.getText().toString(); if(!s.isEmpty()) expression.setText(s.substring(0,s.length()-1)); return; }
        if("=".equals(key)){ calculate(); return; }
        expression.append("pi".equals(key)?String.valueOf(Math.PI):key);
    }
    private void calculate(){
        try { String source=expression.getText().toString(); double value=new ExpressionBuilder(source).build().evaluate(); String answer=Math.rint(value)==value?String.valueOf((long)value):String.format(java.util.Locale.ROOT,"%.8f",value).replaceAll("0+$",""); result.setText(answer); String old=getSharedPreferences("dig_games",MODE_PRIVATE).getString("calc_history",""); String next=source+" = "+answer+"\n"+old; if(next.length()>800)next=next.substring(0,800); getSharedPreferences("dig_games",MODE_PRIVATE).edit().putString("calc_history",next).apply(); history.setText(next); }
        catch(Exception e){ result.setText("Calculo invalido"); }
    }
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
}

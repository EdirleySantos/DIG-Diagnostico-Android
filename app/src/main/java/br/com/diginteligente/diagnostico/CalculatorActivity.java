package br.com.diginteligente.diagnostico;

import android.app.Activity;
import android.graphics.Color;
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
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(14), dp(18), dp(14), dp(14)); root.setBackgroundColor(Color.rgb(247,248,250));
        TextView title = new TextView(this); title.setText("Calculadora inteligente"); title.setTextSize(24); title.setTextColor(Color.rgb(25,51,61)); title.setPadding(0,0,0,dp(12)); root.addView(title);
        expression = new EditText(this); expression.setTextSize(24); expression.setHint("Digite um calculo"); expression.setInputType(InputType.TYPE_CLASS_TEXT); root.addView(expression);
        result = new TextView(this); result.setText("0"); result.setTextSize(34); result.setGravity(Gravity.END); result.setTextColor(Color.rgb(8,126,109)); result.setPadding(0,dp(10),0,dp(10)); root.addView(result);
        GridLayout grid = new GridLayout(this); grid.setColumnCount(4);
        String[] keys={"7","8","9","/","4","5","6","*","1","2","3","-","0",".","(","+","sin(","cos(","sqrt(",")","C","<-","pi","="};
        for(String key:keys){ Button b=new Button(this); b.setText(key); b.setOnClickListener(v->press(key)); grid.addView(b,new GridLayout.LayoutParams(){ { width=0; height=dp(54); columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f); } }); }
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
}

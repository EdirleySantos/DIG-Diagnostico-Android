package br.com.diginteligente.diagnostico;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class GameHubActivity extends Activity {
    private LinearLayout content;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        build();
    }

    @Override protected void onResume() {
        super.onResume();
        if (content != null) render();
    }

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(246, 248, 250));
        TextView header = text("Jogos e Calculadora", 26, Color.WHITE, true);
        header.setPadding(dp(20), dp(24), dp(20), dp(20));
        header.setBackgroundColor(Color.rgb(29, 48, 67));
        root.addView(header);
        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(28));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        render();
    }

    private void render() {
        content.removeAllViews();
        int points = getSharedPreferences("dig_games", MODE_PRIVATE).getInt("total_points", 0);
        LinearLayout rank = panel(Color.rgb(229, 241, 255));
        rank.addView(text("Classificacao geral", 14, Color.rgb(46, 76, 110), false));
        rank.addView(text(points + " pontos", 30, Color.rgb(22, 73, 126), true));
        content.addView(rank, space());
        addGame("Calculadora inteligente", "Cientifica, historico e explicacao", CalculatorActivity.class, Color.rgb(8, 126, 109));
        addGame("Jogo da velha", "Dois jogadores ou maquina adaptativa", TicTacToeActivity.class, Color.rgb(198, 72, 73));
        addGame("Paciencia 3D", "Quatro montes, animacoes e pontuacao", SolitaireActivity.class, Color.rgb(180, 70, 99));
        addGame("Domino 3D", "Duplo-seis completo contra a maquina", DominoActivity.class, Color.rgb(32, 112, 86));
        addGame("Xadrez", "Regras completas e adversario automatico", ChessActivity.class, Color.rgb(75, 82, 112));
    }

    private void addGame(String title, String detail, Class<?> target, int color) {
        LinearLayout card = panel(Color.WHITE);
        card.addView(text(title, 18, Color.rgb(29, 45, 52), true));
        card.addView(text(detail, 13, Color.rgb(83, 94, 99), false));
        Button open = new Button(this);
        open.setText("Abrir");
        open.setAllCaps(false);
        open.setTextColor(Color.WHITE);
        open.setBackground(round(color));
        open.setOnClickListener(v -> startActivity(new Intent(this, target)));
        card.addView(open);
        content.addView(card, space());
    }

    private void addLocked(String title, String detail) {
        LinearLayout card = panel(Color.rgb(242, 244, 246));
        card.addView(text(title, 17, Color.rgb(85, 94, 98), true));
        card.addView(text(detail + " | proxima etapa", 13, Color.rgb(115, 122, 125), false));
        content.addView(card, space());
    }

    private LinearLayout panel(int color) {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setPadding(dp(16), dp(14), dp(16), dp(14));
        p.setBackground(round(color));
        return p;
    }

    private TextView text(String s, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(0, dp(3), 0, dp(5)); return t;
    }
    private GradientDrawable round(int color) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(8)); return d; }
    private LinearLayout.LayoutParams space() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.setMargins(0, 0, 0, dp(10)); return p; }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + .5f); }
}

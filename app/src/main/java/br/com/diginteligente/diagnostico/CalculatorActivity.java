package br.com.diginteligente.diagnostico;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import net.objecthunter.exp4j.ExpressionBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class CalculatorActivity extends Activity {
    private static final int PURPLE = 0xff5e317e;
    private static final int TEAL = 0xff087e6d;
    private static final String PREFS = "dig_calculator";

    private EditText expression;
    private TextView result;
    private LinearLayout toolPanel;
    private LinearLayout historyList;
    private ScrollView scroll;
    private boolean justCalculated;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        migrateAndCleanOldGameData();

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setBackgroundColor(0xfffff6e8);
        LinearLayout root = column();
        root.setPadding(dp(16), dp(12), dp(16), dp(28));

        TextView title = label("Calculadora inteligente", 27, PURPLE);
        title.setPadding(dp(4), 0, 0, dp(12));
        root.addView(title);

        expression = input("Digite um calculo");
        expression.setTextSize(24);
        root.addView(expression);

        result = label("0", 38, 0xffdc465b);
        result.setGravity(Gravity.END);
        result.setBackground(round(0xffffe2e8, 12));
        result.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams resultLp = matchWrap();
        resultLp.setMargins(0, dp(10), 0, dp(10));
        root.addView(result, resultLp);

        addKeypad(root);
        addToolbox(root);
        addHistory(root);

        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(0, insets.getSystemWindowInsetTop(), 0,
                    insets.getSystemWindowInsetBottom() + dp(10));
            return insets;
        });
        setContentView(scroll);
        scroll.requestApplyInsets();
    }

    private void addKeypad(LinearLayout root) {
        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        String[] keys = {"7","8","9","/","4","5","6","*","1","2","3","-",
                "0",".","(","+","sin(","cos(","sqrt(",")","C","<-","pi","="};
        for (String key : keys) {
            Button button = raisedButton(key, buttonColor(key));
            button.setTextColor(buttonTextColor(key));
            button.setOnClickListener(v -> press(key));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = dp(60);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(dp(4), dp(4), dp(4), dp(5));
            grid.addView(button, lp);
        }
        root.addView(grid, matchWrap());
    }

    private void addToolbox(LinearLayout root) {
        TextView toolsTitle = sectionTitle("FERRAMENTAS DE CALCULO");
        root.addView(toolsTitle);
        Spinner selector = spinner(new String[]{"Juros e parcelas", "Litros para quilos",
                "Dolar e real", "Conversor de medidas"});
        root.addView(selector, matchHeight(54));
        toolPanel = column();
        LinearLayout.LayoutParams panelLp = matchWrap();
        panelLp.setMargins(0, dp(8), 0, 0);
        root.addView(toolPanel, panelLp);
        selector.setOnItemSelectedListener(new SimpleItemSelectedListener(position -> showTool(position)));
        showTool(0);
    }

    private void showTool(int position) {
        toolPanel.removeAllViews();
        if (position == 0) showInterestTool();
        else if (position == 1) showDensityTool();
        else if (position == 2) showCurrencyTool();
        else showMeasuresTool();
    }

    private void showInterestTool() {
        EditText principal = input("Valor inicial (R$)");
        EditText rate = input("Juros por periodo (%)");
        EditText periods = input("Quantidade de periodos/parcelas");
        Spinner type = spinner(new String[]{"Juros compostos", "Juros simples"});
        toolPanel.addView(type, matchHeight(52));
        addField(principal); addField(rate); addField(periods);
        Button calculate = actionButton("Calcular parcelas", 0xff45b978);
        toolPanel.addView(calculate, matchHeight(54));
        calculate.setOnClickListener(v -> {
            try {
                double p = number(principal);
                double i = number(rate) / 100d;
                int n = (int) number(periods);
                if (p < 0 || i < 0 || n < 1) throw new IllegalArgumentException();
                boolean compound = type.getSelectedItemPosition() == 0;
                double total = compound ? p * Math.pow(1 + i, n) : p * (1 + i * n);
                double interest = total - p;
                String name = compound ? "Juros compostos" : "Juros simples";
                String details = name + "\nTotal: " + money(total) + "\nJuros: " + money(interest)
                        + "\n" + n + " parcelas de " + money(total / n);
                showToolResult(details, total, name + ": " + money(p) + " -> " + money(total));
            } catch (Exception e) { invalidFields(); }
        });
    }

    private void showDensityTool() {
        EditText liters = input("Quantidade em litros");
        Spinner material = spinner(new String[]{"Agua (1,000 kg/L)", "Leite (1,030 kg/L)",
                "Oleo de cozinha (0,920 kg/L)", "Alcool (0,789 kg/L)", "Densidade personalizada"});
        EditText density = input("Densidade em kg/L");
        density.setText("1,000");
        toolPanel.addView(material, matchHeight(52)); addField(liters); addField(density);
        double[] densities = {1d, 1.03d, 0.92d, 0.789d, 1d};
        material.setOnItemSelectedListener(new SimpleItemSelectedListener(position -> {
            if (position < 4) density.setText(decimal(densities[position]));
            density.setEnabled(position == 4);
        }));
        Button calculate = actionButton("Converter para quilos", 0xff45b978);
        toolPanel.addView(calculate, matchHeight(54));
        calculate.setOnClickListener(v -> {
            try {
                double l = number(liters), d = number(density);
                if (l < 0 || d <= 0) throw new IllegalArgumentException();
                double kg = l * d;
                String name = material.getSelectedItem().toString().split(" \\(")[0];
                showToolResult(decimal(l) + " L de " + name + " = " + decimal(kg) + " kg",
                        kg, decimal(l) + " L para kg = " + decimal(kg));
            } catch (Exception e) { invalidFields(); }
        });
    }

    private void showCurrencyTool() {
        Spinner direction = spinner(new String[]{"Dolar (USD) para real (BRL)", "Real (BRL) para dolar (USD)"});
        EditText amount = input("Valor para converter");
        EditText quote = input("Cotacao de 1 dolar em reais");
        quote.setText(getPreferences().getString("usd_brl", "5,00"));
        TextView quoteStatus = label("Cotacao editavel", 13, 0xff52636a);
        toolPanel.addView(direction, matchHeight(52)); addField(amount); addField(quote);
        toolPanel.addView(quoteStatus);

        LinearLayout actions = row();
        Button update = actionButton("Atualizar PTAX", 0xff62a8e5);
        Button convert = actionButton("Converter", 0xff45b978);
        actions.addView(update, weightedHeight(54));
        actions.addView(convert, weightedHeight(54));
        toolPanel.addView(actions, matchWrap());

        update.setOnClickListener(v -> fetchDollarQuote(update, quote, quoteStatus));
        convert.setOnClickListener(v -> {
            try {
                double value = number(amount), usd = number(quote);
                if (value < 0 || usd <= 0) throw new IllegalArgumentException();
                boolean toReal = direction.getSelectedItemPosition() == 0;
                double converted = toReal ? value * usd : value / usd;
                String text = toReal ? money(value, "US$") + " = " + money(converted)
                        : money(value) + " = " + money(converted, "US$");
                getPreferences().edit().putString("usd_brl", decimal(usd)).apply();
                showToolResult(text + "\nCotacao usada: R$ " + decimal(usd), converted, text);
            } catch (Exception e) { invalidFields(); }
        });
    }

    private void fetchDollarQuote(Button button, EditText quote, TextView status) {
        button.setEnabled(false);
        status.setText("Buscando cotacao oficial...");
        new Thread(() -> {
            try {
                Calendar end = Calendar.getInstance();
                Calendar start = (Calendar) end.clone();
                start.add(Calendar.DAY_OF_MONTH, -10);
                SimpleDateFormat date = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
                String endpoint = "https://olinda.bcb.gov.br/olinda/servico/PTAX/versao/v1/odata/"
                        + "CotacaoDolarPeriodo(dataInicial=@dataInicial,dataFinalCotacao=@dataFinalCotacao)"
                        + "?@dataInicial='" + date.format(start.getTime()) + "'&@dataFinalCotacao='"
                        + date.format(end.getTime()) + "'&$orderby=dataHoraCotacao%20desc&$top=1&$format=json";
                HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setConnectTimeout(8000); connection.setReadTimeout(8000);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder json = new StringBuilder(); String line;
                    while ((line = reader.readLine()) != null) json.append(line);
                    JSONArray values = new JSONObject(json.toString()).getJSONArray("value");
                    if (values.length() == 0) throw new IllegalStateException();
                    JSONObject latest = values.getJSONObject(0);
                    double value = latest.getDouble("cotacaoVenda");
                    String moment = latest.optString("dataHoraCotacao", "").replace("T", " ");
                    runOnUiThread(() -> {
                        quote.setText(decimal(value));
                        status.setText("PTAX venda: " + moment);
                        button.setEnabled(true);
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    status.setText("Sem cotacao online. Use o valor manual.");
                    button.setEnabled(true);
                });
            }
        }).start();
    }

    private void showMeasuresTool() {
        Spinner category = spinner(new String[]{"Comprimento", "Peso", "Volume"});
        Spinner from = spinner(new String[]{"Milimetro", "Centimetro", "Metro", "Quilometro"});
        Spinner to = spinner(new String[]{"Milimetro", "Centimetro", "Metro", "Quilometro"});
        to.setSelection(2);
        EditText value = input("Valor para converter");
        toolPanel.addView(category, matchHeight(52)); addField(value);
        LinearLayout units = row();
        units.addView(from, weightedHeight(54)); units.addView(to, weightedHeight(54));
        toolPanel.addView(units, matchWrap());
        category.setOnItemSelectedListener(new SimpleItemSelectedListener(position -> {
            String[] names = measureNames(position);
            setSpinnerItems(from, names); setSpinnerItems(to, names); to.setSelection(Math.min(2, names.length - 1));
        }));
        Button convert = actionButton("Converter medida", 0xff45b978);
        toolPanel.addView(convert, matchHeight(54));
        convert.setOnClickListener(v -> {
            try {
                double entered = number(value);
                int c = category.getSelectedItemPosition();
                double[] factors = measureFactors(c);
                double converted = entered * factors[from.getSelectedItemPosition()] / factors[to.getSelectedItemPosition()];
                String text = decimal(entered) + " " + from.getSelectedItem() + " = "
                        + decimal(converted) + " " + to.getSelectedItem();
                showToolResult(text, converted, text);
            } catch (Exception e) { invalidFields(); }
        });
    }

    private String[] measureNames(int category) {
        if (category == 1) return new String[]{"Grama", "Quilograma", "Tonelada"};
        if (category == 2) return new String[]{"Mililitro", "Litro", "Metro cubico"};
        return new String[]{"Milimetro", "Centimetro", "Metro", "Quilometro"};
    }

    private double[] measureFactors(int category) {
        if (category == 1) return new double[]{0.001, 1, 1000};
        if (category == 2) return new double[]{0.001, 1, 1000};
        return new double[]{0.001, 0.01, 1, 1000};
    }

    private void showToolResult(String message, double rawValue, String historyText) {
        String raw = decimal(rawValue);
        result.setText(raw);
        expression.setText("");
        justCalculated = true;
        addHistory(historyText, raw);
        new AlertDialog.Builder(this).setTitle("Resultado").setMessage(message)
                .setPositiveButton("Usar na calculadora", (d, w) -> useHistoryValue(raw))
                .setNegativeButton("Fechar", null).show();
    }

    private void addHistory(LinearLayout root) {
        root.addView(sectionTitle("HISTORICO - TOQUE PARA REUTILIZAR"));
        historyList = column();
        root.addView(historyList, matchWrap());
        renderHistory();
        Button clear = actionButton("Apagar historico", 0xffd84f68);
        clear.setOnClickListener(v -> confirmClearHistory());
        LinearLayout.LayoutParams lp = matchHeight(52); lp.setMargins(0, dp(8), 0, 0);
        root.addView(clear, lp);
    }

    private void addHistory(String display, String rawValue) {
        String old = getPreferences().getString("calc_history", "");
        String next = display.replace("\n", " ") + "\t" + rawValue + "\n" + old;
        String[] lines = next.split("\n");
        StringBuilder limited = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, 30); i++) limited.append(lines[i]).append('\n');
        getPreferences().edit().putString("calc_history", limited.toString()).apply();
        renderHistory();
    }

    private void renderHistory() {
        historyList.removeAllViews();
        String saved = getPreferences().getString("calc_history", "").trim();
        if (saved.isEmpty()) {
            TextView empty = label("Historico vazio", 14, 0xff68767b);
            empty.setPadding(dp(12), dp(14), dp(12), dp(14));
            empty.setBackground(round(Color.WHITE, 9)); historyList.addView(empty); return;
        }
        for (String record : saved.split("\n")) {
            if (record.trim().isEmpty()) continue;
            String[] parts = record.split("\\t", 2);
            String display = parts[0];
            String raw = parts.length == 2 ? parts[1] : valueFromLegacyHistory(display);
            Button item = actionButton(display, Color.WHITE);
            item.setTextColor(0xff344249); item.setTextSize(14); item.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            item.setOnClickListener(v -> useHistoryValue(raw));
            LinearLayout.LayoutParams lp = matchHeight(50); lp.setMargins(0, 0, 0, dp(5));
            historyList.addView(item, lp);
        }
    }

    private String valueFromLegacyHistory(String line) {
        int equals = line.lastIndexOf(" = ");
        if (equals < 0) return "";
        String value = line.substring(equals + 3).trim().replace(",", ".");
        try { Double.parseDouble(value); return value; } catch (Exception e) { return ""; }
    }

    private void useHistoryValue(String raw) {
        if (raw == null || raw.trim().isEmpty()) return;
        result.setText(raw);
        expression.setText("");
        justCalculated = true;
        scroll.smoothScrollTo(0, 0);
    }

    private void press(String key) {
        if ("C".equals(key)) { expression.setText(""); result.setText("0"); justCalculated = false; return; }
        if ("<-".equals(key)) { String s = expression.getText().toString(); if (!s.isEmpty()) expression.setText(s.substring(0, s.length() - 1)); return; }
        if ("=".equals(key)) { if (expression.getText().toString().trim().isEmpty()) return; calculate(); return; }
        if (justCalculated) {
            if (isOperator(key)) expression.setText(result.getText().toString()); else result.setText("0");
            justCalculated = false;
        }
        expression.append("pi".equals(key) ? String.valueOf(Math.PI) : key);
    }

    private void calculate() {
        try {
            String source = expression.getText().toString();
            double value = new ExpressionBuilder(source).build().evaluate();
            String answer = decimal(value);
            result.setText(answer); expression.setText(""); justCalculated = true;
            addHistory(source + " = " + answer, answer);
        } catch (Exception e) { result.setText("Calculo invalido"); }
    }

    private boolean isOperator(String key) { return key.length() == 1 && "+-*/".contains(key); }
    private double number(EditText field) { return Double.parseDouble(field.getText().toString().trim().replace(" ", "").replace(",", ".")); }
    private String decimal(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException();
        if (Math.rint(value) == value) return String.valueOf((long) value);
        return String.format(Locale.ROOT, "%.8f", value).replaceAll("0+$", "");
    }
    private String money(double value) { return money(value, "R$"); }
    private String money(double value, String symbol) { return symbol + " " + String.format(new Locale("pt", "BR"), "%,.2f", value); }
    private void invalidFields() { new AlertDialog.Builder(this).setTitle("Confira os valores").setMessage("Preencha todos os campos com numeros validos.").setPositiveButton("Entendido", null).show(); }

    private void confirmClearHistory() {
        new AlertDialog.Builder(this).setTitle("Apagar historico?")
                .setMessage("Todos os calculos salvos nesta tela serao removidos.")
                .setNegativeButton("Cancelar", null).setPositiveButton("Apagar", (d, w) -> {
                    getPreferences().edit().remove("calc_history").apply(); renderHistory();
                }).show();
    }

    private android.content.SharedPreferences getPreferences() { return getSharedPreferences(PREFS, MODE_PRIVATE); }
    private void migrateAndCleanOldGameData() {
        android.content.SharedPreferences old = getSharedPreferences("dig_games", MODE_PRIVATE);
        android.content.SharedPreferences calc = getPreferences();
        if (!calc.contains("calc_history") && old.contains("calc_history")) calc.edit().putString("calc_history", old.getString("calc_history", "")).apply();
        old.edit().clear().apply();
    }

    private void addField(EditText field) { LinearLayout.LayoutParams lp = matchHeight(56); lp.setMargins(0, dp(6), 0, 0); toolPanel.addView(field, lp); }
    private EditText input(String hint) {
        EditText field = new EditText(this); field.setHint(hint); field.setTextSize(17);
        field.setSingleLine(true); field.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        field.setBackground(round(Color.WHITE, 10)); field.setPadding(dp(14), dp(8), dp(14), dp(8)); return field;
    }
    private Spinner spinner(String[] items) { Spinner spinner = new Spinner(this); spinner.setBackground(round(Color.WHITE, 10)); spinner.setPadding(dp(10), 0, dp(8), 0); setSpinnerItems(spinner, items); return spinner; }
    private void setSpinnerItems(Spinner spinner, String[] items) { ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items); spinner.setAdapter(adapter); }
    private TextView sectionTitle(String text) { TextView title = label(text, 12, 0xff67417d); title.setPadding(dp(4), dp(16), 0, dp(6)); return title; }
    private TextView label(String text, int size, int color) { TextView view = new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(color); return view; }
    private Button actionButton(String text, int color) { Button b = raisedButton(text, color); b.setAllCaps(false); return b; }
    private Button raisedButton(String text, int color) {
        Button button = new Button(this); button.setText(text); button.setTextSize(16); button.setTypeface(null, android.graphics.Typeface.BOLD);
        button.setTextColor(Color.WHITE); button.setBackground(round(color, 10)); button.setElevation(dp(7)); button.setTranslationZ(dp(3)); button.setStateListAnimator(null);
        button.setOnTouchListener((v, event) -> { if (event.getAction() == MotionEvent.ACTION_DOWN) v.animate().translationY(dp(5)).translationZ(0).setDuration(70).start(); else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) v.animate().translationY(0).translationZ(dp(3)).setDuration(120).start(); return false; });
        return button;
    }
    private int buttonColor(String key) { if (key.matches("[0-9.]")) return 0xff62cce0; if ("+-*/".contains(key) && key.length() == 1) return 0xffffa45b; if ("=".equals(key)) return 0xff45b978; if ("C".equals(key) || "<-".equals(key)) return 0xffff7184; if ("(".equals(key) || ")".equals(key)) return 0xffffd166; return 0xff9b8bea; }
    private int buttonTextColor(String key) { return "=".equals(key) || "C".equals(key) || "<-".equals(key) || key.startsWith("sin") || key.startsWith("cos") || key.startsWith("sqrt") || "pi".equals(key) ? Color.WHITE : 0xff24313a; }
    private LinearLayout column() { LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setBackgroundColor(0xfffff6e8); return layout; }
    private LinearLayout row() { LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.HORIZONTAL); return layout; }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private LinearLayout.LayoutParams matchHeight(int height) { return new LinearLayout.LayoutParams(-1, dp(height)); }
    private LinearLayout.LayoutParams weightedHeight(int height) { LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(height), 1); lp.setMargins(dp(3), dp(5), dp(3), 0); return lp; }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable round(int color, int radius) { GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius)); return drawable; }

    private static class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        interface Selection { void selected(int position); }
        private final Selection selection;
        SimpleItemSelectedListener(Selection selection) { this.selection = selection; }
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { selection.selected(position); }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
    }
}

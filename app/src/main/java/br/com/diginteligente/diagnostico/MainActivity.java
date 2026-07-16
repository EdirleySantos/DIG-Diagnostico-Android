package br.com.diginteligente.diagnostico;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.DownloadManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.Build;
import android.provider.Settings;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int PICK_FOLDER = 1201;
    private static final long DAY = 24L * 60L * 60L * 1000L;
    private static final long LARGE_FILE = 50L * 1024L * 1024L;
    private static final String RELEASE_API = "https://api.github.com/repos/EdirleySantos/DIG-Diagnostico-Android/releases/latest";
    private static final String PREFS = "dig_local_intelligence";
    private static final String HISTORY_KEY = "health_history";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private LinearLayout content;
    private TextView status;
    private ProgressBar progress;
    private long updateDownloadId = -1;
    private BroadcastReceiver downloadReceiver;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            migrateCalculatorAndCleanGames();
            buildScreen();
            registerDownloadReceiver();
            recordDailySnapshot();
            showOverview();
        } catch (Exception error) {
            showStartupRecovery(error);
        }
    }

    private void migrateCalculatorAndCleanGames() {
        android.content.SharedPreferences old = getSharedPreferences("dig_games", MODE_PRIVATE);
        android.content.SharedPreferences calculator = getSharedPreferences("dig_calculator", MODE_PRIVATE);
        if (!calculator.contains("calc_history") && old.contains("calc_history")) {
            calculator.edit().putString("calc_history", old.getString("calc_history", "")).apply();
        }
        old.edit().clear().apply();
    }

    private void showStartupRecovery(Exception error) {
        LinearLayout recovery = new LinearLayout(this);
        recovery.setOrientation(LinearLayout.VERTICAL);
        recovery.setPadding(dp(24), dp(32), dp(24), dp(24));
        recovery.setBackgroundColor(Color.rgb(246, 248, 250));
        TextView title = text("DIG Diagnostico", 26, Color.rgb(18, 59, 66), true);
        recovery.addView(title);
        TextView message = text("A inicializacao encontrou um problema local. Seus arquivos e aplicativos nao foram alterados.", 16, Color.DKGRAY, false);
        message.setPadding(0, dp(18), 0, dp(18));
        recovery.addView(message);
        Button retry = new Button(this);
        retry.setText("Tentar novamente");
        retry.setOnClickListener(v -> recreate());
        recovery.addView(retry);
        Button resetLearning = new Button(this);
        resetLearning.setText("Limpar somente historico do DIG");
        resetLearning.setOnClickListener(v -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply();
            recreate();
        });
        recovery.addView(resetLearning);
        setContentView(recovery);
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(246, 248, 250));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(22), dp(24), dp(22), dp(20));
        header.setBackground(rounded(Color.rgb(20, 48, 58), 0, 0));
        TextView title = text("DIG Diagnostico", 26, Color.WHITE, true);
        TextView subtitle = text("Protecao e desempenho do seu aparelho", 14, Color.rgb(183, 221, 214), false);
        header.addView(title);
        header.addView(subtitle);
        root.addView(header);

        LinearLayout nav = new LinearLayout(this);
        nav.setPadding(dp(10), dp(10), dp(10), dp(8));
        nav.setBackgroundColor(Color.WHITE);
        nav.setGravity(Gravity.CENTER);
        nav.addView(navButton("Resumo", v -> showOverview()));
        nav.addView(navButton("Apps", v -> analyzeApps()));
        nav.addView(navButton("Limpar", v -> showCleaner()));
        nav.addView(navButton("Seguranca", v -> analyzeSecurity()));
        root.addView(nav);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(4)));

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(8), dp(16), dp(28));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        status = text("Protecao ativa: nenhuma exclusao automatica.", 12, Color.rgb(75, 88, 92), false);
        status.setPadding(dp(16), dp(10), dp(16), dp(10));
        root.addView(status);
        setContentView(root);
    }

    private void showOverview() {
        clear("Saude do aparelho");
        StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
        long total = fs.getTotalBytes();
        long free = fs.getAvailableBytes();
        long used = total - free;
        int installed = getPackageManager().getInstalledApplications(0).size();
        long ownCache = folderSize(getCacheDir());
        int freePercent = total == 0 ? 0 : (int) ((free * 100L) / total);
        int health = Math.max(35, Math.min(100, 70 + Math.min(25, freePercent) - (ownCache > 100L * 1024L * 1024L ? 5 : 0)));
        addHealthCard(health, freePercent);
        addMetricRow("Armazenamento", format(used) + " usados", "Livre", format(free));
        addMetricRow("Aplicativos", String.valueOf(installed), "Versao", BuildConfig.VERSION_NAME);

        TextView next = text("ACOES RECOMENDADAS", 12, Color.rgb(83, 96, 101), true);
        next.setPadding(dp(2), dp(14), 0, dp(8));
        content.addView(next);
        if (freePercent < 15) addWarning("Pouco espaco livre. Analise Downloads, videos e arquivos grandes primeiro.");
        else addInfo("O armazenamento tem uma margem livre adequada. Uma analise de arquivos pode encontrar itens antigos.");
        addAction("Analisar aplicativos agora", v -> analyzeApps());
        addAction("Analisar pasta, SD ou USB", v -> chooseFolder());
        addAction("Verificar seguranca", v -> analyzeSecurity());
        addAction("Abrir calculadora", v -> startActivity(new Intent(this, CalculatorActivity.class)));
        addSecondaryAction("Gerenciar armazenamento", v -> startActivity(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)));
        addSecondaryAction("Ver historico e tendencias", v -> showHistory());
        addSecondaryAction("Verificar atualizacoes", v -> checkForUpdates(true));
        addLearnedRecommendations(freePercent);
        checkForUpdates(false);
    }

    private void recordDailySnapshot() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        JSONArray history;
        try { history = new JSONArray(prefs.getString(HISTORY_KEY, "[]")); }
        catch (Exception ignored) { history = new JSONArray(); }
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(new Date());
        try {
            if (history.length() > 0 && today.equals(history.getJSONObject(history.length() - 1).optString("date"))) return;
            StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
            ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
            ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getMemoryInfo(memory);
            JSONObject item = new JSONObject();
            item.put("date", today);
            item.put("freeStorage", fs.getAvailableBytes());
            item.put("totalStorage", fs.getTotalBytes());
            item.put("freeMemory", memory.availMem);
            item.put("totalMemory", memory.totalMem);
            item.put("apps", getPackageManager().getInstalledApplications(0).size());
            item.put("digCache", folderSize(getCacheDir()) + folderSize(getCodeCacheDir()) + folderSize(getExternalCacheDir()));
            history.put(item);
            JSONArray trimmed = new JSONArray();
            int start = Math.max(0, history.length() - 60);
            for (int i = start; i < history.length(); i++) trimmed.put(history.getJSONObject(i));
            prefs.edit().putString(HISTORY_KEY, trimmed.toString()).apply();
        } catch (Exception ignored) { }
    }

    private JSONArray loadHistory() {
        try { return new JSONArray(getSharedPreferences(PREFS, MODE_PRIVATE).getString(HISTORY_KEY, "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }

    private void addLearnedRecommendations(int freePercent) {
        JSONArray history = loadHistory();
        if (history.length() < 2) {
            addInfo("Aprendizado local iniciado. A partir dos proximos dias, o DIG mostrara tendencias de armazenamento e memoria.");
            return;
        }
        try {
            JSONObject first = history.getJSONObject(0);
            JSONObject last = history.getJSONObject(history.length() - 1);
            long consumed = first.optLong("freeStorage") - last.optLong("freeStorage");
            int days = Math.max(1, history.length() - 1);
            long daily = consumed / days;
            if (daily > 20L * 1024L * 1024L) {
                long free = last.optLong("freeStorage");
                long remainingDays = daily == 0 ? 0 : free / daily;
                addWarning("Tendencia detectada: o armazenamento esta crescendo cerca de " + format(daily) + " por dia. Mantido esse ritmo, o espaco pode ficar baixo em aproximadamente " + remainingDays + " dias.");
            } else if (consumed < -50L * 1024L * 1024L) {
                addInfo("Tendencia positiva: o aparelho ganhou " + format(-consumed) + " de espaco desde a primeira medicao.");
            } else {
                addInfo("O uso do armazenamento esta estavel nas ultimas " + history.length() + " medicoes.");
            }
            int appChange = last.optInt("apps") - first.optInt("apps");
            if (appChange >= 5) addWarning(appChange + " aplicativos foram adicionados desde o inicio do historico. Revise os que nao estiver usando.");
        } catch (Exception ignored) { }
    }

    private void showHistory() {
        clear("Historico local");
        JSONArray history = loadHistory();
        addInfo("As medicoes ficam somente neste aparelho e nao incluem nomes de arquivos, fotos, contatos ou mensagens.");
        if (history.length() == 0) {
            addInfo("Ainda nao existem medicoes salvas.");
            return;
        }
        for (int i = history.length() - 1; i >= Math.max(0, history.length() - 15); i--) {
            try {
                JSONObject item = history.getJSONObject(i);
                long total = item.optLong("totalStorage");
                long free = item.optLong("freeStorage");
                int percent = total == 0 ? 0 : (int) (free * 100L / total);
                content.addView(card(formatDate(item.optString("date")), percent + "% livre | " + item.optInt("apps") + " apps | RAM livre " + format(item.optLong("freeMemory"))));
            } catch (Exception ignored) { }
        }
        addSecondaryAction("Apagar historico local", v -> new AlertDialog.Builder(this)
                .setTitle("Apagar aprendizado local?")
                .setMessage("Todas as medicoes e tendencias deste aparelho serao removidas.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Apagar", (d, w) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(HISTORY_KEY).apply();
                    showHistory();
                }).show());
    }

    private String formatDate(String iso) {
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).parse(iso);
            return new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR")).format(date);
        } catch (Exception ignored) { return iso; }
    }

    private void showCleaner() {
        clear("Limpeza inteligente");
        long cache = folderSize(getCacheDir()) + folderSize(getCodeCacheDir()) + folderSize(getExternalCacheDir());
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memory = new ActivityManager.MemoryInfo();
        manager.getMemoryInfo(memory);
        long usedRam = memory.totalMem - memory.availMem;

        addMetricRow("Temporarios do DIG", format(cache), "Memoria disponivel", format(memory.availMem));
        addInfo("RAM em uso: " + format(usedRam) + " de " + format(memory.totalMem) + ". O Android libera memoria automaticamente quando outro aplicativo precisa.");
        addAction("Limpar temporarios do DIG", v -> confirmOwnCache());
        addAction("Analisar Downloads, SD ou USB", v -> chooseFolder());
        addSecondaryAction("Gerenciar armazenamento", v -> startActivity(new Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)));
        addSecondaryAction("Revisar aplicativos em execucao", v -> startActivity(new Intent(Settings.ACTION_APPLICATION_SETTINGS)));
        addWarning("Por seguranca, o Android nao permite apagar o cache privado ou encerrar outros aplicativos diretamente. Use Detalhes do app para limpar um aplicativo especifico.");
    }

    private void checkForUpdates(boolean userRequested) {
        busy(true, "Consultando atualizacoes...");
        worker.execute(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(RELEASE_API).openConnection();
                connection.setRequestProperty("Accept", "application/vnd.github+json");
                connection.setRequestProperty("User-Agent", "DIG-Diagnostico-Android");
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                int code = connection.getResponseCode();
                if (code != 200) throw new Exception("GitHub respondeu " + code);
                StringBuilder json = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) json.append(line);
                }
                JSONObject release = new JSONObject(json.toString());
                String tag = release.optString("tag_name", "0").replaceFirst("^[vV]", "");
                String notes = release.optString("body", "Nova versao disponivel.");
                String apkUrl = "";
                JSONArray assets = release.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        if (asset.optString("name").toLowerCase(Locale.ROOT).endsWith(".apk")) {
                            apkUrl = asset.optString("browser_download_url");
                            break;
                        }
                    }
                }
                final String version = tag;
                final String downloadUrl = apkUrl;
                final String releaseNotes = notes.length() > 500 ? notes.substring(0, 500) : notes;
                runOnUiThread(() -> {
                    busy(false, "Verificacao de atualizacao concluida");
                    if (compareVersions(version, BuildConfig.VERSION_NAME) > 0 && !downloadUrl.isEmpty()) {
                        showUpdateDialog(version, releaseNotes, downloadUrl);
                    } else if (userRequested) {
                        new AlertDialog.Builder(this).setTitle("Aplicativo atualizado")
                                .setMessage("Voce ja esta usando a versao mais recente: " + BuildConfig.VERSION_NAME)
                                .setPositiveButton("OK", null).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    busy(false, "Nao foi possivel consultar atualizacoes");
                    if (userRequested) Toast.makeText(this, "Falha ao consultar o GitHub. Verifique a internet.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private int compareVersions(String remote, String local) {
        String[] a = remote.split("\\.");
        String[] b = local.split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int av = i < a.length ? numberPart(a[i]) : 0;
            int bv = i < b.length ? numberPart(b[i]) : 0;
            if (av != bv) return Integer.compare(av, bv);
        }
        return 0;
    }

    private int numberPart(String value) {
        try { return Integer.parseInt(value.replaceAll("[^0-9].*$", "")); }
        catch (Exception ignored) { return 0; }
    }

    private void showUpdateDialog(String version, String notes, String url) {
        new AlertDialog.Builder(this).setTitle("Atualizacao " + version)
                .setMessage(notes + "\n\nO Android pedira sua confirmacao antes de instalar.")
                .setNegativeButton("Depois", null)
                .setPositiveButton("Baixar", (d, w) -> downloadUpdate(version, url))
                .show();
    }

    private void downloadUpdate(String version, String url) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(this).setTitle("Autorizar atualizacoes")
                    .setMessage("Permita que o DIG Diagnostico instale a atualizacao baixada. O Android ainda mostrara a confirmacao final.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Abrir configuracao", (d, w) -> startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                            Uri.parse("package:" + getPackageName())))).show();
            return;
        }
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("DIG Diagnostico " + version);
        request.setDescription("Baixando atualizacao segura do GitHub");
        request.setMimeType("application/vnd.android.package-archive");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "DIG-Diagnostico-Android-" + version + ".apk");
        DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        updateDownloadId = manager.enqueue(request);
        Toast.makeText(this, "Atualizacao sendo baixada", Toast.LENGTH_LONG).show();
    }

    private void registerDownloadReceiver() {
        downloadReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id != updateDownloadId) return;
                DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                Uri apk = manager.getUriForDownloadedFile(id);
                if (apk == null) return;
                Intent install = new Intent(Intent.ACTION_VIEW);
                install.setDataAndType(apk, "application/vnd.android.package-archive");
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(install);
            }
        };
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(downloadReceiver, filter);
    }

    private void analyzeApps() {
        clear("Analise de aplicativos");
        boolean advanced = hasUsageAccess();
        addInfo(advanced ? "Analise avancada ativa: tempo sem uso, instalacao e tamanho do aplicativo."
                : "Analise padrao ativa: tamanho, idade e situacao dos aplicativos. O acesso avancado e opcional.");
        if (!advanced) {
            addSecondaryAction("Ativar analise avancada", v -> showRestrictedAccessHelp());
        }
        busy(true, "Classificando aplicativos...");
        worker.execute(() -> {
            List<AppCandidate> candidates = findAppCandidates(advanced);
            runOnUiThread(() -> {
                busy(false, candidates.size() + " sugestoes encontradas");
                if (candidates.isEmpty()) addInfo("Nenhum aplicativo precisa de revisao neste momento.");
                for (AppCandidate app : candidates) addAppCandidate(app);
            });
        });
    }

    private List<AppCandidate> findAppCandidates(boolean advanced) {
        long now = System.currentTimeMillis();
        long start = now - 365L * DAY;
        Map<String, UsageStats> usage = new HashMap<>();
        if (advanced) {
            UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
            for (UsageStats stat : usm.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, start, now)) {
                UsageStats old = usage.get(stat.getPackageName());
                if (old == null || stat.getLastTimeUsed() > old.getLastTimeUsed()) usage.put(stat.getPackageName(), stat);
            }
        }
        List<AppCandidate> result = new ArrayList<>();
        PackageManager pm = getPackageManager();
        for (ApplicationInfo ai : pm.getInstalledApplications(0)) {
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0 || ai.packageName.equals(getPackageName())) continue;
            long apkSize = new File(ai.sourceDir).length();
            long days = 0;
            String reason;
            int priority = 0;
            if (advanced) {
                UsageStats stat = usage.get(ai.packageName);
                long last = stat == null ? 0 : stat.getLastTimeUsed();
                days = last == 0 ? 9999 : (now - last) / DAY;
                if (days >= 180) { reason = days > 3000 ? "Sem uso registrado" : "Sem uso ha " + days + " dias"; priority += 3; }
                else if (days >= 90) { reason = "Pouco usado: " + days + " dias"; priority += 2; }
                else continue;
            } else {
                try {
                    PackageInfo pi = pm.getPackageInfo(ai.packageName, 0);
                    days = (now - pi.lastUpdateTime) / DAY;
                } catch (Exception ignored) { days = 0; }
                if (apkSize >= 150L * 1024L * 1024L) { reason = "Aplicativo grande para revisar"; priority += 2; }
                else if (days >= 365) { reason = "Sem atualizacao ha " + days + " dias"; priority += 1; }
                else continue;
            }
            result.add(new AppCandidate(ai.loadLabel(pm).toString(), ai.packageName, days, apkSize, reason, priority));
        }
        result.sort(Comparator.comparingInt((AppCandidate a) -> a.priority).reversed().thenComparingLong(a -> a.size).reversed());
        return result;
    }

    private void addAppCandidate(AppCandidate app) {
        LinearLayout card = card(app.name, app.reason + " | " + format(app.size));
        Button details = smallButton("Detalhes");
        details.setOnClickListener(v -> openAppDetails(app.packageName));
        Button uninstall = smallButton("Desinstalar");
        uninstall.setOnClickListener(v -> confirmUninstall(app));
        LinearLayout row = new LinearLayout(this);
        row.addView(details);
        row.addView(uninstall);
        card.addView(row);
        content.addView(card);
    }

    private void chooseFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, PICK_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FOLDER && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) return;
            int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try { getContentResolver().takePersistableUriPermission(uri, flags); } catch (SecurityException ignored) { }
            scanFolder(uri);
        }
    }

    private void scanFolder(Uri uri) {
        clear("Analise de arquivos");
        busy(true, "Analisando a pasta escolhida...");
        worker.execute(() -> {
            List<FileCandidate> files = new ArrayList<>();
            DocumentFile root = DocumentFile.fromTreeUri(this, uri);
            if (root != null) walk(root, files, new int[]{0});
            files.sort(Comparator.comparingLong((FileCandidate f) -> f.size).reversed());
            runOnUiThread(() -> {
                busy(false, files.size() + " arquivos para revisar");
                if (files.isEmpty()) addInfo("Nenhum arquivo grande, antigo ou temporario foi encontrado nesta pasta.");
                long safeBytes = 0;
                int safeCount = 0;
                for (FileCandidate f : files) {
                    if (f.safeToClean) { safeBytes += f.size; safeCount++; }
                }
                if (safeCount > 0) {
                    final long cleanBytes = safeBytes;
                    final int cleanCount = safeCount;
                    addAction("Limpar " + cleanCount + " residuos seguros (" + format(cleanBytes) + ")",
                            v -> confirmSafeCleanup(uri, files, cleanCount, cleanBytes));
                }
                for (FileCandidate f : files) addFileCandidate(f);
            });
        });
    }

    private void walk(DocumentFile dir, List<FileCandidate> output, int[] visited) {
        if (visited[0] >= 6000) return;
        DocumentFile[] children;
        try { children = dir.listFiles(); } catch (Exception e) { return; }
        for (DocumentFile file : children) {
            if (++visited[0] >= 6000) return;
            if (file.isDirectory()) {
                walk(file, output, visited);
            } else {
                String name = file.getName() == null ? "Arquivo sem nome" : file.getName();
                String lower = name.toLowerCase(Locale.ROOT);
                long age = file.lastModified() > 0 ? (System.currentTimeMillis() - file.lastModified()) / DAY : 0;
                boolean temporary = lower.endsWith(".tmp") || lower.endsWith(".log") || lower.endsWith(".bak") || lower.endsWith(".apk");
                boolean safeToClean = (lower.endsWith(".tmp") && age >= 7) || (lower.endsWith(".log") && age >= 30);
                if (file.length() >= LARGE_FILE || temporary || age >= 365) {
                    String reason = file.length() >= LARGE_FILE ? "Arquivo grande" : temporary ? "Possivel residuo" : "Arquivo antigo";
                    output.add(new FileCandidate(file, name, file.length(), age, reason, safeToClean));
                }
            }
        }
    }

    private void addFileCandidate(FileCandidate item) {
        String detail = item.reason + " | " + format(item.size) + (item.age > 0 ? " | " + item.age + " dias" : "");
        LinearLayout card = card(item.name, detail);
        Button remove = smallButton("Revisar e excluir");
        remove.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Excluir este arquivo?")
                .setMessage(item.name + "\n\n" + format(item.size) + "\n\nEsta acao nao apaga outros arquivos ou aplicativos.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (d, w) -> {
                    if (item.file.delete()) {
                        content.removeView(card);
                        Toast.makeText(this, "Arquivo excluido", Toast.LENGTH_SHORT).show();
                    } else Toast.makeText(this, "O Android nao autorizou a exclusao", Toast.LENGTH_LONG).show();
                }).show());
        card.addView(remove);
        content.addView(card);
    }

    private void confirmSafeCleanup(Uri uri, List<FileCandidate> files, int count, long bytes) {
        new AlertDialog.Builder(this)
                .setTitle("Limpar residuos seguros?")
                .setMessage(count + " arquivos temporarios e logs antigos serao excluidos, liberando ate " + format(bytes) + ". Fotos, documentos, APKs e backups nao entram nesta limpeza.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Limpar", (d, w) -> {
                    busy(true, "Limpando residuos...");
                    worker.execute(() -> {
                        int removed = 0;
                        for (FileCandidate file : files) if (file.safeToClean && file.file.delete()) removed++;
                        final int totalRemoved = removed;
                        runOnUiThread(() -> {
                            Toast.makeText(this, totalRemoved + " residuos removidos", Toast.LENGTH_LONG).show();
                            scanFolder(uri);
                        });
                    });
                }).show();
    }

    private void analyzeSecurity() {
        clear("Seguranca e permissoes");
        busy(true, "Verificando aplicativos instalados...");
        worker.execute(() -> {
            List<String> warnings = new ArrayList<>();
            PackageManager pm = getPackageManager();
            for (PackageInfo pi : pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)) {
                if (pi.applicationInfo == null || (pi.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                if (pi.requestedPermissions == null) continue;
                for (String permission : pi.requestedPermissions) {
                    if ("android.permission.REQUEST_INSTALL_PACKAGES".equals(permission)) {
                        warnings.add(pi.applicationInfo.loadLabel(pm) + " pode instalar aplicativos de fontes externas.");
                    }
                }
            }
            Collections.sort(warnings);
            runOnUiThread(() -> {
                busy(false, warnings.size() + " pontos para revisar");
                addInfo("Esta verificacao nao declara que um aplicativo e virus; ela destaca permissoes que merecem revisao.");
                if (warnings.isEmpty()) addInfo("Nenhum aplicativo de terceiros com permissao de instalar APKs foi encontrado.");
                for (String warning : warnings) addWarning(warning);
                addAction("Verificar Play Protect", v -> openPlayProtect());
                addAction("Revisar apps desconhecidos", v -> startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)));
            });
        });
    }

    private void confirmOwnCache() {
        new AlertDialog.Builder(this).setTitle("Limpar cache?")
                .setMessage("Somente os arquivos temporarios deste diagnostico serao removidos.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Limpar", (d, w) -> {
                    deleteChildren(getCacheDir());
                    deleteChildren(getCodeCacheDir());
                    deleteChildren(getExternalCacheDir());
                    showCleaner();
                }).show();
    }

    private void confirmUninstall(AppCandidate app) {
        new AlertDialog.Builder(this).setTitle("Abrir desinstalacao?")
                .setMessage("O Android mostrara a confirmacao oficial para " + app.name + ". O diagnostico nao remove o app sozinho.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Continuar", (d, w) -> startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.packageName))))
                .show();
    }

    private boolean hasUsageAccess() {
        AppOpsManager ops = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    private void showRestrictedAccessHelp() {
        new AlertDialog.Builder(this)
                .setTitle("Analise avancada opcional")
                .setMessage("Em alguns aparelhos, o Android bloqueia esta permissao para apps instalados por APK. Primeiro abra os detalhes do DIG Diagnostico e, no menu superior, escolha Permitir configuracoes restritas. Depois volte e ative Acesso ao uso.\n\nSem isso, a analise padrao continua funcionando normalmente.")
                .setNegativeButton("Continuar sem acesso", null)
                .setNeutralButton("Acesso ao uso", (d, w) -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)))
                .setPositiveButton("Abrir detalhes do app", (d, w) -> openAppDetails(getPackageName()))
                .show();
    }

    private void openAppDetails(String packageName) {
        startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName)));
    }

    private void openPlayProtect() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.gms"));
        intent.setPackage("com.android.vending");
        try { startActivity(intent); } catch (Exception e) { startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS)); }
    }

    private void clear(String heading) {
        content.removeAllViews();
        TextView h = text(heading, 22, Color.rgb(18, 59, 66), true);
        h.setPadding(0, dp(10), 0, dp(10));
        content.addView(h);
    }

    private void addMetric(String label, String value) {
        content.addView(card(label, value));
    }

    private void addHealthCard(int health, int freePercent) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(18), dp(18), dp(18), dp(18));
        panel.setBackground(rounded(Color.rgb(224, 244, 239), 12, Color.rgb(144, 204, 191)));
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(text(String.valueOf(health), 44, Color.rgb(8, 102, 85), true));
        top.addView(text("  Nota de saude\n  " + (health >= 85 ? "Aparelho em bom estado" : health >= 65 ? "Alguns pontos para revisar" : "Atencao recomendada"), 15, Color.rgb(24, 70, 66), true));
        panel.addView(top);
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setIndeterminate(false);
        bar.setMax(100);
        bar.setProgress(health);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8));
        bp.setMargins(0, dp(14), 0, dp(8));
        panel.addView(bar, bp);
        panel.addView(text(freePercent + "% do armazenamento esta livre", 13, Color.rgb(52, 86, 84), false));
        content.addView(panel, spacedParams());
    }

    private void addMetricRow(String leftTitle, String leftValue, String rightTitle, String rightValue) {
        LinearLayout row = new LinearLayout(this);
        row.setBackground(rounded(Color.WHITE, 10, Color.rgb(224, 229, 232)));
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.addView(metricColumn(leftTitle, leftValue), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(metricColumn(rightTitle, rightValue), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        content.addView(row, spacedParams());
    }

    private LinearLayout metricColumn(String title, String value) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(text(title, 12, Color.rgb(103, 113, 117), false));
        column.addView(text(value, 16, Color.rgb(31, 49, 54), true));
        return column;
    }

    private void addInfo(String message) {
        TextView view = text(message, 14, Color.rgb(55, 65, 67), false);
        view.setPadding(dp(14), dp(14), dp(14), dp(14));
        view.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams lp = spacedParams();
        content.addView(view, lp);
    }

    private void addWarning(String message) {
        TextView view = text(message, 14, Color.rgb(101, 58, 0), false);
        view.setPadding(dp(14), dp(14), dp(14), dp(14));
        view.setBackgroundColor(Color.rgb(255, 244, 218));
        content.addView(view, spacedParams());
    }

    private void addAction(String label, View.OnClickListener action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setBackground(rounded(Color.rgb(8, 126, 109), 9, 0));
        button.setPadding(dp(14), dp(10), dp(14), dp(10));
        button.setOnClickListener(action);
        content.addView(button, spacedParams());
    }

    private void addSecondaryAction(String label, View.OnClickListener action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(Color.rgb(25, 71, 78));
        button.setBackground(rounded(Color.WHITE, 9, Color.rgb(198, 211, 214)));
        button.setOnClickListener(action);
        content.addView(button, spacedParams());
    }

    private LinearLayout card(String title, String detail) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.setBackground(rounded(Color.WHITE, 10, Color.rgb(225, 230, 232)));
        card.addView(text(title, 16, Color.rgb(25, 42, 45), true));
        TextView d = text(detail, 13, Color.rgb(80, 92, 94), false);
        d.setPadding(0, dp(4), 0, dp(4));
        card.addView(d);
        card.setLayoutParams(spacedParams());
        return card;
    }

    private Button navButton(String label, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(11);
        b.setTextColor(Color.rgb(38, 66, 71));
        b.setAllCaps(false);
        b.setOnClickListener(listener);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(3), 0, dp(3), 0);
        b.setBackground(rounded(Color.rgb(240, 245, 245), 8, 0));
        b.setLayoutParams(new LinearLayout.LayoutParams(0, dp(46), 1));
        return b;
    }

    private Button smallButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        return b;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private GradientDrawable rounded(int fill, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radius));
        if (stroke != 0) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams spacedParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(8));
        return lp;
    }

    private void busy(boolean active, String message) {
        progress.setVisibility(active ? View.VISIBLE : View.GONE);
        status.setText(message);
    }

    private String format(long bytes) {
        return Formatter.formatFileSize(this, Math.max(0, bytes));
    }

    private long folderSize(File file) {
        if (file == null || !file.exists()) return 0;
        if (file.isFile()) return file.length();
        long total = 0;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) total += folderSize(child);
        return total;
    }

    private void deleteChildren(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) deleteChildren(child);
            child.delete();
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (downloadReceiver != null) unregisterReceiver(downloadReceiver);
        worker.shutdownNow();
        super.onDestroy();
    }

    private static class AppCandidate {
        final String name;
        final String packageName;
        final long days;
        final long size;
        final String reason;
        final int priority;
        AppCandidate(String name, String packageName, long days, long size, String reason, int priority) {
            this.name = name;
            this.packageName = packageName;
            this.days = days;
            this.size = size;
            this.reason = reason;
            this.priority = priority;
        }
    }

    private static class FileCandidate {
        final DocumentFile file;
        final String name;
        final long size;
        final long age;
        final String reason;
        final boolean safeToClean;
        FileCandidate(DocumentFile file, String name, long size, long age, String reason, boolean safeToClean) {
            this.file = file;
            this.name = name;
            this.size = size;
            this.age = age;
            this.reason = reason;
            this.safeToClean = safeToClean;
        }
    }
}

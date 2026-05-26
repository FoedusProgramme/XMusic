package com.xapps.media.xmusic.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.xapps.media.xmusic.data.DataManager;
import com.xapps.media.xmusic.databinding.ActivityCrashReportBinding;
import com.xapps.media.xmusic.fragment.ExperimentsFragment;
import com.xapps.media.xmusic.utils.Log;
import com.xapps.media.xmusic.utils.XUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import com.xapps.media.xmusic.R;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CrashReportActivity extends AppCompatActivity {
    
    private ActivityCrashReportBinding binding;
    private String error;
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
		XUtils.updateTheme();
        XUtils.applyDynamicColors(this, DataManager.isOledThemeEnabled());
        if (XUtils.isDarkMode(this) && DataManager.isOledThemeEnabled())getTheme().applyStyle(R.style.ThemeOverlay_XMusic_OLED, true);
        super.onCreate(savedInstanceState);
        binding = ActivityCrashReportBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        error = getIntent().getStringExtra("error");
        binding.reportButton.setOnClickListener(v -> {
            try {
                String suffix = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.US).format(new java.util.Date());
                File file = new File(getCacheDir(), "crash_report-" + suffix + ".txt");
                FileOutputStream fos = new FileOutputStream(file);
                String report = error == null? "Unknown error" : error;
                fos.write(report.getBytes());
                fos.close();

                Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);

                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(Intent.createChooser(intent, "Share crash report"));

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
		
		binding.restartButton.setOnClickListener(v -> {
			startActivity(new Intent(this, SplashActivity.class));
			finish();
		});
		binding.exportButton.setOnClickListener(v -> {
			XUtils.showMessage(this, "Exporting logs...");
			exportLogs(this);
		});
    }

    private void exportLogs(Activity c) {
        executorService.execute(() -> {
            try {
                java.lang.Process p = new ProcessBuilder()
                    .command("logcat", "-dball", "--uid=" + android.os.Process.myUid())
                    .redirectErrorStream(true)
                    .start();

                File dir = new File(c.getCacheDir(), "logs");
                dir.mkdirs();

                File out = new File(dir, "xmusic_log.txt");
				if (out.exists()) out.delete();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                     FileWriter writer = new FileWriter(out)) {
                    writer.write("SDK: " + Build.VERSION.SDK_INT + "\n");
                    writer.write("Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n\n");

                    String line;
                    while ((line = reader.readLine()) != null) {
                        writer.write(line);
                        writer.write('\n');
                    }
                }

                p.waitFor();
                p.destroy();

                Uri uri = FileProvider.getUriForFile(
                    c,
                    c.getPackageName() + ".provider",
                    out
                );

                c.runOnUiThread(() -> {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(intent, "Export logs"));
                });

            } catch (Exception e) {
                Log.e("XMusic", "Log export failed", e);
            }
        });
    }
}

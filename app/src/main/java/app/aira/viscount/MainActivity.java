package app.aira.viscount;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import top.niunaijun.blackbox.BlackBoxCore;

public class MainActivity extends Activity {

    private static final int USER_ID = 0;

    private EditText editInput;
    private TextView tvLog;
    private ScrollView scrollView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editInput = findViewById(R.id.edit_input);
        tvLog = findViewById(R.id.tv_log);
        scrollView = (ScrollView) tvLog.getParent();

        Button btnInstallPackage = findViewById(R.id.btn_install_package);
        Button btnInstallApk = findViewById(R.id.btn_install_apk);
        Button btnLaunch = findViewById(R.id.btn_launch);

        btnInstallPackage.setOnClickListener(v -> installByPackageName());
        btnInstallApk.setOnClickListener(v -> installByApkPath());
        btnLaunch.setOnClickListener(v -> launchApp());
    }

    private void installByPackageName() {
        String packageName = getInput();
        if (packageName == null) return;

        log("Installing by package name: " + packageName);
        new Thread(() -> {
            try {
                var result = BlackBoxCore.get().installPackageAsUser(packageName, USER_ID);
                mainHandler.post(() -> {
                    if (result != null && result.isSuccess()) {
                        log("Install SUCCESS: " + packageName);
                    } else {
                        log("Install FAILED: " + packageName + (result != null ? " - " + result.getMessage() : ""));
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> log("Install ERROR: " + e.getMessage()));
            }
        }).start();
    }

    private void installByApkPath() {
        String path = getInput();
        if (path == null) return;

        File apkFile = new File(path);
        if (!apkFile.exists()) {
            log("File not found: " + path);
            return;
        }

        log("Installing APK from: " + path);
        new Thread(() -> {
            try {
                var result = BlackBoxCore.get().installPackageAsUser(apkFile, USER_ID);
                mainHandler.post(() -> {
                    if (result != null && result.isSuccess()) {
                        log("Install SUCCESS: " + apkFile.getName());
                    } else {
                        log("Install FAILED: " + apkFile.getName() + (result != null ? " - " + result.getMessage() : ""));
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> log("Install ERROR: " + e.getMessage()));
            }
        }).start();
    }

    private void launchApp() {
        String packageName = getInput();
        if (packageName == null) return;

        log("Launching: " + packageName);
        new Thread(() -> {
            try {
                boolean success = BlackBoxCore.get().launchApk(packageName, USER_ID);
                mainHandler.post(() -> {
                    if (success) {
                        log("Launch SUCCESS: " + packageName);
                    } else {
                        log("Launch FAILED: " + packageName);
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> log("Launch ERROR: " + e.getMessage()));
            }
        }).start();
    }

    private String getInput() {
        String text = editInput.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Enter a package name or APK path", Toast.LENGTH_SHORT).show();
            return null;
        }
        return text;
    }

    private void log(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String entry = "[" + timestamp + "] " + message + "\n";
        tvLog.append(entry);
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }
}

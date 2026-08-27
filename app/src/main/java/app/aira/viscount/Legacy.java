package app.aira.viscount;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.frameworks.BPackageManager;

public class MainActivity extends Activity {

    private static final int USER_ID = 0;

    private EditText editInput;
    private TextView tvLog;
    private ScrollView scrollView;
    private EditText editShellCmd;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editInput = findViewById(R.id.edit_input);
        tvLog = findViewById(R.id.tv_log);
        scrollView = findViewById(R.id.scroll_log);
        editShellCmd = findViewById(R.id.edit_shell_cmd);

        Button btnInstallPackage = findViewById(R.id.btn_install_package);
        Button btnInstallApk = findViewById(R.id.btn_install_apk);
        Button btnLaunch = findViewById(R.id.btn_launch);
        Button btnListInstalled = findViewById(R.id.btn_list_installed);
        Button btnShellExec = findViewById(R.id.btn_shell_exec);

        btnInstallPackage.setOnClickListener(v -> installByPackageName());
        btnInstallApk.setOnClickListener(v -> installByApkPath());
        btnLaunch.setOnClickListener(v -> launchApp());
        btnListInstalled.setOnClickListener(v -> listInstalledApps());
        btnShellExec.setOnClickListener(v -> executeShellCommand());
    }

    private void installByPackageName() {
        String packageName = getInput();
        if (packageName == null) return;

        log("Installing by package name: " + packageName);
        new Thread(() -> {
            try {
                var result = BlackBoxCore.get().installPackageAsUser(packageName, USER_ID);
                mainHandler.post(() -> {
                    if (result != null && result.success) {
                        log("Install SUCCESS: " + packageName);
                    } else {
                        log("Install FAILED: " + packageName + (result != null ? " - " + result.msg : ""));
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
                    if (result != null && result.success) {
                        log("Install SUCCESS: " + apkFile.getName());
                    } else {
                        log("Install FAILED: " + apkFile.getName() + (result != null ? " - " + result.msg : ""));
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

    private void listInstalledApps() {
        log("--- Listing Installed Apps (Virtual Engine) ---");
        new Thread(() -> {
            try {
                BPackageManager pm = BlackBoxCore.getBPackageManager();

                boolean isDirectlyInstalled = pm.isInstalled("com.example.test", USER_ID);
                log("isInstalled() test: " + isDirectlyInstalled);

                List<PackageInfo> packages = pm.getInstalledPackages(0, USER_ID);
                if (packages == null || packages.isEmpty()) {
                    mainHandler.post(() -> log("No installed packages found in virtual engine"));
                    return;
                }

                log("Found " + packages.size() + " installed package(s):");
                log("");

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < packages.size(); i++) {
                    PackageInfo pi = packages.get(i);
                    String pkgName = pi.packageName;
                    String version = pi.versionName != null ? pi.versionName : "N/A";
                    String uid = String.valueOf(pi.applicationInfo != null ? pi.applicationInfo.uid : -1);

                    Drawable icon = null;
                    String label = pkgName;
                    try {
                        if (pi.applicationInfo != null) {
                            android.content.pm.PackageManager sysPm = getPackageManager();
                            icon = sysPm.getApplicationIcon(pi.applicationInfo);
                            CharSequence labelCs = sysPm.getApplicationLabel(pi.applicationInfo);
                            if (labelCs != null) label = labelCs.toString();
                        }
                    } catch (Exception e) {
                        icon = null;
                    }

                    String iconStatus = icon != null ? "[ICON OK]" : "[NO ICON]";
                    sb.append(i + 1).append(". ").append(label)
                      .append(" (").append(pkgName).append(")")
                      .append(" v").append(version)
                      .append(" uid=").append(uid)
                      .append(" ").append(iconStatus)
                      .append("\n");
                }

                String result = sb.toString();
                mainHandler.post(() -> {
                    log(result);
                    log("--- End of Installed Apps List ---");
                });

            } catch (Exception e) {
                mainHandler.post(() -> log("List ERROR: " + e.getMessage()));
            }
        }).start();
    }

    private void executeShellCommand() {
        String cmd = editShellCmd.getText().toString().trim();
        if (cmd.isEmpty()) {
            Toast.makeText(this, "Enter a shell command", Toast.LENGTH_SHORT).show();
            return;
        }

        log("$ " + cmd);
        new Thread(() -> {
            Process process = null;
            try {
                process = Runtime.getRuntime().exec(new String[]{"/system/bin/sh", "-c", cmd});

                DataOutputStream os = new DataOutputStream(process.getOutputStream());
                os.writeBytes(cmd + "\n");
                os.writeBytes("exit\n");
                os.flush();
                os.close();

                BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                StringBuilder output = new StringBuilder();
                String line;

                while ((line = stdout.readLine()) != null) {
                    output.append(line).append("\n");
                }

                StringBuilder errOutput = new StringBuilder();
                while ((line = stderr.readLine()) != null) {
                    errOutput.append(line).append("\n");
                }

                int exitCode = process.waitFor();

                String finalOutput = output.toString();
                String finalErr = errOutput.toString();
                mainHandler.post(() -> {
                    if (!finalOutput.isEmpty()) {
                        log(finalOutput.trim());
                    }
                    if (!finalErr.isEmpty()) {
                        log("[STDERR] " + finalErr.trim());
                    }
                    log("[exit: " + exitCode + "]");
                });

            } catch (Exception e) {
                mainHandler.post(() -> log("[ERROR] " + e.getMessage()));
            } finally {
                if (process != null) {
                    process.destroy();
                }
            }
        }, "ShellExec").start();
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
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            tvLog.append(entry);
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        } else {
            mainHandler.post(() -> {
                tvLog.append(entry);
                scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
            });
        }
    }
}

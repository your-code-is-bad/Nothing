package app.aira.viscount;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;



import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;
import top.niunaijun.blackbox.fake.frameworks.BPackageManager;


public class MainActivity extends AppCompatActivity {

    private int USER_ID = 0;
    private String packageName = "com.notdoppler.deadzed";
    private Button btnLaunch;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); 
        btnLaunch = findViewById(R.id.btn_launch);
        btnLaunch.setOnClickListener(v -> launchApp());
        start();
    }

    public void start() {
        boolean isInstall = checkInstall();
       if (!isInstall) {
            install();
        } else {
            Toast.makeText(this, "App is already installed", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean checkInstall() {
        boolean isInstall = BlackBoxCore.getBPackageManager().isInstalled(packageName, USER_ID);
        return isInstall;
    }

    public void install() {
        new Thread(() -> {
            try {
                var result = BlackBoxCore.get().installPackageAsUser(packageName, USER_ID);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        
        
    }

    public void launchApp() {
        new Thread(() -> {
            try {
                if (!checkInstall()) {
                    InstallResult result = BlackBoxCore.get().installPackageAsUser(packageName, USER_ID);
                    if (result == null || !result.success) {
                        String msg = result != null && result.msg != null ? result.msg : "Unknown error";
                        runOnUiThread(() -> Toast.makeText(this, "Install failed: " + msg, Toast.LENGTH_SHORT).show());
                        return;
                    }
                }
                boolean launched = BlackBoxCore.get().launchApk(packageName, USER_ID);
                if (!launched) {
                    runOnUiThread(() -> Toast.makeText(this, "Launch failed", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
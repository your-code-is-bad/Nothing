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
        var result = BlackBoxCore.get().installPackageAsUser(packageName, USER_ID);
        if (result.success) {
            Toast.makeText(this, "Installation successful", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Installation failed", Toast.LENGTH_SHORT).show();
        }
    }

    public void launchApp() {
        new Thread(() -> {
            try {
                BlackBoxCore.get().launchPackage(packageName, USER_ID);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        
      
    }
}
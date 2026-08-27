package app.aira.viscount;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;



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
        boolean isInstall = BlackBoxCore.get().getPackageManager().isInstalled(USER_ID, packageName);
        return isInstall;
    }

    public void install() {
        var result = BlackBoxCore.get().installPackage(USER_ID, packageName);
        if (result.isSuccess()) {
            Toast.makeText(this, "Installation successful", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Installation failed", Toast.LENGTH_SHORT).show();
        }
    }

    public void launchApp() {
        boolean isInstall = checkInstall();
        if (isInstall) {
            BlackBoxCore.get().launchApk(USER_ID, packageName);
        }
    }
}
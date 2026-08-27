package app.aira.viscount;

import android.os.Bundle;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import java.util.List;



import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;
import top.niunaijun.blackbox.fake.frameworks.BPackageManager;


public class MainActivity extends AppCompatActivity {

    private int USER_ID = 0;
    private String packageName = "com.notdoppler.deadzed";
    private Button btnLaunch;
    private LinearLayout appsContainer;
    private TextView emptyMessage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); 
        btnLaunch = findViewById(R.id.btn_launch);
        btnLaunch.setOnClickListener(v -> launchApp());
        appsContainer = findViewById(R.id.apps_container);
        emptyMessage = findViewById(R.id.tv_empty);
        inialised();
    }

    void inialised() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
           Thread.currentThread().interrupt();
           return;
        }	 
        //start();
        loadInstalledApps();
        
    }

    public void start() {
        boolean isInstall = checkInstall();
       if (isInstall) {
            Toast.makeText(this, "App is already installed", Toast.LENGTH_SHORT).show();
        } else {
	    install();
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
        launchApp(packageName);
    }

    private void launchApp(String packageName) {
        new Thread(() -> {
            try {
                
                boolean launched = BlackBoxCore.get().launchApk(packageName, USER_ID);
                if (!launched) {
                    runOnUiThread(() -> Toast.makeText(this, "Launch failed", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadInstalledApps() {
        try {
            List<PackageInfo> packages = BlackBoxCore.get().getInstalledPackages(0, USER_ID);
            runOnUiThread(() -> {
                appsContainer.removeAllViews();
                if (packages == null || packages.isEmpty()) {
		    install();
                    emptyMessage.setText("No installed apps found");
                    emptyMessage.setVisibility(View.VISIBLE);
                    return;
                }

                emptyMessage.setVisibility(View.GONE);
                for (PackageInfo packageInfo : packages) {
                    addAppRow(packageInfo);
                }
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                emptyMessage.setText("Could not load installed apps");
                emptyMessage.setVisibility(View.VISIBLE);
            });
        }
    }

    private void addAppRow(PackageInfo packageInfo) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_installed_app, appsContainer, false);
        TextView name = row.findViewById(R.id.app_name);
        TextView packageText = row.findViewById(R.id.app_package);
        ImageView icon = row.findViewById(R.id.app_icon);
        Button launch = row.findViewById(R.id.btn_launch_app);

        String label = packageInfo.packageName;
        Drawable appIcon = null;
        try {
            if (packageInfo.applicationInfo != null) {
                label = getPackageManager().getApplicationLabel(packageInfo.applicationInfo).toString();
                appIcon = getPackageManager().getApplicationIcon(packageInfo.applicationInfo);
            }
        } catch (Exception ignored) {
            // The package can still be launched when its label or icon is unavailable.
        }

        name.setText(label);
        packageText.setText(packageInfo.packageName);
        if (appIcon != null) {
            icon.setImageDrawable(appIcon);
        }
        String appPackageName = packageInfo.packageName;
        launch.setOnClickListener(v -> launchApp(appPackageName));
        appsContainer.addView(row);
    }
}

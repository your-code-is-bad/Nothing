package app.aira.viscount;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class GetApp extends Activity {

    private LinearLayout container;
    private static final int PICK_APK = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        Button btnPick = new Button(this);
        btnPick.setText("Install from APK");
        btnPick.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("application/vnd.android.package-archive");
            startActivityForResult(i, PICK_APK);
        });
        root.addView(btnPick, new LinearLayout.LayoutParams(-1, -2));

        container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        root.addView(container, new LinearLayout.LayoutParams(-1, -2));

        setContentView(root);

        PackageManager pm = getPackageManager();
        for (android.content.pm.ApplicationInfo ai : pm.getInstalledApplications(0)) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            ImageView icon = new ImageView(this);
            icon.setImageDrawable(ai.loadIcon(pm));
            row.addView(icon, new LinearLayout.LayoutParams(64, 64));

            TextView label = new TextView(this);
            label.setText(ai.loadLabel(pm).toString());
            label.setPadding(16, 0, 0, 0);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));

            Button btn = new Button(this);
            btn.setText("Import");
            String pkg = ai.packageName;
            btn.setOnClickListener(v -> install(pkg));
            row.addView(btn);

            container.addView(row);
        }
    }

    private void install(String pkg) {
        new MainActivity().install(pkg);
        Toast.makeText(this, "Importing: " + pkg, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_APK && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                File tmp = new File(getCacheDir(), "import.apk");
                try (InputStream in = getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(tmp)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                }
                new MainActivity().install(tmp);
                Toast.makeText(this, "APK imported", Toast.LENGTH_SHORT).show();
                finish();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Import failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}

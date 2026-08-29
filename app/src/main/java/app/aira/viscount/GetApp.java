package app.aira.viscount;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;

public class GetApp extends Activity {

    private static final int PICK_APK = 1;
    private static final int USER_ID = 0;

    private AppAdapter adapter;
    private TextView status;

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

        status = new TextView(this);
        status.setText("Loading apps...");
        status.setPadding(16, 8, 16, 8);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        ListView list = new ListView(this);
        root.addView(list, new LinearLayout.LayoutParams(-1, -1));

        setContentView(root);

        adapter = new AppAdapter();
        list.setAdapter(adapter);

        new Thread(this::loadUserApps).start();
    }

    private void loadUserApps() {
        final List<AppItem> items = new ArrayList<>();
        try {
            PackageManager pm = getPackageManager();
            for (ApplicationInfo ai : pm.getInstalledApplications(0)) {
                if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
                if (ai.packageName.equals(getPackageName())) continue;
                items.add(new AppItem(ai.packageName, ai.loadLabel(pm).toString(), ai.loadIcon(pm)));
            }
            Collections.sort(items, (a, b) -> a.label.compareToIgnoreCase(b.label));
        } catch (Exception e) {
            e.printStackTrace();
        }
        runOnUiThread(() -> {
            if (items.isEmpty()) {
                status.setText("No user apps installed");
            } else {
                status.setVisibility(View.GONE);
            }
            adapter.setItems(items);
        });
    }

    private void importApp(String pkg) {
        new Thread(() -> {
            InstallResult result = null;
            try {
                result = BlackBoxCore.get().installPackageAsUser(pkg, USER_ID);
            } catch (Exception e) {
                e.printStackTrace();
            }
            final InstallResult r = result;
            runOnUiThread(() -> {
                if (r != null && r.success) {
                    Toast.makeText(this, "Imported: " + pkg, Toast.LENGTH_SHORT).show();
                } else {
                    String msg = r != null ? r.msg : "Import failed";
                    Toast.makeText(this, "Import failed: " + msg, Toast.LENGTH_SHORT).show();
                }
                setResult(RESULT_OK);
                finish();
            });
        }).start();
    }

    private void importApk(File apk) {
        new Thread(() -> {
            InstallResult result = null;
            try {
                result = BlackBoxCore.get().installPackageAsUser(apk, USER_ID);
            } catch (Exception e) {
                e.printStackTrace();
            }
            final InstallResult r = result;
            runOnUiThread(() -> {
                if (r != null && r.success) {
                    Toast.makeText(this, "APK imported", Toast.LENGTH_SHORT).show();
                } else {
                    String msg = r != null ? r.msg : "Import failed";
                    Toast.makeText(this, "Import failed: " + msg, Toast.LENGTH_SHORT).show();
                }
                setResult(RESULT_OK);
                finish();
            });
        }).start();
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
                importApk(tmp);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Import failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private static class AppItem {
        final String packageName;
        final String label;
        final Drawable icon;

        AppItem(String packageName, String label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }
    }

    private class AppAdapter extends BaseAdapter {

        private final List<AppItem> items = new ArrayList<>();

        void setItems(List<AppItem> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            Holder holder;
            if (row == null) {
                holder = new Holder();
                row = buildRow(holder);
                row.setTag(holder);
            } else {
                holder = (Holder) row.getTag();
            }
            AppItem item = items.get(position);
            holder.icon.setImageDrawable(item.icon);
            holder.label.setText(item.label);
            holder.importBtn.setOnClickListener(v -> importApp(item.packageName));
            return row;
        }

        private View buildRow(Holder holder) {
            LinearLayout row = new LinearLayout(GetApp.this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(4), dp(16), dp(4));

            ImageView icon = new ImageView(GetApp.this);
            LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(48), dp(48));
            iconLp.setMargins(0, dp(4), 0, dp(4));
            row.addView(icon, iconLp);
            holder.icon = icon;

            TextView label = new TextView(GetApp.this);
            label.setPadding(dp(16), 0, 0, 0);
            label.setTextSize(16);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
            holder.label = label;

            Button btn = new Button(GetApp.this);
            btn.setText("Import");
            row.addView(btn);
            holder.importBtn = btn;
            return row;
        }
    }

    private static class Holder {
        ImageView icon;
        TextView label;
        Button importBtn;
    }
}
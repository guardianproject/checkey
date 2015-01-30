
package info.guardianproject.checkey;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class DetailViewActivity extends ActionBarActivity {
    public static final String TAG = "DetailViewActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_view);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }

        String appName = intent.getStringExtra(Intent.EXTRA_TEXT);
        TextView appNameTextView = (TextView) findViewById(R.id.app_name);
        appNameTextView.setText(appName);

        String packageName = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        TextView packageNameTextView = (TextView) findViewById(R.id.package_name);
        packageNameTextView.setText(packageName);

        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(packageName,
                    PackageManager.GET_UNINSTALLED_PACKAGES);
            Drawable icon = info.loadIcon(pm);
            ImageView appIconImageView = (ImageView) findViewById(R.id.app_icon);
            appIconImageView.setImageDrawable(icon);
            TextView apkPathTextView = (TextView) findViewById(R.id.apk_path);
            apkPathTextView.setText(info.sourceDir);
            TextView publicApkPathLabel = (TextView) findViewById(R.id.public_apk_path_label);
            TextView publicApkPathTextView = (TextView) findViewById(R.id.public_apk_path);
            if (TextUtils.equals(info.sourceDir, info.publicSourceDir)) {
                publicApkPathLabel.setVisibility(View.GONE);
                publicApkPathTextView.setVisibility(View.GONE);
            } else {
                publicApkPathLabel.setVisibility(View.VISIBLE);
                publicApkPathTextView.setVisibility(View.VISIBLE);
                publicApkPathTextView.setText(info.publicSourceDir);
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        MainActivity.showCertificateInfo(this, packageName);
    }
}

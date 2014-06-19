
package info.guardianproject.checkey;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class MainActivity extends ActionBarActivity {
    private final String TAG = "MainActivity";

    private static PackageManager pm;
    private static CertificateFactory certificateFactory;
    private static int selectedItem = -1;
    private AppListFragment appListFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appListFragment == null)
            appListFragment = (AppListFragment) getSupportFragmentManager().findFragmentById(
                    R.id.fragment_app_list);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ListAdapter adapter = appListFragment.getListAdapter();
        AppEntry appEntry = (AppEntry) adapter.getItem(selectedItem);
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra(Intent.EXTRA_TEXT, appEntry.getLabel());
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
            case R.id.save:
                saveCertificate(appEntry, intent);
                return true;
            case R.id.virustotal:
                virustotal(appEntry, intent);
                return true;
            case R.id.by_apk_hash:
                byApkHash(appEntry, intent);
                return true;
            case R.id.by_package_name:
                byPackageName(appEntry, intent);
                return true;
            case R.id.by_signing_certificate:
                bySigningCertificate(appEntry, intent);
                return true;
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static X509Certificate[] getX509Certificates(Context context, String packageName) {
        X509Certificate[] certs = null;
        if (pm == null)
            pm = context.getApplicationContext().getPackageManager();
        try {
            PackageInfo pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            if (certificateFactory == null)
                certificateFactory = CertificateFactory.getInstance("X509");
            certs = new X509Certificate[pkgInfo.signatures.length];
            for (int i = 0; i < certs.length; i++) {
                byte[] cert = pkgInfo.signatures[i].toByteArray();
                InputStream inStream = new ByteArrayInputStream(cert);
                certs[i] = (X509Certificate) certificateFactory.generateCertificate(inStream);
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        return certs;
    }

    private static void showCertificateInfo(Activity activity, AppEntry appEntry) {
        String packageName = appEntry.getPackageName();
        X509Certificate[] certs = getX509Certificates(activity, packageName);
        if (certs == null || certs.length < 1)
            return;
        // for now, just support the first cert since that is far and away
        // the
        // most common
        X509Certificate cert = certs[0];
        TextView issuerdn = (TextView) activity.findViewById(R.id.issuerdn);
        issuerdn.setText(cert.getIssuerDN().getName());
        TextView subjectdn = (TextView) activity.findViewById(R.id.subjectdn);
        subjectdn.setText(cert.getSubjectDN().getName());
    }

    @SuppressLint("WorldReadableFiles")
    private void saveCertificate(AppEntry appEntry, Intent intent) {
        String packageName = appEntry.getPackageName();
        try {
            for (X509Certificate x509 : getX509Certificates(this, packageName)) {
                String fileName = packageName + ".cer";
                @SuppressWarnings("deprecation")
                final FileOutputStream os = openFileOutput(fileName,
                        Context.MODE_WORLD_READABLE);
                os.write(x509.getEncoded());
                os.close();

                String subject = packageName + " - " + x509.getIssuerDN().getName()
                        + " - " + x509.getNotAfter();
                Uri uri = Uri.fromFile(getFileStreamPath(fileName));
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("application/pkix-cert");
                i.putExtra(Intent.EXTRA_STREAM, uri);
                i.putExtra(Intent.EXTRA_TITLE, subject);
                i.putExtra(Intent.EXTRA_SUBJECT, subject);
                startActivity(Intent.createChooser(i, getString(R.string.save_cert_using)));
            }
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void virustotal(AppEntry appEntry, Intent intent) {
        String urlString = "https://www.virustotal.com/en/file/"
                + Utils.getBinaryHash(appEntry.getApkFile(), "sha256") + "/analysis/";
        intent.setData(Uri.parse(urlString));
        intent.putExtra(Intent.EXTRA_TITLE, R.string.virustotal);
        startActivity(intent);
    }

    private void byApkHash(AppEntry appEntry, Intent intent) {
        String urlString = "https://androidobservatory.org/?searchby=binhash&q="
                + Utils.getBinaryHash(appEntry.getApkFile(), "sha1");
        intent.setData(Uri.parse(urlString));
        intent.putExtra(Intent.EXTRA_TITLE, R.string.by_apk_hash);
        startActivity(intent);
    }

    private void byPackageName(AppEntry appEntry, Intent intent) {
        String urlString = "https://androidobservatory.org/?searchby=pkg&q="
                + appEntry.getPackageName();
        intent.setData(Uri.parse(urlString));
        intent.putExtra(Intent.EXTRA_TITLE, R.string.by_package_name);
        startActivity(intent);
    }

    private void bySigningCertificate(AppEntry appEntry, Intent intent) {
        String sha1;
        try {
            sha1 = Utils.getCertificateFingerprint(appEntry.getApkFile(), "sha1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            Toast.makeText(this, "Cannot make fingerprint of signing certificate",
                    Toast.LENGTH_LONG).show();
            return;
        }
        intent.setData(Uri.parse("https://androidobservatory.org/?searchby=certhash&q=" + sha1));
        intent.putExtra(Intent.EXTRA_TITLE, R.string.by_signing_certificate);
        startActivity(intent);
    }

    public static class AppListFragment extends ListFragment implements
            LoaderCallbacks<List<AppEntry>> {

        private AppListAdapter adapter;
        private ListView listView;
        private static final String STATE_CHECKED = "info.guardianproject.checkey.STATE_CHECKED";
        WebView androidObservatoryView;

        public AppListFragment() {
            super();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            setEmptyText(getString(R.string.no_applications_found));

            adapter = new AppListAdapter(getActivity());
            setListAdapter(adapter);
            setListShown(false);

            listView = getListView();
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

            if (savedInstanceState != null) {
                int position = savedInstanceState.getInt(STATE_CHECKED, -1);
                if (position > -1) {
                    listView.setItemChecked(position, true);
                }
            }

            // Prepare the loader
            // either reconnect with an existing one or start a new one
            getLoaderManager().initLoader(0, null, this);
        }

        @Override
        public void onSaveInstanceState(Bundle savedInstanceState) {
            super.onSaveInstanceState(savedInstanceState);
            savedInstanceState.putInt(STATE_CHECKED, selectedItem);
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            // Start the CAB using the ActionMode.Callback defined above
            ActionBarActivity activity = (ActionBarActivity) getActivity();
            selectedItem = position;
            AppEntry appEntry = (AppEntry) adapter.getItem(selectedItem);
            showCertificateInfo(activity, appEntry);
        }

        @Override
        public Loader<List<AppEntry>> onCreateLoader(int id, Bundle args) {
            // This is called when a new loader needs to be created.
            // This sample only has one Loader with no arguments, so it is
            // simple.
            return new AppListLoader(getActivity());
        }

        @Override
        public void onLoadFinished(Loader<List<AppEntry>> loader, List<AppEntry> data) {
            adapter.setData(data);

            // The list should now be shown
            if (isResumed()) {
                setListShown(true);
            } else {
                setListShownNoAnimation(true);
            }
        }

        @Override
        public void onLoaderReset(Loader<List<AppEntry>> loader) {
            // Clear the data in the adapter
            adapter.setData(null);
        }
    }
}

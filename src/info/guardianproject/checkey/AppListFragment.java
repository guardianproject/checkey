/*
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Based on Paul Blundell's Tutorial:
http://blog.blundell-apps.com/tut-asynctask-loader-using-support-library/

which is originally based on:
https://developer.android.com/reference/android/content/AsyncTaskLoader.html
 */

package info.guardianproject.checkey;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

public class AppListFragment extends ListFragment implements LoaderCallbacks<List<AppEntry>> {

    private PackageManager pm;
    private AppListAdapter adapter;
    private ActionMode actionMode;
    private ListView listView;
    private int selectedItem = -1;
    private static final String STATE_CHECKED = "info.guardianproject.checkey.STATE_CHECKED";
    WebView androidObservatoryView;

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
        if (actionMode != null) {
            return;
        }

        // Start the CAB using the ActionMode.Callback defined above
        ActionBarActivity activity = (ActionBarActivity) getActivity();
        actionMode = activity.startSupportActionMode(mActionModeCallback);
        selectedItem = position;
    }

    private void saveCertificate(AppEntry appEntry, Intent intent) {
        if (pm == null)
            pm = getActivity().getApplicationContext().getPackageManager();
        Activity activity = getActivity();
        String packageName = appEntry.getPackageName();
        PackageInfo pkgInfo;
        try {
            pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            CertificateFactory certFactory = CertificateFactory.getInstance("X509");
            for (Signature sig : pkgInfo.signatures) {
                byte[] cert = sig.toByteArray();
                InputStream inStream = new ByteArrayInputStream(cert);
                X509Certificate x509 = (X509Certificate) certFactory.generateCertificate(inStream);

                String fileName = packageName + ".cer";
                final FileOutputStream os = activity.openFileOutput(fileName,
                        Context.MODE_WORLD_READABLE);
                os.write(cert);
                os.close();

                String subject = packageName + " - " + x509.getIssuerDN().getName()
                        + " - " + x509.getNotAfter();
                Uri uri = Uri.fromFile(activity.getFileStreamPath(fileName));
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("application/pkix-cert");
                i.putExtra(Intent.EXTRA_STREAM, uri);
                i.putExtra(Intent.EXTRA_TITLE, subject);
                i.putExtra(Intent.EXTRA_SUBJECT, subject);
                startActivity(Intent.createChooser(i, getString(R.string.save_cert_using)));
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
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
            Toast.makeText(getActivity(), "Cannot make fingerprint of signing certificate",
                    Toast.LENGTH_LONG).show();
            return;
        }
        intent.setData(Uri.parse("https://androidobservatory.org/?searchby=certhash&q=" + sha1));
        intent.putExtra(Intent.EXTRA_TITLE, R.string.by_signing_certificate);
        startActivity(intent);
    }

    @Override
    public Loader<List<AppEntry>> onCreateLoader(int id, Bundle args) {
        // This is called when a new loader needs to be created.
        // This sample only has one Loader with no arguments, so it is simple.
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

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        // Called when the action mode is created; startActionMode() was called
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.context, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            AppEntry appEntry = (AppEntry) adapter.getItem(selectedItem);
            Intent intent = new Intent(getActivity(), WebViewActivity.class);
            intent.putExtra(Intent.EXTRA_TEXT, appEntry.getLabel());
            switch (item.getItemId()) {
                case R.id.save:
                    saveCertificate(appEntry, intent);
                    break;
                case R.id.virustotal:
                    virustotal(appEntry, intent);
                    break;
                case R.id.by_apk_hash:
                    byApkHash(appEntry, intent);
                    break;
                case R.id.by_package_name:
                    byPackageName(appEntry, intent);
                    break;
                case R.id.by_signing_certificate:
                    bySigningCertificate(appEntry, intent);
                    break;

                default:
                    return false;
            }
            mode.finish();
            return true;
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            listView.setItemChecked(selectedItem, false);
            listView.clearChoices();
            selectedItem = -1;
            actionMode = null;
        }
    };
}

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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;

import java.security.NoSuchAlgorithmException;
import java.util.List;

public class AppListFragment extends ListFragment implements LoaderCallbacks<List<AppEntry>> {

    private AppListAdapter adapter;
    WebView androidObservatoryView;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setEmptyText(getString(R.string.no_applications_found));

        adapter = new AppListAdapter(getActivity());
        setListAdapter(adapter);
        setListShown(false);

        registerForContextMenu(getListView());

        // Prepare the loader
        // either reconnect with an existing one or start a new one
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
        AppEntry appEntry = (AppEntry) adapter.getItem(menuInfo.position);
        Intent intent = new Intent(getActivity(), WebViewActivity.class);
        switch (item.getItemId()) {
            case R.id.by_apk_hash:
                byApkHash(appEntry, intent);
                break;
            case R.id.by_package_name:
                byPackageName(appEntry, intent);
                break;
            case R.id.by_signing_certificate:
                bySigningCertificate(appEntry, intent);
                break;
        }
        return true;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        AppEntry appEntry = (AppEntry) adapter.getItem(position);
        Intent intent = new Intent(getActivity(), WebViewActivity.class);
        byApkHash(appEntry, intent);
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
}

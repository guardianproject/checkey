
package info.guardianproject.checkey;

import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewActivity extends ActionBarActivity {

    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        int resid = intent.getIntExtra(Intent.EXTRA_TITLE, 0);
        if (resid != 0)
            actionBar.setTitle(resid);

        WebView webView = (WebView) findViewById(R.id.webview);
        webView.setWebViewClient(new MyWebViewClient());
        uri = intent.getData();
        webView.loadUrl(uri.toString());
        Log.i("WebViewActivity", uri.toString());
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri clickedUri = Uri.parse(url);
            String host = clickedUri.getHost();
            if (host.equals("www.virustotal.com") || host.equals("androidobservatory.org")) {
                // do not override; let my WebView load the page
                return false;
            }
            // otherwise launch another Activity to handle the link
            startActivity(new Intent(Intent.ACTION_VIEW, clickedUri));
            return true;
        }

    }
}

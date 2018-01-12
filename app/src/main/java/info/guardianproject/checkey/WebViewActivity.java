
package info.guardianproject.checkey;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebViewActivity extends ActionBarActivity {

    private Intent intent;
    private Uri uri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        intent = getIntent();
        int resid = intent.getIntExtra(Intent.EXTRA_TITLE, 0);
        if (resid != 0)
            actionBar.setTitle(resid);

        WebView webView = (WebView) findViewById(R.id.webview);
        webView.setWebViewClient(new MyWebViewClient());
        uri = intent.getData();
        webView.loadUrl(uri.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.webview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        try {
            switch (item.getItemId()) {
                case android.R.id.home:
                    setResult(RESULT_CANCELED);
                    finish();
                    return true;
                case R.id.share:
                    String appName = intent.getStringExtra(Intent.EXTRA_TEXT);
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("plain/text");
                    i.putExtra(Intent.EXTRA_TITLE, appName);
                    i.putExtra(Intent.EXTRA_SUBJECT, appName);
                    i.putExtra(Intent.EXTRA_TEXT, uri.toString());
                    startActivity(Intent.createChooser(i, getString(R.string.share_url_using)));
                    return true;
                case R.id.open_in_browser:
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                    return true;
            }
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
        return super.onOptionsItemSelected(item);
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (TextUtils.isEmpty(url))
                return true;
            Uri clickedUri = Uri.parse(url);
            if (uri == null)
                return true;
            String host = clickedUri.getHost();
            if (host.equals("www.virustotal.com") || host.equals("androidobservatory.org")) {
                // do not override; let my WebView load the page
                return false;
            }
            // otherwise launch another Activity to handle the link
            startActivity(new Intent(Intent.ACTION_VIEW, clickedUri));
            return true;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            String host = uri.getHost();
            int errno = error.getPrimaryError();
            if (host.equals("androidobservatory.org")
                    && (errno == SslError.SSL_EXPIRED || errno == SslError.SSL_UNTRUSTED)) {
                handler.proceed();
            } else {
                super.onReceivedSslError(view, handler, error);
            }
        }

    }
}


package info.guardianproject.checkey;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.webkit.WebView;

public class WebViewActivity extends ActionBarActivity {

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
        webView.loadUrl(intent.getData().toString());
        Log.i("WebViewActivity", intent.getData().toString());
    }

}

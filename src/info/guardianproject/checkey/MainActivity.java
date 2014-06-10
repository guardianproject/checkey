
package info.guardianproject.checkey;

import android.annotation.TargetApi;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

@TargetApi(11)
// TODO replace with appcompat-v7
public class MainActivity extends FragmentActivity {
    private final String TAG = "MainActivity";
    private AppListFragment appListFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_select);
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
        switch (item.getItemId()) {
            case android.R.id.home:
                setResult(RESULT_CANCELED);
                finish();
                return true;
            case R.id.action_settings:
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
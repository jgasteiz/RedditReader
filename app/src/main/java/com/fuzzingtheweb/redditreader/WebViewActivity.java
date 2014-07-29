package com.fuzzingtheweb.redditreader;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import com.fuzzingtheweb.redditreader.R;

public class WebViewActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        // Create an intent for displaying the url in a webView
        Intent intent = getIntent();
        Uri blogUri = intent.getData();
        String url = blogUri.toString();

        // Create the fragment
        Bundle arguments = new Bundle();
        arguments.putString(WebViewFragment.KEY_URL, url);
        WebViewFragment fragment = new WebViewFragment();
        fragment.setArguments(arguments);
        getFragmentManager().beginTransaction()
                .add(R.id.web_view, fragment)
                .commit();
    }

}

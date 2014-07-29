package com.fuzzingtheweb.redditreader;


import android.app.ListFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class FeedFragment extends ListFragment {
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String LOG_TAG = FeedFragment.class.getSimpleName();

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static FeedFragment newInstance(int sectionNumber, String title) {
        FeedFragment fragment = new FeedFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public FeedFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        Log.v(LOG_TAG, "Section active: " + getArguments().getInt(ARG_SECTION_NUMBER));
        refreshFeed(getArguments().getInt(ARG_SECTION_NUMBER));

        return rootView;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        FeedItem feedItem = (FeedItem) l.getItemAtPosition(position);
        Intent intent = new Intent(getActivity(), WebViewActivity.class);
        intent.setData(Uri.parse(feedItem.getUrl()));
        startActivity(intent);
    }

    private void refreshFeed(int itemPosition) {
        String itemName = SectionsPagerAdapter.SECTIONS[itemPosition];
        FetchFeedTask fetchFeedTask = new FetchFeedTask(itemName);
        fetchFeedTask.execute();
    }

    public void populateListView(final ArrayList<FeedItem> feed) {

        ArrayAdapter<FeedItem> feedAdapter = new ArrayAdapter<FeedItem> (
                getActivity(),
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                feed)
        {
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                text1.setText(feed.get(position).getTitle());
                text2.setText(feed.get(position).getUrl());
                return view;
            }
        };

        try {
            ListView listView = (ListView) getView().findViewById(android.R.id.list);
            listView.setAdapter(feedAdapter);
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    /**
     * Asynctask for making a call to the API.
     */
    private class FetchFeedTask extends AsyncTask<Object, Void, JSONObject> {

        private String mItemName;

        private final String LOG_TAG = FetchFeedTask.class.getSimpleName();
        private final String KEY_TITLE = "title";
        private final String KEY_URL = "url";

        public FetchFeedTask(String itemName) {
            mItemName = itemName;
        }

        @Override
        protected JSONObject doInBackground(Object[] params) {
            JSONObject jsonResponse = null;
            StringBuilder builder = new StringBuilder();
            HttpClient client = new DefaultHttpClient();
            int responseCode;

            String baseUrl = "http://www.reddit.com/r/" + mItemName + ".json";

            HttpGet httpget = new HttpGet(baseUrl);

            try {
                HttpResponse response = client.execute(httpget);
                StatusLine statusLine = response.getStatusLine();
                responseCode = statusLine.getStatusCode();

                Log.v(LOG_TAG, "URL : " + baseUrl);
                Log.v(LOG_TAG, "Response code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    HttpEntity entity = response.getEntity();
                    InputStream content = entity.getContent();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(content));

                    String line;
                    while((line = reader.readLine()) != null){
                        builder.append(line);
                    }

                    jsonResponse = new JSONObject(builder.toString());
//                    Log.v(LOG_TAG, "Response: " + jsonResponse);
                } else {
                    Log.i(LOG_TAG, "Unsuccessful HTTP Response Code: " + responseCode);
                }
                Log.i(LOG_TAG, "Code: " + responseCode);
            } catch (MalformedURLException e) {
                Log.e(LOG_TAG, e.getMessage());
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
            }

            return jsonResponse;
        }

        @Override
        protected void onPostExecute(JSONObject jsonResponse) {
            JSONArray jsonFeed;
            JSONObject postData;
            String title, url;

            ArrayList<FeedItem> items = new ArrayList<FeedItem>();

            if (jsonResponse != null) {
                try {
                    jsonFeed = jsonResponse.getJSONObject("data").getJSONArray("children");
                    for (int i = 0; i < jsonFeed.length(); i++) {
                        postData = jsonFeed.getJSONObject(i).getJSONObject("data");
                        title = Html.fromHtml(postData.getString(KEY_TITLE)).toString();
                        url = postData.getString(KEY_URL);

                        items.add(new FeedItem(title, url));
//                        Log.v(LOG_TAG, "Post " + i + ": " + title + ", " + url);
                    }

                    populateListView(items);

                } catch (JSONException e) {
                    Log.e(LOG_TAG, e.getMessage());
                }
            }
        }
    }
}

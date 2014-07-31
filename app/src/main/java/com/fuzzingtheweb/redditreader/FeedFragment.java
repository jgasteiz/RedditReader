package com.fuzzingtheweb.redditreader;


import android.app.ListFragment;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.fuzzingtheweb.redditreader.data.FeedDBAdapter;

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

    private FeedDBAdapter mDBHelper;

    // Views
    private View mRootView;
    private ListView mListView;
    private ProgressBar mProgressBar;

    private int mSubRedditPosition;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mSubRedditPosition = getArguments().getInt(ARG_SECTION_NUMBER);
        mDBHelper = new FeedDBAdapter(getActivity());
        mDBHelper.open();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.feed_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refreshFeed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Initialize views
        mListView = (ListView) mRootView.findViewById(android.R.id.list);
        mProgressBar = (ProgressBar) mRootView.findViewById(android.R.id.progress);

        loadFeed();
        return mRootView;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        FeedItem feedItem = (FeedItem) l.getItemAtPosition(position);
        Intent intent = new Intent(getActivity(), WebViewActivity.class);
        intent.setData(Uri.parse(feedItem.getUrl()));
        startActivity(intent);
    }

    private void refreshFeed() {
        showProgressBar();
        String subReddit = SectionsPagerAdapter.SECTIONS[mSubRedditPosition];
        Log.v(LOG_TAG, "Active subReddit: " + subReddit);
        FetchFeedTask fetchFeedTask = new FetchFeedTask(subReddit);
        fetchFeedTask.execute();
    }

    private void showProgressBar() {
        mProgressBar.setVisibility(View.VISIBLE);
        mListView.setVisibility(View.GONE);
    }

    private void hideProgressBar() {
        mListView.setVisibility(View.VISIBLE);
        mProgressBar.setVisibility(View.GONE);
    }

    private void loadFeed() {
        String subReddit = SectionsPagerAdapter.SECTIONS[mSubRedditPosition];
        Cursor feedCursor = mDBHelper.fetchSubRedditPosts(subReddit);

        if (feedCursor.getCount() == 0) {
            refreshFeed();
        } else {
            Log.v(LOG_TAG, "There are items!");
            // Call populateListView with the items, somehow.
            ArrayList<FeedItem> itemsList = new ArrayList<FeedItem>();

            String title, url, domain, id, author, score, thumbnail, permalink, created, numComments;
            int i = 0;
            while(feedCursor.moveToNext()) {
                title = feedCursor.getString(feedCursor.getColumnIndex(FeedDBAdapter.COLUMN_TITLE));
                url = feedCursor.getString(feedCursor.getColumnIndex(FeedDBAdapter.COLUMN_URL));
                domain = feedCursor.getString(feedCursor.getColumnIndex(FeedDBAdapter.COLUMN_DOMAIN));
                id = feedCursor.getString(feedCursor.getColumnIndex(FeedDBAdapter.COLUMN_REDDIT_ID));
                author = feedCursor.getString(feedCursor.getColumnIndex(FeedDBAdapter.COLUMN_AUTHOR));
                score = feedCursor.getString(feedCursor.getColumnIndex(FeedDBAdapter.COLUMN_SCORE));
                thumbnail = feedCursor.getString(feedCursor.getColumnIndex(FeedDBAdapter.COLUMN_THUMBNAIL));
                permalink = feedCursor.getString(feedCursor.getColumnIndex(FeedDBAdapter.COLUMN_PERMALINK));
                created = feedCursor.getString(feedCursor.getColumnIndex(FeedDBAdapter.COLUMN_CREATED));
                numComments = feedCursor.getString(feedCursor.getColumnIndex(FeedDBAdapter.COLUMN_NUM_COMMENTS));

                Log.v(LOG_TAG, title + ", " + url + ", " + subReddit);
                itemsList.add(i, new FeedItem(
                        title, url, subReddit, domain, id, author,
                        score, thumbnail, permalink, created, numComments));
                i++;
            }
            feedCursor.close();

            Log.v(LOG_TAG, "List length: " + itemsList.size());
            if (itemsList.isEmpty() == false) {
                populateListView(itemsList);
            }
        }
    }

    public void populateListView(final ArrayList<FeedItem> feed) {
        hideProgressBar();

        ArrayAdapter<FeedItem> feedAdapter = new ArrayAdapter<FeedItem> (
                getActivity(),
                R.layout.post_item,
                R.id.item_title,
                feed)
        {
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                FeedItem feedItem = feed.get(position);

                ((TextView) view.findViewById(R.id.item_title)).setText(feedItem.getTitle());
                ((TextView) view.findViewById(R.id.item_url)).setText(feedItem.getDomain());
                ((TextView) view.findViewById(R.id.item_score)).setText(feedItem.getScore() + " points | ");
                ((TextView) view.findViewById(R.id.item_author)).setText(" by " + feedItem.getAuthor() + " | ");
                ((TextView) view.findViewById(R.id.item_num_comments)).setText(feedItem.getNumComments() + " comments");

                return view;
            }
        };

        try {
            mListView.setAdapter(feedAdapter);
        } catch (NullPointerException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

    /**
     * Asynctask for making a call to the API.
     */
    private class FetchFeedTask extends AsyncTask<Object, Void, JSONObject> {

        private String mSubReddit;

        private final String LOG_TAG = FetchFeedTask.class.getSimpleName();
        private final String KEY_TITLE = "title";
        private final String KEY_URL = "url";
        private final String KEY_DOMAIN = "domain";
        private final String KEY_REDDIT_ID = "id";
        private final String KEY_AUTHOR = "author";
        private final String KEY_SCORE = "score";
        private final String KEY_THUMBNAIL = "thumbnail";
        private final String KEY_PERMALINK = "permalink";
        private final String KEY_CREATED = "created";
        private final String KEY_NUM_COMMENTS = "num_comments";


        public FetchFeedTask(String subReddit) {
            mSubReddit = subReddit;
        }

        @Override
        protected JSONObject doInBackground(Object[] params) {
            JSONObject jsonResponse = null;
            StringBuilder builder = new StringBuilder();
            HttpClient client = new DefaultHttpClient();
            int responseCode;

            String baseUrl = "http://www.reddit.com/r/" + mSubReddit + ".json";

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
            String title, url, domain, id, author, score, thumbnail, permalink, created, numComments;

            ArrayList<FeedItem> items = new ArrayList<FeedItem>();

            if (jsonResponse != null) {
                try {
                    jsonFeed = jsonResponse.getJSONObject("data").getJSONArray("children");
                    for (int i = 0; i < jsonFeed.length(); i++) {
                        postData = jsonFeed.getJSONObject(i).getJSONObject("data");
                        title = Html.fromHtml(postData.getString(KEY_TITLE)).toString();
                        url = postData.getString(KEY_URL);
                        domain = postData.getString(KEY_DOMAIN);
                        id = postData.getString(KEY_REDDIT_ID);
                        author = postData.getString(KEY_AUTHOR);
                        score = postData.getString(KEY_SCORE);
                        thumbnail = postData.getString(KEY_THUMBNAIL);
                        permalink = postData.getString(KEY_PERMALINK);
                        created = postData.getString(KEY_CREATED);
                        numComments = postData.getString(KEY_NUM_COMMENTS);

                        items.add(new FeedItem(
                                title, url, mSubReddit, domain, id, author,
                                score, thumbnail, permalink, created, numComments));
                    }

                    Log.v(LOG_TAG, "Deleting existing posts");
                    mDBHelper.deleteAllSubredditPosts(mSubReddit);

                    Log.v(LOG_TAG, "Creating new posts");
                    mDBHelper.createPosts(items);

                    Log.v(LOG_TAG, "Populating the list view");
                    populateListView(items);

                } catch (JSONException e) {
                    Log.e(LOG_TAG, e.getMessage());
                }
            }
        }
    }
}

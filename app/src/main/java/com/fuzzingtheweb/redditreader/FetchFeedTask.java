package com.fuzzingtheweb.redditreader;

import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;

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
 * Asynctask for making a call to the API.
 */
public class FetchFeedTask extends AsyncTask<Object, Void, JSONObject> {

    private String mSubReddit;
    private FeedDBAdapter mDBHelper;
    private FeedFragment mContext;

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


    public FetchFeedTask(String subReddit, FeedDBAdapter dBHelper, FeedFragment context) {
        mSubReddit = subReddit;
        mDBHelper = dBHelper;
        mContext = context;
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
                mContext.populateListView(items);

            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }
    }
}

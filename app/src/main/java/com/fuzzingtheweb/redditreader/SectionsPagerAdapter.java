package com.fuzzingtheweb.redditreader;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.support.v13.app.FragmentPagerAdapter;

import java.util.Locale;

/**
 * A {@link android.support.v13.app.FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {

    public final static String[] SECTIONS = {
            "all", "gifs", "django", "python", "android"
    };
    private final static int NUM_TABS = SECTIONS.length;

    public SectionsPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
    }

    @Override
    public int getCount() {
        return NUM_TABS;
    }

    @Override
    public Fragment getItem(int position) {
        return FeedFragment.newInstance(position, SECTIONS[position]);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return SECTIONS[position];
    }
}

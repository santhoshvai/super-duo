package barqsoft.footballscores;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.view.View;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Created by yehya khaled on 3/3/2015.
 */
public class Utilities
{
    public static String getLeague(Context context, int league_num)
    {

        // get league codes and label from resources
        int[] leagueCodes = context.getResources().getIntArray(R.array.league_codes);
        String[] leagueLabels = context.getResources().getStringArray(R.array.league_labels);

        // find the position of the league code and we get the league label (same index)
        for(int i=0; i < leagueCodes.length; i++) {
            if (leagueCodes[i] == league_num) {
                return leagueLabels[i];
            }
        }

        return  "Not known League Please report got: " + league_num;
    }
    public static String getMatchDay(Context context, int match_day, int league_num) {

        // use an algoritm for the champions league
        if (league_num == R.integer.league_champions_league_code) {
            if (match_day <= 6) {
                return context.getString(R.string.group_stage_text) +", "+
                        context.getString(R.string.matchday_text) +": "+ String.valueOf(match_day);
            } else if (match_day == 7 || match_day == 8) {
                return context.getString(R.string.first_knockout_round);
            } else if (match_day == 9 || match_day == 10) {
                return context.getString(R.string.quarter_final);
            } else if (match_day == 11 || match_day == 12) {
                return context.getString(R.string.semi_final);
            } else {
                return context.getString(R.string.final_text);
            }
        } else {
            // else just return the match day
            return context.getString(R.string.matchday_text) +": " + String.valueOf(match_day);
        }
    }

    public static String getScores(String home_goals, String awaygoals)
    {
        if (home_goals.equals("null") || awaygoals.equals("null"))
        {
            return " - ";
        }
        else
        {
            return home_goals + " - " + awaygoals;
        }
    }
    /**
     * Check if the network is available
     * @return boolean
     */
    public static boolean isNetworkAvailable(Context context) {

        // get the connectivity manager service
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        // get info about the network
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();

        // return true if we have networkinfo and are connected
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            return true;
        }  else {
            return false;
        }
    }

    public static boolean isRtl(Context context) {
        boolean rtl = false;

        // check the direction depending on the api level the device is running
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {

            // if we are running on API 17 or higher we can use getlayoutdirection
            Configuration config = context.getResources().getConfiguration();
            if(config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                rtl = true;
            }
        } else {

            // else we have to base it on the locale setting and a collection of rtl language codes
            Set<String> lang = new HashSet<String>();
            lang.add("ar");
            lang.add("dv");
            lang.add("fa");
            lang.add("ha");
            lang.add("he");
            lang.add("iw");
            lang.add("ji");
            lang.add("ps");
            lang.add("ur");
            lang.add("yi");

            // get the users' locale
            Locale locale = Locale.getDefault();

            // check if the rtl collection contains the local language setting
            rtl = lang.contains(locale.getLanguage());
        }

        return rtl;
    }

    /**
     * Helper function to check if an int exists in an int array
     * @param array int[]
     * @param key int
     * @return boolean
     */
    public static boolean contains(final int[] array, final int key) {

        // loop and return true when found
        for (final int i : array) {
            if (i == key) {
                return true;
            }
        }

        return false;
    }

    /**
     * Show a toast for a short while
     * @param text String
     */
    public static Toast showToast(Context context, String text) {

        // set the toast text and show it for a short moment
        Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
        toast.show();

        return toast;
    }

    /**
     * Scale a bitmap and return the scaled version
     * @param context Context
     * @param bitmap Bitmal
     * @param height int
     * @return Bitmap
     */
    public static Bitmap scaleBitmap(Context context, Bitmap bitmap, int height) {

        // get the device density
        final float densityMultiplier = context.getResources().getDisplayMetrics().density;

        // calculate the height, and relative width
        int newHeight = (int) (height * densityMultiplier);
        int newWidth = (int) (newHeight * bitmap.getWidth() / ((double) bitmap.getHeight()));

        // scale the bitmap
        bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

        return bitmap;
    }

}

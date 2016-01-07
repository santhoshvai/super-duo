package barqsoft.footballscores.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import barqsoft.footballscores.DatabaseContract;
import barqsoft.footballscores.R;
import barqsoft.footballscores.Utilities;

/**
 * Created by yehya khaled on 3/2/2015.
 */
public class myFetchService extends IntentService
{
    public static final String LOG_TAG = myFetchService.class.getSimpleName();

    // leagues we want to include
    private int[] mLeagueCodes;
    private HashMap<String, String> mLogoUrlMap = new HashMap<>();


    public myFetchService()
    {
        super("myFetchService");
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        // get selected leagues from resources array
        mLeagueCodes = getResources().getIntArray(R.array.leagues_selected);
        // load the team logos
        loadTeams();
        getData("p2");
        getData("n3");

        return;
    }

    private void getData (String timeFrame)
    {
        //Creating fetch URL
        final String BASE_URL = "http://api.football-data.org/v1/fixtures"; //Base URL
        final String QUERY_TIME_FRAME = "timeFrame"; //Time Frame parameter to determine days
        //final String QUERY_MATCH_DAY = "matchday";

        Uri fetch_build = Uri.parse(BASE_URL).buildUpon().
                appendQueryParameter(QUERY_TIME_FRAME, timeFrame).build();
        Log.v(LOG_TAG, "The url we are looking at is: "+fetch_build.toString()); //log spam

        String JSON_data = null;
        try {
            JSON_data = queryFootballDataApi(new URL(fetch_build.toString()));
            if (JSON_data != null) {
                //This bit is to check if the data contains any matches. If not, we call processJson on the dummy data
                JSONArray matches = new JSONObject(JSON_data).getJSONArray("fixtures");
                if (matches.length() == 0) {
                    //if there is no data, call the function on dummy data
                    //this is expected behavior during the off season.
                    processJSONdata(getString(R.string.dummy_data), getApplicationContext(), false);
                    return;
                }

                processJSONdata(JSON_data, getApplicationContext(), true);
            } else {
                //Could not Connect
                Log.d(LOG_TAG, "Could not connect to server.");
            }
        }
        catch(Exception e)
        {
            Log.e(LOG_TAG,e.getMessage());
        }
    }
    private void processJSONdata (String JSONdata,Context mContext, boolean isReal)
    {
        //JSON data

        final String SEASON_LINK = "http://api.football-data.org/v1/soccerseasons/";
        final String MATCH_LINK = "http://api.football-data.org/v1/fixtures/";
        final String FIXTURES = "fixtures";
        final String LINKS = "_links";
        final String SOCCER_SEASON = "soccerseason";
        final String SELF = "self";
        final String MATCH_DATE = "date";
        final String HOME_TEAM = "homeTeamName";
        final String AWAY_TEAM = "awayTeamName";
        final String RESULT = "result";
        final String HOME_GOALS = "goalsHomeTeam";
        final String AWAY_GOALS = "goalsAwayTeam";
        final String MATCH_DAY = "matchday";



        try {
            JSONArray matches = new JSONObject(JSONdata).getJSONArray(FIXTURES);


            //ContentValues to be inserted
            Vector<ContentValues> values = new Vector <ContentValues> (matches.length());
            for(int i = 0;i < matches.length();i++)
            {
                //Match data
                String League = null;


                JSONObject match_data = matches.getJSONObject(i);
                League = match_data.getJSONObject(LINKS).getJSONObject(SOCCER_SEASON).
                        getString("href");
                League = League.replace(SEASON_LINK, "");
                boolean interestedLeague = Utilities.contains(mLeagueCodes, Integer.parseInt(League));
                //This if statement controls which leagues we're interested in the data from.
                //add leagues here in order to have them be added to the DB.
                // If you are finding no data in the app, check that this contains all the leagues.
                // If it doesn't, that can cause an empty DB, bypassing the dummy data routine.
                if(    interestedLeague     )
                {
                    String match_id = match_data.getJSONObject(LINKS).getJSONObject(SELF).
                            getString("href");
                    match_id = match_id.replace(MATCH_LINK, "");
                    if(!isReal){
                        //This if statement changes the match ID of the dummy data so that it all goes into the database
                        match_id=match_id+Integer.toString(i);
                    }

                    // get the date and time from match date field
                    String date = match_data.getString(MATCH_DATE);
                    String time = date.substring(date.indexOf("T") + 1, date.indexOf("Z"));
                    date = date.substring(0, date.indexOf("T"));
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-ddHH:mm:ss", Locale.US);
                    simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

                    // convert date and time to local datetime and extract date and time again
                    try {
                        Date parsedDate = simpleDateFormat.parse(date + time);
                        SimpleDateFormat newDate = new SimpleDateFormat("yyyy-MM-dd:HH:mm", Locale.US);
                        newDate.setTimeZone(TimeZone.getDefault());
                        date = newDate.format(parsedDate);
                        time = date.substring(date.indexOf(":") + 1);
                        date = date.substring(0, date.indexOf(":"));

                        // change the dummy data's date to match current date range
                        if(!isReal) {
                            Date dummyDate = new Date(System.currentTimeMillis() + ((i-2)*86400000));
                            SimpleDateFormat dummyDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                            date = dummyDateFormat.format(dummyDate);
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, e.getMessage());
                    }
                    String Home = match_data.getString(HOME_TEAM);

                    String Away = match_data.getString(AWAY_TEAM);

                    String Home_goals = match_data.getJSONObject(RESULT).getString(HOME_GOALS);
                    String Away_goals = match_data.getJSONObject(RESULT).getString(AWAY_GOALS);
                    String match_day = match_data.getString(MATCH_DAY);

                    ContentValues match_values = new ContentValues();
                    match_values.put(DatabaseContract.scores_table.MATCH_ID,match_id);
                    match_values.put(DatabaseContract.scores_table.DATE_COL,date);
                    match_values.put(DatabaseContract.scores_table.TIME_COL,time);
                    match_values.put(DatabaseContract.scores_table.HOME_COL,Home);
                    match_values.put(DatabaseContract.scores_table.HOME_LOGO_COL, mLogoUrlMap.get(Home));
                    match_values.put(DatabaseContract.scores_table.AWAY_COL,Away);
                    match_values.put(DatabaseContract.scores_table.AWAY_LOGO_COL, mLogoUrlMap.get(Away));
                    match_values.put(DatabaseContract.scores_table.HOME_GOALS_COL,Home_goals);
                    match_values.put(DatabaseContract.scores_table.AWAY_GOALS_COL,Away_goals);
                    match_values.put(DatabaseContract.scores_table.LEAGUE_COL,League);
                    match_values.put(DatabaseContract.scores_table.MATCH_DAY,match_day);
                    //log spam
                    //Log.v(LOG_TAG,crest_url);
                    //Log.v(LOG_TAG,match_id);
//                    Log.v(LOG_TAG,date);
//                    Log.e(LOG_TAG,time);
                    //Log.v(LOG_TAG,Home);
//                    Log.v(LOG_TAG,getTeamLogoUrl(home_team_url));
//                    Log.v(LOG_TAG,away_team_url);
                    //Log.v(LOG_TAG,Away);
//                    Log.v(LOG_TAG,Home_goals);
                    //Log.v(LOG_TAG,Away_goals);

                    values.add(match_values);
                }
            }
            int inserted_data = 0;
            ContentValues[] insert_data = new ContentValues[values.size()];
            values.toArray(insert_data);
            inserted_data = mContext.getContentResolver().bulkInsert(
                    DatabaseContract.BASE_CONTENT_URI,insert_data);

            //Log.v(LOG_TAG,"Succesfully Inserted : " + String.valueOf(inserted_data));
        }
        catch (JSONException e)
        {
            Log.e(LOG_TAG,e.getMessage());
        }

    }
    /**
     * Query the football-data.org api with given url and return the result as a string
     * @param fetch URL
     */
    private String queryFootballDataApi(URL fetch) {
        HttpURLConnection m_connection = null;
        BufferedReader reader = null;
        String JSON_data = null;
        //Opening Connection
        try {
            m_connection = (HttpURLConnection) fetch.openConnection();
            m_connection.setRequestMethod("GET");
            m_connection.addRequestProperty("X-Auth-Token", getString(R.string.api_key));
            m_connection.connect();

            // Read the input stream into a String
            InputStream inputStream = m_connection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return JSON_data;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }
            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return JSON_data;
            }
            JSON_data = buffer.toString();
        }
        catch (Exception e)
        {
            Log.e(LOG_TAG,"queryFootballDataApi: Exception here " + e.getMessage());
        }
        finally {
            if(m_connection != null)
            {
                m_connection.disconnect();
            }
            if (reader != null)
            {
                try {
                    reader.close();
                }
                catch (IOException e)
                {
                    Log.e(LOG_TAG,"queryFootballDataApi: Error Closing Stream");
                }
            }
        }
        return JSON_data;
    }

    /**
     * Load the team details for selected leagues and store the team id with the logo url as a list
     */
    private void loadTeams() {

        // for each league get the teams
        for (final int code : mLeagueCodes) {
            try {
                // construct api teams query url by adding the soccerseasons, leaguecode, and teams path
                URL queryTeamsUrl = new URL(Uri.parse("http://api.football-data.org/v1")
                        .buildUpon()
                        .appendPath("soccerseasons")
                                .appendPath(Integer.toString(code))
                                .appendPath("teams")
                                .build()
                                .toString());

                // query the api and get the teams
                String teams = queryFootballDataApi(queryTeamsUrl);

                // process the returned json data
                if (teams != null) {
                    processTeams(teams);
                } else {
                    Log.d(LOG_TAG, "Failed to load teams" +": "+ code);
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Exception here in loadTeams: " + e.getMessage());
            }
        }
    }

    /**
     * Convert loaded json teams data to list of team id and team logo url
     * @param teamsString String
     */
    private void processTeams(String teamsString) {

        // json element names
        final String TEAMS = "teams";
        final String LINKS = "_links";
        final String SELF = "self";
        final String NAME = "name";
        final String CREST_URL = "crestUrl";

        final String SELF_LINK = "http://api.football-data.org/v1/teams/";

        // get teams and add the id and logo url to the vector
        try {
            JSONArray teams = new JSONObject(teamsString).getJSONArray(TEAMS);

            if (teams.length() > 0) {
                for(int i = 0;i < teams.length();i++) {

                    // get the team
                    JSONObject team = teams.getJSONObject(i);

                    String teamName = team.getString(NAME);
                    // get the cresturl
                    String teamLogoUrl = team.getString(CREST_URL);

                    // convert .svg urls to .png urls
                    //  SVG original:   https://upload.wikimedia.org/wikipedia/de/d/d8/Heracles_Almelo.svg
                    //  PNG 200px:	    https://upload.wikimedia.org/wikipedia/de/thumb/d/d8/Heracles_Almelo.svg/200px-Heracles_Almelo.svg.png
                    if (teamLogoUrl != null && teamLogoUrl.endsWith(".svg")) {
                        String svgLogoUrl = teamLogoUrl;
                        String filename = svgLogoUrl.substring(svgLogoUrl.lastIndexOf("/") + 1);
                        int wikipediaPathEndPos = svgLogoUrl.indexOf("/wikipedia/") + 11;
                        String afterWikipediaPath = svgLogoUrl.substring(wikipediaPathEndPos);
                        int thumbInsertPos = wikipediaPathEndPos + afterWikipediaPath.indexOf("/") + 1;
                        String afterLanguageCodePath = svgLogoUrl.substring(thumbInsertPos);
                        teamLogoUrl = svgLogoUrl.substring(0, thumbInsertPos);
                        teamLogoUrl += "thumb/" + afterLanguageCodePath;
                        teamLogoUrl += "/200px-" + filename + ".png";
                    }

                    mLogoUrlMap.put(teamName, teamLogoUrl);
                }
            } else {
                Log.e(LOG_TAG, "No teams found");
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

}


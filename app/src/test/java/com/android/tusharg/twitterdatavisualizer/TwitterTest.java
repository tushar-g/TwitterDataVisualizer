package com.android.tusharg.twitterdatavisualizer;

import android.support.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ThreadFactory;

import twitter4j.FilterQuery;
import twitter4j.GeoLocation;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class TwitterTest {

    @Test
    public void tweets() {

        ConfigurationBuilder cb = getConfigurationBuilder();
        Twitter twitter = null;
        try {
            twitter = new TwitterFactory(cb.build()).getInstance();
            twitter.getOAuth2Token();
        } catch (TwitterException e) {
            e.printStackTrace();
        }

        try {
            GeoLocation geo = new GeoLocation(28.701076, 77.258704);
//            GeoLocation geo = new GeoLocation(28.4951066,77.089144);
            Query query = new Query();
            query.setGeoCode(geo, 1, Query.Unit.mi);
            query.setCount(100);
//            Query query = new Query("@G_tushar_");
//            QueryResult result = twitter.getUserTimeline("G_tushar_");
//            ArrayList<Status> tweets = (ArrayList<Status>) twitter.getUserTimeline("G_tushar_");
            QueryResult result;
            HashMap<String, Status> hm = new HashMap<>();
            ArrayList<Status> tweets;
//            do {
            result = twitter.search(query);
            tweets = (ArrayList<Status>) result.getTweets();
            System.out.println("Retrieved tweets: " + tweets.size()
                    + " with maxId : " + query.getMaxId()
                    + "Hashmap size is: " + hm.size());
            long smallestId = Long.MAX_VALUE;
            for (Status s : tweets) {
                if (s.getGeoLocation() != null) {
                    if (!hm.containsKey(s.getGeoLocation().toString())) {
                        hm.put(s.getGeoLocation().toString(), s);
                    }
                }
                smallestId = Math.min(smallestId, s.getId());
            }
            query.setMaxId(smallestId - 1);
//            } while (hm.size() < 100 && tweets.size() != 0);
            System.out.println("Size of tweets: " + tweets.size());

            System.out.println("Size of HashMap is: " + hm.size());
            for (String key : hm.keySet()) {
                Status s = hm.get(key);
                System.out.println("=========================================");
                System.out.println("@" + s.getUser().getScreenName() +
                        " - " + s.getCreatedAt() +
                        s.getGeoLocation().toString() +
                        " - " + s.getText());
            }


//            do {
//                List<twitter4j.Status> statuses = twitter.getUserTimeline("Twitter");
//                result = twitter.getUserTimeline();
//                List<Status> tweets = result.getTweets();
//                System.out.print("How many tweets :" + statuses.size());
//                for (Status tweet : tweets) {
//                    System.out.println("@" + tweet.getUser().getScreenName() + " - " + tweet.getText());
//                }
//
//            } while ((query = result.nextQuery()) != null);
        } catch (TwitterException te) {
            te.printStackTrace();
            System.out.println("Failed to search tweets: " + te.getMessage());
        }
    }

    @NonNull
    private ConfigurationBuilder getConfigurationBuilder() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
//        cb.setDebugEnabled(true);
//        cb.setApplicationOnlyAuthEnabled(true);

        cb.setOAuthConsumerKey(Constants.TWITTER_CONSUMER_KEY)
                .setOAuthConsumerSecret(Constants.TWITTER_CONSUMER_SECRET)
                .setOAuthAccessToken(Constants.TWITTER_ACCES_TOKEN)
                .setOAuthAccessTokenSecret(Constants.TWITTER_ACCES_TOKEN_SECRET);
        return cb;
    }

    /**
     * Test Stream API of twitter.
     */
    @Test
    public void tweetsStream() {
        ConfigurationBuilder cb = getConfigurationBuilder();
        TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();

        StatusListener listener = new StatusListener() {

            @Override
            public void onStatus(Status status) {
                //here you do whatever you want with the tweet
                System.out.println("onStatus: " + status.getText());

            }

            @Override
            public void onException(Exception ex) {
                ex.printStackTrace();
                System.out.println("onException: " + ex.toString());
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice arg0) {
                System.out.println("onDeletionNotice: " + arg0);
            }

            @Override
            public void onScrubGeo(long arg0, long arg1) {
                System.out.println("onScrubGeo: " + arg0);

            }

            @Override
            public void onStallWarning(StallWarning arg0) {
                // TODO Auto-generated method stub
                System.out.println("onStallWarning: " + arg0);
            }

            @Override
            public void onTrackLimitationNotice(int arg0) {
                // TODO Auto-generated method stub
                System.out.println("onTrackLimitationNotice: " + arg0);
            }
        };

        twitterStream.addListener(listener);
        FilterQuery filterQuery = new FilterQuery();
//        double[][] locations = {{-74,40}, {-73,41}}; //those are the boundary from New York City
        double[][] locations = {{20, 70}, {30, 80}};
        filterQuery.locations(locations);
        twitterStream.filter(filterQuery);
    }

    /**
     * This is to check the Json limitation of not accepting an int greater than 53 bits.
     */
    @Test
    public void jsonTweetIdCheck() {
        JsonDataObject j = null;
        try {
            j = new Gson().fromJson(jsonSampleData(), new TypeToken<JsonDataObject>() {
            }.getType());
            System.out.println("Parsed Values are:");
            System.out.println("id_str : " + j.getId_str());
            System.out.println("id : " + j.getId());
        } catch (Exception e) {
            System.out.println("Caught an exception : " + e.toString());
        }
    }

    /**
     * Sample data taken from https://dev.twitter.com/overview/api/twitter-ids-json-and-snowflake
     *
     * @return json string
     */
    private String jsonSampleData() {
        return "  {\n" +
                "    \"coordinates\": null,\n" +
                "    \"truncated\": false,\n" +
                "    \"created_at\": \"Thu Oct 14 22:20:15 +0000 2010\",\n" +
                "    \"favorited\": false,\n" +
                "    \"entities\": {\n" +
                "      \"urls\": [\n" +
                "      ],\n" +
                "      \"hashtags\": [\n" +
                "      ],\n" +
                "      \"user_mentions\": [\n" +
                "        {\n" +
                "          \"name\": \"Matt Harris\",\n" +
                "          \"id\": 777925,\n" +
                "          \"id_str\": \"777925\",\n" +
                "          \"indices\": [\n" +
                "            0,\n" +
                "            14\n" +
                "          ],\n" +
                "          \"screen_name\": \"themattharris\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"text\": \"@themattharris hey how are things?\",\n" +
                "    \"annotations\": null,\n" +
                "    \"contributors\": [\n" +
                "      {\n" +
                "        \"id\": 819797,\n" +
                "        \"id_str\": \"819797\",\n" +
                "        \"screen_name\": \"episod\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"id\": 12738165059,\n" +
                "    \"id_str\": \"12738165059\",\n" +
                "    \"retweet_count\": 0,\n" +
                "    \"geo\": null,\n" +
                "    \"retweeted\": false,\n" +
                "    \"in_reply_to_user_id\": 777925,\n" +
                "    \"in_reply_to_user_id_str\": \"777925\",\n" +
                "    \"in_reply_to_screen_name\": \"themattharris\",\n" +
                "    \"user\": {\n" +
                "      \"id\": 6253282,\n" +
                "      \"id_str\": \"6253282\"\n" +
                "    },\n" +
                "    \"source\": \"web\",\n" +
                "    \"place\": null,\n" +
                "    \"in_reply_to_status_id\": 12738040524,\n" +
                "    \"in_reply_to_status_id_str\": \"12738040524\"\n" +
                "  }";
    }

    /**
     * Model object to get values from sample json.
     */
    static class JsonDataObject {
        int id;

        String id_str;

        public String getId_str() {
            return id_str;
        }

        public void setId_str(String id_str) {
            this.id_str = id_str;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    }


    @Test
    public void streamTest() {
        ConfigurationBuilder cb = getConfigurationBuilder();
        TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
        StatusListener listener = new StatusListener() {
            @Override
            public void onStatus(Status status) {
                System.out.println("@" + status.getUser().getScreenName() + " - " + status.getText());
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
                System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
            }

            @Override
            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
                System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
            }

            @Override
            public void onScrubGeo(long userId, long upToStatusId) {
                System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
            }

            @Override
            public void onStallWarning(StallWarning warning) {
                System.out.println("Got stall warning:" + warning);
            }

            @Override
            public void onException(Exception ex) {
                ex.printStackTrace();
                System.out.println(ex.toString());
            }
        };
        twitterStream.addListener(listener);
        twitterStream.sample();
        int count = 0;
        while (count < 5) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count++;
        }
    }

}
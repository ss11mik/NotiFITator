/*
 * This file is part of the NotiFITator distribution (https://github.com/ss11mik/NotiFITator).
 *  Copyright (c) 2020 Ondřej Mikula.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, version 3.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package cz.webz.ss11mik.notifitator.backend;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;
import androidx.preference.PreferenceManager;
import androidx.room.Room;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import cz.webz.ss11mik.notifitator.R;
import cz.webz.ss11mik.notifitator.containers.Course;
import cz.webz.ss11mik.notifitator.containers.Event;
import okhttp3.Authenticator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class Repository {

    private final MyDao dao;

    private String url;

    private List<Integer> courseColors;
    private int lastGeneratedColor = 0;

    public Repository(Context context) {
        dao = Room.databaseBuilder(context, Database.class, "events.db")
                .fallbackToDestructiveMigration()
                .build()
                .eventDao();

        url = context.getString(R.string.url_wis);

        initCourseColors(context);
    }



    private static final TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }
    };
    private static final SSLContext trustAllSslContext;
    static {
        try {
            trustAllSslContext = SSLContext.getInstance("SSL");
            trustAllSslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }
    private static final SSLSocketFactory trustAllSslSocketFactory = trustAllSslContext.getSocketFactory();


    private String getCredentials (Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            SharedPreferences encryptedPreferences = EncryptedSharedPreferences.create(
                    "login",
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            SharedPreferences sharedPreferences = PreferenceManager
                    .getDefaultSharedPreferences(context);

            String username = sharedPreferences.getString("username", "");
            String password = encryptedPreferences.getString("password", "");

            return Credentials.basic(username, password);
        }
        catch (IOException | GeneralSecurityException e) {
            return "";
        }
    }


    public void fetch(final FetchCallback callback, final Context context) {

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        builder.authenticator(new Authenticator() {
            @Override
            public Request authenticate(Route route, Response response) {
                if (responseCount(response) >= 3) {
                    return null;
                }
                return response.request().newBuilder().header(
                        "Authorization",
                        getCredentials(context))
                        .build();
            }
        });

        builder.sslSocketFactory(trustAllSslSocketFactory, (X509TrustManager)trustAllCerts[0]);
        builder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });

        Request request = new Request.Builder()
                .url(url)
                .build();

        OkHttpClient client = builder.build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                callback.fetched(false);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.code() == 200) {
                    callback.fetched(true);

                    List<Event> events = parseNews(Jsoup.parse(response.body().string()));

                    Set<String> courseNames = new LinkedHashSet<>();
                    for (Event event : events) {
                        courseNames.add(event.getCourse());
                    }
                    for (String courseName : courseNames) {
                        insertCourse(new Course(courseName, getRandomColor()));
                    }

                    dao.clearEvents();
                    dao.insertEvents(events);
                }
                else
                    callback.fetched(false);
            }
        });

    }


    private List<Event> parseNews(Document document) {
        List<Event> events = new ArrayList<>();

        Element table = document.getElementsByClass("stbl").first().child(0);

        table.getElementsByTag("tr").first().remove();  //header

        for (Element row : table.getElementsByTag("tr")) {
            Event event = new Event();
            event.setDate(row.child(0).text());
            event.setCourse(row.child(1).text());
            event.setType(row.child(2).text());
            event.setText(row.child(3).text());
            event.makeId();
            events.add(event);
        }

        return events;
    }



    int getRandomColor() {
        if (lastGeneratedColor >= courseColors.size())
            lastGeneratedColor = 0;
        return courseColors.get(lastGeneratedColor++);
    }

    void initCourseColors (Context context) {
        courseColors = new ArrayList<>();

        courseColors.add(context.getColor(R.color.course0));
        courseColors.add(context.getColor(R.color.course1));
        courseColors.add(context.getColor(R.color.course2));
        courseColors.add(context.getColor(R.color.course3));
        courseColors.add(context.getColor(R.color.course4));
        courseColors.add(context.getColor(R.color.course5));
        courseColors.add(context.getColor(R.color.course6));
        courseColors.add(context.getColor(R.color.course7));

        Collections.shuffle(courseColors);
    }




    private int responseCount(Response response) {
        int result = 1;
        while ((response = response.priorResponse()) != null) {
            result++;
        }
        return result;
    }

    public interface FetchCallback {
        void fetched(boolean success);
    }



    public LiveData<List<Event>> getEvents() {
        return dao.getEvents();
    }

    public List<Event> getTodayEvents() {
        Calendar midnight = new GregorianCalendar() {{
            set(Calendar.HOUR_OF_DAY, 0);
            set(Calendar.MINUTE, 0);
            set(Calendar.SECOND, 0);
            set(Calendar.MILLISECOND, 0);
        }};
        long start = midnight.getTimeInMillis();

        Calendar tomorrowMidnight = new GregorianCalendar() {{
            add(Calendar.DAY_OF_YEAR, 1);
            set(Calendar.HOUR_OF_DAY, 0);
            set(Calendar.MINUTE, 0);
            set(Calendar.SECOND, 0);
            set(Calendar.MILLISECOND, 0);
        }};
        long end = tomorrowMidnight.getTimeInMillis() - 1;

        return dao.getEventsFromRange(start, end);
    }

    public List<Event> getTomorrowEvents() {

        Calendar tomorrowMidnight = new GregorianCalendar() {{
            add(Calendar.DAY_OF_YEAR, 1);
            set(Calendar.HOUR_OF_DAY, 0);
            set(Calendar.MINUTE, 0);
            set(Calendar.SECOND, 0);
            set(Calendar.MILLISECOND, 0);
        }};
        long start = tomorrowMidnight.getTimeInMillis();

        Calendar nextMidnight = new GregorianCalendar() {{
            add(Calendar.DAY_OF_YEAR, 2);
            set(Calendar.HOUR_OF_DAY, 0);
            set(Calendar.MINUTE, 0);
            set(Calendar.SECOND, 0);
            set(Calendar.MILLISECOND, 0);
        }};
        long end = nextMidnight.getTimeInMillis() - 1;

        return dao.getEventsFromRange(start, end);
    }



    public void insertCourse(final Course course) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                dao.insertCourse(course);
            }
        });
    }

    public LiveData<Course> getCourseByName(String name) {
        return dao.getCourseByName(name);
    }
}

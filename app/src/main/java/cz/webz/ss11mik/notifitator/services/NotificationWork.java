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

package cz.webz.ss11mik.notifitator.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.preference.PreferenceManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import cz.webz.ss11mik.notifitator.R;
import cz.webz.ss11mik.notifitator.activities.MainActivity;
import cz.webz.ss11mik.notifitator.backend.Repository;
import cz.webz.ss11mik.notifitator.containers.Event;

public class NotificationWork extends Worker {

    private static final String NAME = "NotificationWork";

    private Context context;

    public NotificationWork(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        final Repository repository = new Repository(context);

        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(context);

        boolean notifyForToday = preferences.getBoolean("notif_that_day", false);
        boolean notifyForTomorrow = preferences.getBoolean("notif_day_before", false);

        if (notifyForToday) {
            List<Event> todaysEvents = repository.getTodayEvents();
            for (Event event : todaysEvents)
                sendNotification(event, context, true);
        }
        if (notifyForTomorrow) {
            List<Event> tomorrowsEvents = repository.getTomorrowEvents();
            for (Event event : tomorrowsEvents)
                sendNotification(event, context, false);
        }


        return Result.success();
    }

    public static void scheduleNotificationWork(Context context, long notificationTime) {

        WorkManager manager = WorkManager.getInstance(context);
        manager.cancelAllWorkByTag(NAME);
        manager.cancelAllWork();
        manager.pruneWork();

        PeriodicWorkRequest.Builder workBuilder =
                new PeriodicWorkRequest.Builder(
                        NotificationWork.class,
                        1,
                        TimeUnit.DAYS);

        workBuilder.setInitialDelay(getInitialDelay(notificationTime), TimeUnit.MILLISECONDS);

        PeriodicWorkRequest work = workBuilder.build();

        manager.enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                work);
    }

    private static long getInitialDelay (long notificationTime) {
        Date time = new Date(notificationTime);
        int hour = time.getHours();
        int minute = time.getMinutes();

        Calendar calendar = Calendar.getInstance();

        if(calendar.get(Calendar.HOUR_OF_DAY) > hour ||
                (calendar.get(Calendar.HOUR_OF_DAY) == hour && calendar.get(Calendar.MINUTE) + 1 >= minute)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis() - System.currentTimeMillis();
    }


    private static void sendNotification(Event event, Context context, boolean today) {

        int NOTIFICATION_ID = (int) System.currentTimeMillis() << 8 | new Random().nextInt(255);
        String CHANNEL_ID = "NotiFITator";
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "NotiFITator",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("channel for events from WIS");
            channel.setShowBadge(true);
            notificationManager.createNotificationChannel(channel);
        }

        String day = today? context.getString(R.string.today) : context.getString(R.string.tomorrow);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(today? R.drawable.ic_today : R.drawable.ic_tomorrow)
                .setContentTitle(day + " " + event.getType() + ": " + event.getCourse())
                .setCategory(Notification.CATEGORY_EVENT)
                .setContentText(event.getText());

        Intent resultIntent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}

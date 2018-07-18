package com.linminitools.mysync;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.TimePicker;

import java.net.URI;
import java.util.Calendar;

import javax.xml.transform.URIResolver;

import static android.content.Context.MODE_PRIVATE;
import static android.support.v4.app.NotificationCompat.*;
import static com.linminitools.mysync.MainActivity.configs;
import static com.linminitools.mysync.MainActivity.schedulers;
import static com.linminitools.mysync.MainActivity.settings;


public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent i) {

        SharedPreferences sched_prefs = ctx.getSharedPreferences("schedulers", MODE_PRIVATE);
        SharedPreferences config_prefs = ctx.getSharedPreferences("configs", MODE_PRIVATE);
        //SharedPreferences settings_prefs = ctx.getSharedPreferences("settings",MODE_PRIVATE);

        SharedPreferences set_prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        configs.clear();
        schedulers.clear();
        //settings.put("Notifications",settings_prefs.getBoolean("Notifications",true));

        for(int c=1; c<100;c++){
            if (config_prefs.getString("rs_ip_"+String.valueOf(c),"").isEmpty()){
                break;
            }
            else {
                RS_Configuration config = new RS_Configuration(c);
                config.rs_user=config_prefs.getString("rs_user_"+String.valueOf(c),"");
                config.rs_ip = config_prefs.getString("rs_ip_"+String.valueOf(c), "");
                config.rs_port = config_prefs.getString("rs_port_"+String.valueOf(c), "");
                config.rs_options = config_prefs.getString("rs_options_"+String.valueOf(c),"");
                config.rs_module = config_prefs.getString("rs_module_"+String.valueOf(c), "");
                config.local_path = config_prefs.getString("local_path_"+String.valueOf(c), "");
                configs.add(config);
            }
        }


        for (int c = 1; c < 100; c++) {
            if (sched_prefs.getInt("id_" + String.valueOf(c), -1) < 0) {
                Log.d("SCHEDULERS", "GETINT=-1");
                break;
            } else {
                TimePicker tp = new TimePicker(ctx);
                tp.setIs24HourView(true);

                tp.setCurrentHour(sched_prefs.getInt("hour_" + String.valueOf(c), 0));
                tp.setCurrentMinute(sched_prefs.getInt("min_" + String.valueOf(c), 0));
                String days = sched_prefs.getString("days_" + String.valueOf(c), "");
                String name = sched_prefs.getString("name_" + String.valueOf(c), "");
                int config_pos = sched_prefs.getInt("config_pos_" + String.valueOf(c), 0);
                Scheduler sched = new Scheduler(days, tp, c);
                sched.name = name;
                sched.config_pos = config_pos;
                schedulers.add(sched);
            }
        }


        if ((i.getAction()).equals("android.intent.action.BOOT_COMPLETED" )) {
            for (Scheduler s : schedulers){
                s.setAlarm(ctx);
                }
        }

        if ((i.getAction()).equals("android.media.action.DISPLAY_NOTIFICATION")) {
            int pos = i.getIntExtra("config", 0);
            String message;
            try {
                RS_Configuration c = configs.get(pos);
                c.executeConfig(ctx);
                message = "Rsync configuration " + c.name + " started on " + Calendar.getInstance().getTime().toString();
                }catch (IndexOutOfBoundsException e){
                message = "Rsync Configuration Not Found! Job is Cancelled";
                }

            if (set_prefs.getBoolean("notifications",true)) {

                Uri ring_path=Uri.parse(set_prefs.getString("ringtone", RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI));
                Ringtone ring = RingtoneManager.getRingtone(ctx,ring_path);



                if (Build.VERSION.SDK_INT < 26) {
                    Builder not = new Builder(ctx);
                    not.setContentTitle("MYSYNC Status");
                    not.setSmallIcon(R.mipmap.ic_launcher);
                    not.setStyle(new BigTextStyle().bigText(message));
                    not.setContentText(message);
                    Notification n = not.build();

                    NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.notify(1, n);

                } else {
                    Builder not = new Builder(ctx, NotificationChannel.DEFAULT_CHANNEL_ID);

                    not.setContentTitle("MYSYNC Status");
                    not.setStyle(new BigTextStyle().bigText(message));
                    not.setContentText(message);
                    not.setSmallIcon(R.mipmap.ic_launcher);
                    Notification n = not.build();

                    NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.notify(1, n);


                }

                if (set_prefs.getBoolean("vibrate",true)){
                    Vibrator vib = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vib.vibrate(VibrationEffect.createOneShot(350,1));
                    }
                    else{
                        vib.vibrate(350);
                    }

                }
                Log.d("ALARMRECEIVE","ONE");
                ring.play();


            }

        }
    }

}



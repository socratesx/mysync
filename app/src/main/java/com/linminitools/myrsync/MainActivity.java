package com.linminitools.myrsync;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 1;
    @NonNull
    public static final ArrayList<RS_Configuration> configs = new ArrayList<>();
    @NonNull
    public static final ArrayList<Scheduler> schedulers = new ArrayList<>();
    public static Context appContext;
    private File log_file;


    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appContext=getApplicationContext();
        configs.clear();
        schedulers.clear();

        if (Build.VERSION.SDK_INT >= 23) if (!checkPermission()) requestPermission(); // Code for permission
        ViewPager viewPager = findViewById(R.id.pager);
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.refresh_adapter();

        viewPager.setAdapter(adapter);

        TabLayout tabLayout =  findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        Boolean is_first_run = getSharedPreferences("Install",MODE_PRIVATE).getBoolean("first_run",true);
        Log.d("ARCH", Arrays.toString(Build.SUPPORTED_ABIS));
        if (is_first_run) {
            getSharedPreferences("Install",MODE_PRIVATE).edit().putBoolean("first_run",false).apply();
            AssetManager AM = this.getAssets();

            try {

                String appFileDirectory = getFilesDir().getPath();
                String executableFilePath = appFileDirectory + "/rsync";

                File old_file = new File(executableFilePath);
                old_file.delete();

                getSharedPreferences("Install",MODE_PRIVATE).edit().putString("rsync_binary",executableFilePath).apply();

                String bin_path ="rsync_binary/armv7/rsync";

                for (String arch : Build.SUPPORTED_ABIS){
                    if (arch.contains("arm64")) bin_path="rsync_binary/armv8/rsync";
                }

                InputStream in = AM.open(bin_path, AssetManager.ACCESS_BUFFER);

                File rsync_executable = new File(executableFilePath);


                FileOutputStream fos = new FileOutputStream(rsync_executable);
                byte[] buffer = new byte[in.available()];

                in.read(buffer);
                in.close();

                fos.write(buffer);

                fos.close();
                in.close();

                rsync_executable.setExecutable(true);
                Log.d("RSYNC_EXEC",rsync_executable.getAbsolutePath());

            } catch (Exception e) {
                e.printStackTrace();
                Log.d("EXEC_EXception",e.toString());
            }
        }



        SharedPreferences config_prefs = appContext.getSharedPreferences("configs", MODE_PRIVATE);
        SharedPreferences sched_prefs = appContext.getSharedPreferences("schedulers", MODE_PRIVATE);
        //SharedPreferences settings_prefs = appContext.getSharedPreferences("settings",MODE_PRIVATE);
        SharedPreferences prefs = appContext.getSharedPreferences("Rsync_Command_build", MODE_PRIVATE);

        String log_path = prefs.getString("log", appContext.getApplicationInfo().dataDir + "/logfile.log");
        log_file = new File(log_path);
        try {
            log_file.createNewFile();
        }
        catch (IOException e){
            e.printStackTrace();
        }

        //settings.put("Notifications",settings_prefs.getBoolean("Notifications",true));
        //settings.put("Vibration",settings_prefs.getBoolean("Vibration",false));
        //settings.put("Sound",settings_prefs.getBoolean("Sound",true));

        Map<String,?> config_keys = config_prefs.getAll();
        for(Map.Entry<String,?> entry : config_keys.entrySet()){
            if (entry.getKey().contains("rs_id_")) {
                String id=String.valueOf(entry.getValue());

                RS_Configuration config = new RS_Configuration((Integer)entry.getValue());
                config.rs_user=config_prefs.getString("rs_user_"+id,"");
                config.rs_ip = config_prefs.getString("rs_ip_"+id, "");
                config.rs_port = config_prefs.getString("rs_port_"+id, "");
                config.rs_options = config_prefs.getString("rs_options_"+id,"");
                config.rs_module = config_prefs.getString("rs_module_"+id, "");
                config.local_path = config_prefs.getString("local_path_"+id, "");
                config.name = config_prefs.getString("rs_name_"+id, "");
                config.addedOn = config_prefs.getLong("rs_addedon_"+id,0);
                configs.add(config);
            }
        }

        Map<String,?> sched_keys = sched_prefs.getAll();
        for(Map.Entry<String,?> entry : sched_keys.entrySet()){
            if (entry.getKey().contains("id_")) {
                String id=String.valueOf(entry.getValue());

                TimePicker tp=new TimePicker(this);

                tp.setIs24HourView(true);
                tp.setCurrentHour(sched_prefs.getInt("hour_"+id,0));
                tp.setCurrentMinute(sched_prefs.getInt("min_"+id,0));
                String days = sched_prefs.getString("days_"+id,"");
                String name = sched_prefs.getString("name_"+id,"");
                int config_pos=sched_prefs.getInt("config_pos_"+id,0);

                Scheduler sched = new Scheduler(days,tp,(Integer) entry.getValue());
                sched.name=name;
                sched.addedOn = sched_prefs.getLong("addedon",0);
                sched.config_pos=config_pos;
                schedulers.add(sched);
            }
        }
        Collections.sort(schedulers);
        Collections.sort(configs);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.app_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.me_exit) {
            this.finishAffinity();
            return true;
        }

        if (id == R.id.me_about) {
            SpannableString s = new SpannableString(getResources().getString(R.string.about));
            Linkify.addLinks(s,Linkify.ALL);
            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(this)
                            .setTitle("About")
                            .setMessage(s)
                            .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            ;
            AlertDialog alertDialog = alertDialogBuilder.show();
            TextView textView = alertDialog.findViewById(android.R.id.message);

            textView.setTextSize(12);
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }

        if (id == R.id.me_help) {
            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(this)
                            .setTitle("Help")
                            .setMessage(getResources().getString(R.string.help))
                            .setPositiveButton("Close", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                    ;
            alertDialogBuilder.show();
        }

        if (id == R.id.me_settings) {

            Intent i = new Intent(this,Settings.class);

            startActivity(i);

        }


        return super.onOptionsItemSelected(item);
    }



    public void createConfig(View v){

        Intent i = new Intent(this,addConfig.class);
        startActivityForResult(i,1);

        }

    public void createScheduler(View v){

        Intent i = new Intent(this,addScheduler.class);
        startActivityForResult(i,2);

    }




    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 41) {

            try {

                Uri pathUri = data.getData();
                Uri dirUri = DocumentsContract.buildDocumentUriUsingTree(pathUri, DocumentsContract.getTreeDocumentId(pathUri));
                DocumentFile pickedDir= DocumentFile.fromTreeUri(appContext,dirUri);

                DocumentFile previous_log = Objects.requireNonNull(pickedDir).findFile("logfile.txt");
                if (previous_log!=null) previous_log.delete();


                DocumentFile exported_log = pickedDir.createFile("text/plain","logfile.txt");
                FileInputStream inputStream = new FileInputStream(log_file);
                OutputStream outputStream = getContentResolver().openOutputStream(Objects.requireNonNull(exported_log).getUri());

                byte[] buffer = new byte[inputStream.available()];
                //noinspection ResultOfMethodCallIgnored
                inputStream.read(buffer);
                inputStream.close();

                Objects.requireNonNull(outputStream).write(buffer);
                outputStream.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {

            ViewPager viewPager = findViewById(R.id.pager);
            ViewPagerAdapter v = (ViewPagerAdapter) viewPager.getAdapter();
            Objects.requireNonNull(v).refresh_adapter();


            viewPager.setAdapter(v);
            viewPager.setCurrentItem(requestCode);

            TabLayout tabLayout = findViewById(R.id.tabs);
            tabLayout.setupWithViewPager(viewPager);

        }
    }



    public static String getPath(@NonNull final Context context, @NonNull final Uri uri) {

        // DocumentProvider
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                StringBuilder path;
                if ("primary".equalsIgnoreCase(type)) {
                    path = new StringBuilder(Environment.getExternalStorageDirectory().toString());

                    for (int i=0;i<split.length;i++){
                        if (i>0) path.append("/").append(split[i]);
                    }
                    //return Environment.getExternalStorageDirectory() + "/" + split[1];
                    return path.toString();
                }

                else {

                    path = new StringBuilder("/storage");
                    for (String aSplit : split) {
                        path.append("/").append(aSplit);
                    }
                    return path.toString();
                }

            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                switch (type) {
                    case "image":
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        break;
                    case "video":
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        break;
                    case "audio":
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        break;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, Objects.requireNonNull(contentUri), selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    private static String getDataColumn(Context context, @NonNull Uri uri, String selection,
                                        String[] selectionArgs) {

        final String column = "_data";
        final String[] projection = {
                column
        };

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(MainActivity.this, "Write External Storage permission allows to export logfile to your external storage. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("value", "Permission Granted, Now you can use local drive .");
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
        }
    }

    public void config_clickHandler(@NonNull View v){

        if (v.getTag(R.id.bt_edit)!=null) {
            int pos = (int) v.getTag(R.id.bt_edit);
            Intent intent = new Intent(this,editConfig.class);
            intent.putExtra("pos",pos);
            startActivity(intent);
        }
        else if (v.getTag(R.id.bt_delete)!=null) {
            final int pos = (int) v.getTag(R.id.bt_delete);

            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(this)
                            .setTitle("Delete Configuration")
                            .setMessage("This action will delete this configuration and all its schedulers. Are you sure?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    configs.get(pos).deleteFromDisk();
                                    configs.remove(pos);

                                    ArrayList<Scheduler> connected_schedulers_to_deleted_configuration=new ArrayList<>();
                                    for (Scheduler s : schedulers){
                                        if (pos==s.config_pos){
                                            connected_schedulers_to_deleted_configuration.add(s);
                                            s.deleteFromDisk();
                                        }
                                    }
                                    schedulers.removeAll(connected_schedulers_to_deleted_configuration);
                                    onActivityResult(1,RESULT_OK,null);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(@NonNull DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
            alertDialogBuilder.show();


        }


    }

    public void sched_clickHandler(@NonNull View v){

        if (v.getTag(R.id.bt_edit_sched)!=null) {
            int pos = (int) v.getTag(R.id.bt_edit_sched);
            Intent intent = new Intent(this,editSched.class);
            intent.putExtra("pos",pos);
            startActivityForResult(intent,2);

        }
        else if (v.getTag(R.id.bt_delete_sched)!=null) {
            final int pos = (int) v.getTag(R.id.bt_delete_sched);

            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(this)
                            .setTitle("Delete Scheduler")
                            .setMessage("Are you sure?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    schedulers.get(pos).deleteFromDisk();
                                    schedulers.get(pos).cancelAlarm(appContext);
                                    schedulers.remove(pos);
                                    onActivityResult(2,RESULT_OK,null);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(@NonNull DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
            alertDialogBuilder.show();


        }

    }


}
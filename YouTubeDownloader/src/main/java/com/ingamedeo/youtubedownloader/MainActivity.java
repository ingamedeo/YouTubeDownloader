package com.ingamedeo.youtubedownloader;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

//Version: 3.0_dev - TO DO

/*
  videoid is NULL >.DONE.<
  DonateActivity screen
  SwitchPreference >.DONE.<
  Orientation fixed

  AUTOUPDATE Not yet

  Load Data when come back from settings >.DONE.<

 */

public class MainActivity extends ActionBarActivity {

    private static final int RESULT_SETTINGS = 1;
    private static final int RESULT_WIFI = 2;
    private SharedPreferences sharedPrefs;
    private ProgressDialog mProgressDialog;
    private Context c = this;

    /* <Data> */
    private String title;
    private String videoid;
    private String mp3url;
    private String status = "";
    private String downloadsize;
    /* <Data/> */

    /* Pref Declaration */
    boolean savemode;
    private String savedir;
    private boolean autoupdate;

    private ProgressBar spinner;

    //UI Components Declaration
    private TextView title_box;
    private TextView status_box;
    private Button mp3_butt;
    private TextView downloadsize_box;

    private ColorStateList oldColors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Setting up spinner
        spinner = (ProgressBar)findViewById(R.id.progressBar1);
        spinner.setVisibility(View.GONE);

        //Set up UI Components
        title_box = (TextView) findViewById(R.id.title);
        status_box = (TextView) findViewById(R.id.status);
        downloadsize_box = (TextView) findViewById(R.id.downloadsize);
        mp3_butt = (Button) findViewById(R.id.downmp3);

        oldColors =  status_box.getTextColors(); //Save default color

        Typeface font_title = Typeface.createFromAsset(c.getAssets(), "fonts/JosefinSlab-Bold.ttf");
        title_box.setTypeface(font_title);
        status_box.setTypeface(font_title);
        downloadsize_box.setTypeface(font_title);

        //Set up ProgressDialog
        mProgressDialog = new CustomProgressDialog(MainActivity.this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setProgressDrawable(getResources().getDrawable(R.drawable.apptheme_progress_horizontal_holo_dark));
        mProgressDialog.setIndeterminateDrawable(getResources().getDrawable(R.drawable.apptheme_progress_indeterminate_horizontal_holo_dark));

        syncpref();

        /* Check First Time Launch */
        if (sharedPrefs.getBoolean("firststart", true)) { //Check if it's the first time you run the application
            //Show a disclaimer.
            showdialog(0, getResources().getString(R.string.disclaimer_title), getResources().getString(R.string.disclaimer_text));
        } else {
            checkandstart();
        }

    }

    void handleSendText(Intent intent) { //Gets share data from YouTube app.
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);

        if (sharedText != null) {
            // Update UI to reflect text being shared
            spinner.setVisibility(View.VISIBLE);

            String url;
            if (sharedText.contains("http://")) { //Old YouTube App Support
                url = sharedText.substring(sharedText.indexOf("http://"));
            } else if (sharedText.contains("https://")) {
                url = sharedText.substring(sharedText.indexOf("https://"));
            } else {
                url = null;
            }


            DownloadWebPageTask task = new DownloadWebPageTask();
            task.execute(url);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item)
    {

        switch (item.getItemId()) {
            case R.id.action_settings:
                    Intent i = new Intent(getApplicationContext(), MenuActivity.class);
                    startActivityForResult(i, RESULT_SETTINGS);
                overridePendingTransition(R.anim.slid_in, R.anim.slid_out);
                return true;
            case R.id.exit:
                finish();
                return true;
            case R.id.donate:
                Intent d = new Intent(getApplicationContext(), DonateActivity.class);
                startActivityForResult(d, 0); //Using startActivityForResult() to run animation when you come back to this activity
                overridePendingTransition(R.anim.slid_in, R.anim.slid_out);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) { //When you update the settings in menu, this updates them in the app.
        super.onActivityResult(requestCode, resultCode, data);

        overridePendingTransition(R.anim.slid_in, R.anim.slid_out);

        switch (requestCode) {
            case RESULT_SETTINGS:
                syncpref();
                break;
            case RESULT_WIFI:
                checkandstart();
                break;
        }
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        }
        return false;
    }

    public boolean isConnectedWifi(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
    }

    /* Sync & Update Pref */
    private void syncpref() {
        // Get preferences. Inizialization.
        sharedPrefs = PreferenceManager // Create a Preferences Object. This is used to retrive preferences from menu.
                .getDefaultSharedPreferences(this);
        savemode = sharedPrefs.getBoolean("prefSavename", false);
        Log.i("log_tag", "Save Pref looks like is: " + savemode);
        savedir = sharedPrefs.getString("prefSavedir", "");
        if (savedir.isEmpty()) { //If empty set SD Card as default ;)
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString("prefSavedir", String.valueOf(Environment.getExternalStorageDirectory()));
            editor.commit();
        }
        Log.i("log_tag", "SaveDir Pref looks like: " + savedir);
        autoupdate = sharedPrefs.getBoolean("prefAutoupdate", true);
        Log.i("log_tag", "AutoUpdate looks like is: " + autoupdate);
    }

    /* Check Internet Connection and Start processing */
    private void checkandstart() {
        if (!isOnline()) { //Check if you are online or not ;)
            showdialog(2, getResources().getString(R.string.no_internet_title), getResources().getString(R.string.no_internet_message));
        } else if (!isConnectedWifi()) {
            showdialog(1, getString(R.string.mobile_warn_title), getString(R.string.mobile_warn_message));
        } else { //Start Processing

            //Call handleSendText.
            Intent intent = getIntent();
            handleSendText(intent);
        }
    }

    /* Show Dialogs in this app */
    private void showdialog(final int id, String title, String message) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setCancelable(false);
        switch (id) {
            case 0:
                alert.setPositiveButton(getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        //Update Pref
                        SharedPreferences.Editor editor = sharedPrefs.edit();
                        editor.putBoolean("firststart", false);
                        editor.commit();
                        //Dismiss Dialog
                        dialog.dismiss();

                        checkandstart();
                    }
                });
                break;
            case 1:
                alert.setPositiveButton(getString(R.string.use_3g), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();

                        //Call handleSendText.
                        Intent intent = getIntent();
                        handleSendText(intent);

                    }
                });
                alert.setNegativeButton(getString(R.string.turnon_wifi), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), RESULT_WIFI); //Open Wi-Fi Settings
                        overridePendingTransition(R.anim.slid_in, R.anim.slid_out);
                    }
                });
                break;
            case 2:
                alert.setPositiveButton(getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        finish();
                    }
                });
                break;
            case 3:
                if (message==null || message.isEmpty()) { //If result is null, show a default error message
                    alert.setMessage(getResources().getString(R.string.download_error_message));
                }
                status_box.setText(getResources().getString(R.string.status_error));
                status_box.setTextColor(Color.RED);

                //Set title
                title_box.setText(getResources().getString(R.string.status_title_error));
                alert.setPositiveButton(getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                break;
        }
        alert.setIcon(R.drawable.ic_launcher);
        alert.show();
    }

    boolean cancelled = false;
    private class DownloadWebPageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

                        /* Code Below uses vidtomp3 remote api to convert video to MP3!
                                          - MP3 Download ;) */

            ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
            postParameters.add(new BasicNameValuePair("mediaurl", urls[0])); //Post Request mediaurl as parameter ;)

            String response_mp3 = null;

            String returnString = null;

            //execute request and get response ;)
            try {
                response_mp3 = CustomHttpClient.executeHttpPost(
                        "http://www.vidtomp3.com/cc/conversioncloud.php",  // vidtomp3 remove server api
                        postParameters);

                Log.i("log_tag", "API Response: " + response_mp3);
                returnString = response_mp3.substring(response_mp3.indexOf("statusurl")+12, response_mp3.length()-4); //Returns the statusurl
                returnString = returnString.replace("\\", ""); //This removes backslashes from the url ;))
                Log.i("log_tag", "1st: " + returnString); //Logs statusurl returned by the api

                // Connects to statusurl..should get an XML document..anyway..I'm not parsing it ;)
                final DefaultHttpClient httpClient = new DefaultHttpClient();

                HttpGet httpGet = new HttpGet(returnString);

                while (!status.equals("finished") && !cancelled) { //Repeat until status is finished
                    Log.i("log_tag", "Looping right now!");

                    //Set timeout ;)
                    final HttpParams httpParameters = httpClient.getParams();

                    HttpConnectionParams.setConnectionTimeout(httpParameters, 5 * 1000);
                    HttpConnectionParams.setSoTimeout(httpParameters, 5 * 1000);

                    HttpResponse httpResponse = httpClient.execute(httpGet);
                    HttpEntity httpEntity = httpResponse.getEntity();

                    HashMap<String, String> data;
                    if (httpEntity!=null) {
                        InputStream instream = httpEntity.getContent();
                        Vidtomp3parser parser = new Vidtomp3parser();
                        data = parser.parse(instream);
                        status = data.get("status");
                        if (status.equals("finished")) {
                            videoid = data.get("videoid");
                            title = data.get("file");
                            mp3url = data.get("downloadurl");
                            Log.i("log_tag","2nd: " + mp3url); //This should return mp3 download url ;)
                            downloadsize = data.get("filesize");
                            if (mProgressDialog.isShowing()) { //If showing hide it
                                mProgressDialog.dismiss(); //Dismiss ProgressDialog
                            }
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    spinner.setVisibility(View.GONE);
                                    status_box.setText(getResources().getString(R.string.status_checking));
                                    status_box.setTextColor(oldColors);
                                    if (!mProgressDialog.isShowing()) { //If not showing we show it
                                        mProgressDialog.setTitle(getResources().getString(R.string.converting_title));
                                        mProgressDialog.setMessage(getResources().getString(R.string.converting_message));
                                        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                            @Override
                                            public void onCancel(DialogInterface dialogInterface) {
                                                cancelled = true;
                                            }
                                        });
                                        mProgressDialog.show(); //Show ProgressDialog
                                    }
                                }
                            });

                            if (!cancelled) {
                                Thread.sleep(1000); //Sleeps 1 sec on failed
                            }
                        }
                    } else {
                        break;
                    }
                }

            } catch (ClientProtocolException e) {
                e.printStackTrace();
                return e.getMessage();
            } catch (IOException e) {
                e.printStackTrace();
                return e.getMessage();
            } catch (Exception e) {
                e.printStackTrace();
                return e.getMessage();
            }

            return null; /* If it's not null something is srs wrong :( */
        }

        @Override
        protected void onPostExecute(String result) { //When I have everything done...

            if (result!=null) {
                spinner.setVisibility(View.GONE);
                showdialog(3, getResources().getString(R.string.download_error_title), result);
            } else {
                try {

                    if (status.equals("finished")) { //New check..check if status equals finished..
                        status_box.setText(getResources().getString(R.string.status_ok));
                        status_box.setTextColor(Color.GREEN);

                        //Set title
                        title_box.setText(title);

                        //Set Download Size
                        downloadsize_box.setText(getResources().getString(R.string.downloadsize_def) + ": " + downloadsize);

                        //Enable the MP3 button ;)
                        mp3_butt.setEnabled(true);
                        mp3_butt.setClickable(true);
                    } else { //not yet? Something went fuckin wrong :(

                        if (mProgressDialog.isShowing()) { //If showing hide it
                            mProgressDialog.dismiss(); //Dismiss ProgressDialog
                        }

                        showdialog(3, getResources().getString(R.string.download_error_title), result);

                        //Disable the MP3 button ;)
                        mp3_butt.setEnabled(false);
                        mp3_butt.setClickable(false);
                    }

                    //Download MP3
                    mp3_butt.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final DownloadTask downloadTask = new DownloadTask(MainActivity.this);
                            downloadTask.execute(mp3url);

                            mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    downloadTask.cancel(true);
                                }
                            });
                        }
                    });

                    //Click on the Title to copy video title ;))
                    title_box.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            //Copy to ClipBoard
                            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                            clipboard.setText(title);
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    });

                    spinner.setVisibility(View.GONE);

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("log_tag","Error in OnPostExecute :((((");
                    spinner.setVisibility(View.GONE);
                    showdialog(3, getResources().getString(R.string.download_error_title), result);

                }


            }
            }
    }


    //Download Class - Download inside application This class downloads the video converted to Mp3
    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.setTitle(getResources().getString(R.string.download_title));
            mProgressDialog.setMessage(getResources().getString(R.string.download_message));
            mProgressDialog.show(); //Show ProgressDialog
        }

        @Override
        protected String doInBackground(String... sUrl) {
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            wl.acquire();

            try {
                InputStream input = null;
                OutputStream output = null;
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(sUrl[0]);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();

                    // expect HTTP 200 OK, so we don't mistakenly save error report
                    // instead of the file
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK)
                        return "Server returned HTTP " + connection.getResponseCode()
                                + " " + connection.getResponseMessage();

                    // this will be useful to display download percentage
                    // might be -1: server did not report the length
                    int fileLength = connection.getContentLength();

                    // download the file
                    input = connection.getInputStream();
                    //Save video in sdcard
                    //output = new FileOutputStream(Environment.getExternalStorageDirectory() + "/" + videoid +".mp4");
                    //Save video in the Download folder
                    if (savemode) { //Check Save Pref.
                        output = new FileOutputStream(savedir + "/" + title);
                    } else {
                        output = new FileOutputStream(savedir + "/" + videoid +".mp3");
                    }

                    byte data[] = new byte[4096];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {
                        // allow canceling with back button
                        if (isCancelled())
                            return null;
                        total += count;
                        // publishing the progress....
                        if (fileLength > 0) // only if total length is known
                            publishProgress((int) (total * 100 / fileLength));
                        output.write(data, 0, count);
                    }
                } catch (Exception e) {
                    return e.toString();
                } finally {
                    try {
                        if (output != null)
                            output.close();
                        if (input != null)
                            input.close();
                    }
                    catch (IOException ignored) { }

                    if (connection != null)
                        connection.disconnect();
                }
            } finally {
                wl.release();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) { //Download Result
            mProgressDialog.dismiss();
            if (result != null)
                showdialog(3, getResources().getString(R.string.download_error_title), result);
            else
                Toast.makeText(context, getResources().getString(R.string.download_completed) + " " + savedir, Toast.LENGTH_LONG).show();
        }
    }
}


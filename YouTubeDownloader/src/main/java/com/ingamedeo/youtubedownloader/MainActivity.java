package com.ingamedeo.youtubedownloader;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;
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
import org.apache.http.util.EntityUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

//Version: 2.1

public class MainActivity extends ActionBarActivity {

    private static final int RESULT_SETTINGS = 1;
    final Context context = this;
    SharedPreferences sharedPrefs;
    String encoding = "UTF-8";
    String title;
    Float length;
    String videoid;
    ProgressDialog mProgressDialog;
    String mp3url;
    String status;
    String downloadsize;
    String returnString;
    String percent;
    boolean savemode;
    String savedir;
    private boolean autoupdate;

    private ProgressBar spinner;

    TextView title_box;
    TextView status_box;
    Button mp3_butt;
    TextView downloadsize_box;

    ColorStateList oldColors;

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

        syncpref();

        if (!isOnline()) { //Check if you are online or not ;)
            Toast.makeText(context, getResources().getString(R.string.no_internet), Toast.LENGTH_LONG).show();
            finish();
        }

        if (sharedPrefs.getBoolean("firststart", true)) { //Check if it's the first time you run the application
            //Create a disclaimer.
            new AlertDialog.Builder(this)
            .setTitle(getResources().getString(R.string.disclaimer_title))
            .setMessage(getResources().getString(R.string.disclaimer_text))
            .setCancelable(false)
            .setPositiveButton(getResources().getString(android.R.string.yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    //Update Pref
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putBoolean("firststart", false);
                    editor.commit();
                    //Dismiss Dialog
                    dialog.dismiss();
                }
            })
            .setIcon(R.drawable.ic_launcher)
            .show();
        }

        //Call handleSendText.
        Intent intent = getIntent();
        handleSendText(intent);
    }

    void handleSendText(Intent intent) { //Gets share data from YouTube app.
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);

        if (sharedText != null) {
            // Update UI to reflect text being shared
            spinner.setVisibility(View.VISIBLE);

            String url = sharedText.substring(sharedText.indexOf("http://"));

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
                    Intent i = new Intent(getBaseContext(), MenuActivity.class);
                    startActivityForResult(i, RESULT_SETTINGS);
                return true;
            case R.id.exit:
                finish();
                return true;
            case R.id.donate:
                Toast.makeText(context, getResources().getString(R.string.donatemessage), Toast.LENGTH_LONG).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) { //When you update the settings in menu, this updates them in the app.
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SETTINGS:
                syncpref();
                break;

        }

    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        }
        return false;
    }

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

    private class DownloadWebPageTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

                        /* Code Below uses vidtomp3 remote api to convert video to MP3!
                                          MP3 Download function ;) */

            ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
            postParameters.add(new BasicNameValuePair("mediaurl", urls[0])); //Post Request mediaurl as parameter ;)

            String response_mp3 = null;

            returnString = null;

            //execute request and get response ;)
            try {
                response_mp3 = CustomHttpClient.executeHttpPost(
                        "http://www.vidtomp3.com/cc/conversioncloud.php",  // vidtomp3 remove server api
                        postParameters);

                Log.i("log_tag", "API Response: " + response_mp3);
                String result = response_mp3.toString(); //This should be removed. No need to convert to string.
                returnString = result.substring(result.indexOf("statusurl")+12, result.length()-4); //Returns the statusurl (It's a sort of JSON...but not parsing right way)
                returnString = returnString.replace("\\", ""); //This removes backslashes from the url ;))
                Log.i("log_tag", "1st: " + returnString); //Logs statusurl returned by the api

            }
            catch (Exception e) {
                Log.e("log_tag","Error while connecting to vidtomp3 api! " + e.toString());
            }

            try { //Connects to statusurl..should get an XML document..anyway..I'm not parsing it ;)
                DefaultHttpClient httpClient = new DefaultHttpClient();

                //Set timeout ;)
                final HttpParams httpParameters = httpClient.getParams();

                HttpConnectionParams.setConnectionTimeout(httpParameters, 5 * 1000);
                HttpConnectionParams.setSoTimeout(httpParameters, 5 * 1000);

                HttpGet httpGet = new HttpGet(returnString);

                HttpResponse httpResponse = httpClient.execute(httpGet);
                HttpEntity httpEntity = httpResponse.getEntity();
                String output = EntityUtils.toString(httpEntity);

                status = output.substring(output.indexOf("<status step=")+14, output.indexOf("/>")-1); //Extract status from XML file
                Log.i("log_tag", "Status: " + status); //This should return video download status ;)

                if (status.equals("finished")) {
                    Log.i("log_tag", "File ready to download ;)");

                    //Changed cos vidtomp3 changed their output 26-04-2014
                    mp3url = output.substring(output.indexOf("<downloadurl><![CDATA[")+22, output.indexOf("]]></downloadurl>")); //Extract mp3 download URL from XML file
                    Log.i("log_tag","2nd: " + mp3url); //This should return mp3 download url ;)
                    title = output.substring(output.indexOf("<file><![CDATA[")+15, output.indexOf("]]></file>")-4); //Extract title from XML file (without .mp3)
                    Log.i("log_tag",title); //This should return video title ;)
                    downloadsize = output.substring(output.indexOf("<filesize><![CDATA[")+19, output.indexOf("]]></filesize>")); //Extractdownload size from XML file
                    Log.i("log_tag", "Download Size: " + downloadsize); //This should return mp3 download size ;)
                }

            } catch (ClientProtocolException e) {
                e.printStackTrace();
                Log.i("log_tag", "There is a problem with your download ;(");
            } catch (IOException e) {
                e.printStackTrace();
                Log.i("log_tag", "There is a problem with your download ;(");
            } catch (Exception e) {
                e.printStackTrace();
                Log.i("log_tag", "There is a problem with your download ;(");
            }


            //This return is never read.
            //return response;
            return null;
        }

        //                  ONPOSTEXECUTE START!!

        @Override
        protected void onPostExecute(String result) { //When I have everything done...

            final ProgressDialog progressDialog;

            try {
                oldColors =  status_box.getTextColors(); //Save default color

                mProgressDialog = new ProgressDialog(MainActivity.this);
                mProgressDialog.setMessage(getResources().getString(R.string.progress_message));
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setCancelable(true);
                //Only API 11 > >
                int currentAPIVersion = android.os.Build.VERSION.SDK_INT;
                if (currentAPIVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                    mProgressDialog.setProgressNumberFormat(null); //Doesn't show number 0/100
                }

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
                } else {

                    spinner.setVisibility(View.GONE);


                    progressDialog = ProgressDialog.show(MainActivity.this, "", getResources().getString(R.string.converting));

                    new Thread() {

                        public void run() {

                            try{

                                sleep(10000);

                                runOnUiThread(new Runnable() { //Run on UI Thread ;)
                                    @Override
                                    public void run() {
                                        status_box.setText(getResources().getString(R.string.status_checking));
                                        status_box.setTextColor(oldColors);

                                        spinner.setVisibility(View.VISIBLE); //Spinner while checking...
                                    }
                                });

                                //ven30

                            } catch (Exception e) {

                                Log.e("tag", e.getMessage());

                            }

                            // dismiss the progress dialog
                            progressDialog.dismiss();

                        }

                    }.start();


                    status_box.setText(getResources().getString(R.string.status_error));
                    status_box.setTextColor(Color.RED);

                    //Set title
                    title_box.setText(getResources().getString(R.string.status_title_error));

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
                Toast.makeText(context, getResources().getString(R.string.operation_failed), Toast.LENGTH_LONG).show();

            }
            }

        /* ONPOSTEXECUTE END!! */

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
            mProgressDialog.show(); //Shows progressdialog we created before ;)
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
                    if (savemode==true) { //Check Save Pref.
                        output = new FileOutputStream(savedir + "/" + title +".mp3");
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
                Toast.makeText(context, getResources().getString(R.string.download_error) + " " + result, Toast.LENGTH_LONG).show();
            else
                Toast.makeText(context, getResources().getString(R.string.download_completed), Toast.LENGTH_LONG).show();
        }
    }
}


package com.ingamedeo.youtubedownloader;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

//Version: 0.5b
//getResources().getString(R.string.mystring); How to use strings from XML

public class MainActivity extends ActionBarActivity {

    String encoding = "UTF-8";
    String title;
    Float length;
    String videoid;
    final Context context = this;
    ProgressDialog mProgressDialog;
    String mp3url;
    String status;
    String downloadsize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads().detectDiskWrites().detectNetwork() // StrictMode is most commonly used to catch accidental disk or network access on the application's main thread
                .penaltyLog().build());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isOnline()==false) { //Check if you are online or not ;)
            Toast.makeText(context, getResources().getString(R.string.no_internet), Toast.LENGTH_LONG).show();
            finish();
        }

        //Create a disclaimer.
        final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(getResources().getString(R.string.disclaimer_title));
        alertDialog.setMessage(getResources().getString(R.string.disclaimer_text));
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
           public void onClick(DialogInterface dialog, int which) {
        //do stuff
               alertDialog.dismiss();
           }
        });
        alertDialog.setIcon(R.drawable.ic_launcher);
        alertDialog.show();

        //Call handleSendText.
        Intent intent = getIntent();
        handleSendText(intent);
    }

    void handleSendText(Intent intent) { //Gets share data from YouTube app.
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        String url;
        String videocode;
        String returnString;
        if (sharedText != null) {
            // Update UI to reflect text being shared

            //Get Video URL - Removes last part ;)
            url = sharedText.replaceAll("&feature=youtube_gdata_player","");

            //Get video code
            videocode = url.substring(url.lastIndexOf("=") + 1);

            videoid = videocode; //Maybe this can be removed

            readWebpage(videocode); //Calls readWebpage - Parses http://www.youtube.com/get_video_info?video_id= and downloads video info ;)

            /*
                          Code Below uses vidtomp3 remote api to convert video to MP3!
                                          MP3 Download function ;)
             */
            ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
            postParameters.add(new BasicNameValuePair("mediaurl", url)); //Post Request mediaurl as parameter ;)

            String response = null;

            returnString = null;

            //execute request and get response ;)
            try {
                response = CustomHttpClient.executeHttpPost(
                        "http://www.vidtomp3.com/cc/conversioncloud.php",  // vidtomp3 remove server api
                        postParameters);

                String result = response.toString(); //This should be removed. To need to convert to string.
                returnString = result.substring(result.indexOf("statusurl")+12, result.length()-4); //Returns the statusurl (It's a sort of JSON...but not parsing right way)
                returnString = returnString.replace("\\", ""); //This removes backslashes from the url ;))
                Log.i("log_tag", returnString); //Logs statusurl returned by the api

            }
            catch (Exception e) {
                    Log.e("log_tag","Error while connecting to vidtomp3 api! " + e.toString());
            }

            try { //Connects to statusurl..should get an XML document..anyway..I'm not parsing it ;)
                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(returnString);

                HttpResponse httpResponse = httpClient.execute(httpGet);
                HttpEntity httpEntity = httpResponse.getEntity();
                String output = EntityUtils.toString(httpEntity);

                mp3url = output.substring(output.indexOf("<downloadurl><![CDATA[")+22, output.indexOf("/]]></downloadurl>")); //Extract mp3 download URL from XML file
                Log.i("log_tag",mp3url); //This should return mp3 download url ;)
                title = output.substring(output.indexOf("<file><![CDATA[")+15, output.indexOf("]]></file>")-4); //Extract title from XML file (without .mp3)
                Log.i("log_tag",title); //This should return video title ;)
                status = output.substring(output.indexOf("<status step=")+14, output.indexOf("/>")-1); //Extract status from XML file
                Log.i("log_tag", "Status: " + status); //This should return video download status ;)
                downloadsize = output.substring(output.indexOf("<filesize><![CDATA[")+19, output.indexOf("]]></filesize>")); //Extractdownload size from XML file
                Log.i("log_tag", "Download Size: " + downloadsize); //This should return mp3 download size ;)

                if (status.equals("finished")) {
                    Log.i("log_tag", "File ready to download ;)");
                }

            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }



        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
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

    private class DownloadWebPageTask extends AsyncTask<String, Void, String> {
        String dataAsString;
        String urlAsString;
        @Override
        protected String doInBackground(String... urls) {
            String response = "";
            for (String url : urls) {
                DefaultHttpClient client = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(url);
                try {
                    HttpResponse execute = client.execute(httpGet);
                    InputStream content = execute.getEntity().getContent(); //instream - Can be used only once!

                    //Get page HTML... (video info stuff)
                    ByteArrayOutputStream content_2 = new ByteArrayOutputStream();

                    // Read response into a buffered stream
                    int readBytes = 0;
                    byte[] sBuffer = new byte[512];
                    while ((readBytes = content.read(sBuffer)) != -1) {
                        content_2.write(sBuffer, 0, readBytes);
                    }

                    // Return result from buffered stream - This is HTML video info page
                    dataAsString = new String(content_2.toByteArray());

                    //      Get Page HTML
                    ////////////////////////////////////

                    //Create a new InputStream from dataAsString we can work on
                    InputStream is = new ByteArrayInputStream(dataAsString.getBytes());
                    //Adding stuff
                    String videoInfo = getStringFromInputStream(encoding, is);
                    List<NameValuePair> infoMap = new ArrayList<NameValuePair>();
                    URLEncodedUtils.parse(infoMap, new Scanner(videoInfo), encoding);

                    for (NameValuePair pair : infoMap) {
                        String key = pair.getName();
                        String val = pair.getValue();
                        //System.out.println(key + " " + val);

                        /*

                        WARNING: This function has been disabled! Now Video Title is retrived through vidtomp3 api ;)

                        if (key.equals("title")) { //Parses page and get video title ;)
                            title = val;
                            //System.out.println(title); DEBUG Deleted on 02 01 2014
                        }
                        */
                    }

                    BufferedReader buffer = new BufferedReader(
                            new InputStreamReader(content));
                    String s = "";
                    while ((s = buffer.readLine()) != null) {
                        response += s;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            /*
              Well...there is a problem..we have to find a way to extract video download URL from YouTube video info page.
              So I call a PHP script hosted on my domain, that returns back video download URL ;)
              Let's see...
             */
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://ingamedeo.altervista.org/ff.php"); //PHP Script - Download URL

            // Request parameters and other properties.
            List<NameValuePair> params = new ArrayList<NameValuePair>(1);
            params.add(new BasicNameValuePair("page", dataAsString)); //Add parameter page POST request ;)
            try {
                httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            //Execute and get the response.
            HttpResponse response2 = null;
            try {
                response2 = httpclient.execute(httppost); //POST request.. page > video info stuff
            } catch (IOException e) {
                e.printStackTrace();
            }
            HttpEntity entity = response2.getEntity();

            if (entity != null) {
                InputStream instream = null;
                try {
                    instream = entity.getContent();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    ByteArrayOutputStream content_2 = new ByteArrayOutputStream(); //Create new ByteArray to read the response ;)

                    // Read response into a buffered stream
                    int readBytes = 0;
                    byte[] sBuffer = new byte[512];
                    try {
                        while ((readBytes = instream.read(sBuffer)) != -1) {
                            content_2.write(sBuffer, 0, readBytes);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Return result from buffered stream
                    urlAsString = new String(content_2.toByteArray());
                    Log.i("log_tag", urlAsString); //Should return video URL (From PHP Script)
                } finally {
                    try {
                        instream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }


            //This return is never read.
            return response;
        }

        @Override
        protected void onPostExecute(String result) { //When I have everything done...
            TextView title_box = (TextView) findViewById(R.id.title);
            //TextView videourl_box = (TextView) findViewById(R.id.videourl); DEBUG - REMOVED at 02 01 2014 21:53
            TextView status_box = (TextView) findViewById(R.id.status);
            TextView downloadsize_box = (TextView) findViewById(R.id.downloadsize);
            Button download_butt = (Button) findViewById(R.id.download);
            Button mp3_butt = (Button) findViewById(R.id.downmp3);


            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads().detectDiskWrites().detectNetwork() // StrictMode is most commonly used to catch accidental disk or network access on the application's main thread
                    .penaltyLog().build());

            mProgressDialog = new ProgressDialog(MainActivity.this);
            mProgressDialog.setMessage(getResources().getString(R.string.progress_message));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(true);

            //System.out.println(title);

            //testo.setText(Html.fromHtml(uri));

            //testo.setText(videoid);
            //videourl_box.setText(urlAsString); DEBUG - REMOVED at 02 01 2014 21:53
            //urlAsString.trim().length() > 15
            //if (urlAsString.trim().length() > 15) { //Check if url is not just signature= (empty)
            if (status.equals("finished")) { //New check..check if status equals finished..
                status_box.setText(getResources().getString(R.string.status_ok));
                status_box.setTextColor(Color.GREEN);

                //Set title
                title_box.setText(title);

                //Set Download Size
                downloadsize_box.setText(getResources().getString(R.string.downloadsize_def) + ": " + downloadsize);


                //Enable the button ;) Not implemented yet
                //download_butt.setEnabled(true);
                //download_butt.setClickable(true);

                //Enable the MP3 button ;)
                mp3_butt.setEnabled(true);
                mp3_butt.setClickable(true);
            } else {
                status_box.setText(getResources().getString(R.string.status_error));
                status_box.setTextColor(Color.RED);

                //Set title
                title_box.setText(getResources().getString(R.string.status_title_error));

                //Disable the button ;)
                //download_butt.setEnabled(false);
                //download_butt.setClickable(false);

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

            //Download Video MP4
            download_butt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //Download the video!

                    final DownloadTask downloadTask = new DownloadTask(MainActivity.this);
                    downloadTask.execute(urlAsString);

                    mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            downloadTask.cancel(true);
                        }
                    });

                    /*
                    //
                    // Download Manager Method (found not working on Samsung Devices. Like S4. Tester: Sofia)
                    //
                    String url = urlAsString;
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setDescription("YouTube Video Download");
                    request.setTitle(title);
// in order for this if to run, you must use the android 3.2 to compile your app
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                        request.allowScanningByMediaScanner();
                        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    }
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, title+".mp4");

// get download service and enqueue file
                    DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    manager.enqueue(request);
                    */


                    //AlertDialog!
                    //final AlertDialog alertDialog = new AlertDialog.Builder(context).create();
                    //alertDialog.setTitle("YouTube Video Download Started!");
                    //alertDialog.setMessage("I'm downloading your video ;) Please Wait");
                    //alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                     //   public void onClick(DialogInterface dialog, int which) {
                            //do stuff
                     //       alertDialog.dismiss();
                     //   }
                    //});
                    //alertDialog.setIcon(R.drawable.ic_launcher);
                   // alertDialog.show();

                }
            });

            /*
            videourl_box.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    //Copy to ClipBoard
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    clipboard.setText(urlAsString);
                    Toast.makeText(getApplicationContext(), "URL copied to clipboard!", Toast.LENGTH_SHORT).show();
                    return false;
                }
            });
            */

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
        }
    }

    public void readWebpage(String videocode) { //wif videocode as input calls the task that downloads and parses video info stuff
        DownloadWebPageTask task = new DownloadWebPageTask();
        task.execute("http://www.youtube.com/get_video_info?video_id="+videocode);
    }

    private static String getStringFromInputStream(String encoding, InputStream instream) throws UnsupportedEncodingException, IOException {
        Writer writer = new StringWriter();

        char[] buffer = new char[1024];
        try {
            Reader reader = new BufferedReader(new InputStreamReader(instream, encoding));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        } finally {
            instream.close();
        }
        String result = writer.toString();
        return result;
    }

    //Download Class - Download inside application
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
                    output = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + videoid +".mp3");

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


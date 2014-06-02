package com.ingamedeo.youtubedownloader;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class CustomProgressDialog extends ProgressDialog {

    public CustomProgressDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Method method = TextView.class.getMethod("setVisibility",
                    Integer.TYPE); //Make this method available here

            Field[] fields = this.getClass().getSuperclass()
                    .getDeclaredFields(); //Get all Fields

            for (Field field : fields) { //For each field
                if (field.getName().equalsIgnoreCase("mProgressNumber")) { //If it's mProgressNumber
                    field.setAccessible(true);
                    TextView textView = (TextView) field.get(this);
                    method.invoke(textView, View.GONE); //Set it's visibility go GONE
                }
            }

        } catch (Exception e) {
            Log.e("log_tag",
                    "Failed to invoke the progressDialog method 'setVisibility' and set 'mProgressNumber' to GONE.",
                    e);
        }
    }
}
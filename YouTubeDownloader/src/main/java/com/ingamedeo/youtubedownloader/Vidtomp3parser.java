package com.ingamedeo.youtubedownloader;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 *
 * >> vidtomp3 Parser <<
 *
 * Written by: Amedeo Arch <ingamedeo> --- 01/06/14.
 * License: http://creativecommons.org/licenses/by-nc-sa/4.0/
 *
 */
public class Vidtomp3parser {

    private static final String ns = null; //Don't use namespaces

    public HashMap<String, String> parse(InputStream in)  throws XmlPullParserException, IOException {

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readData(parser);
        } finally {
            in.close();
        }
    }

    private HashMap<String, String> readData(XmlPullParser parser) throws XmlPullParserException, IOException {

        HashMap<String, String> data = new HashMap<String, String>();

        String status = null;
        String videoid  = null;
        String file  = null;
        String downloadurl  = null;
        String filesize = null;

        parser.require(XmlPullParser.START_TAG, ns, "conversioncloud");
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("status")) {
                status = readStatus(parser);
            } else if (name.equals("videoid")) {
                videoid = readTag(parser, "videoid");

            } else if (name.equals("file")) {
                file = readTag(parser, "file");

            } else if (name.equals("downloadurl")) {
                downloadurl = readTag(parser, "downloadurl");
            }

            else if (name.equals("filesize")) {
                filesize = readTag(parser, "filesize");
            }
            else {
                skip(parser);
            }
        }

        /* Log Data from vidtomp3 API */

        /*
        *
        * Log.i("log_tag", videoid);
        * Log.i("log_tag", file);
        * Log.i("log_tag", downloadurl);
        * Log.i("log_tag", filesize);
        * Log.i("log_tag", status);
        *
         */

        data.put("videoid", videoid);
        data.put("file", file);
        data.put("downloadurl", downloadurl);
        data.put("filesize", filesize);
        data.put("status", status);

        return data;
    }

    private String readTag(XmlPullParser parser, String tag) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, tag);
        String title = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, tag);
        return title;
    }

    private String readStatus(XmlPullParser parser) throws IOException, XmlPullParserException {
        String status = "";
        parser.require(XmlPullParser.START_TAG, ns, "status");

        String tag = parser.getName();
        if (tag.equals("status")) {
            status = parser.getAttributeValue(null, "step");
            parser.nextTag();
        }

        parser.require(XmlPullParser.END_TAG, ns, "status");
        return status;
    }

    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) { //When this method is called current TAG should be a START_TAG
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--; //We I find a END_TAG I decrease depth by 1
                    break;
                case XmlPullParser.START_TAG:
                    depth++; //We I find a START_TAG I increase depth by 1
                    break;
            }
        }
    }
}

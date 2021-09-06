package net.znordic.fetchringscanner;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;

public class StoreData {

    private static final String TAG = "StoreData";
    public static String filename = "ringscanner";

    public static final int XML = 0;
    public static final int JSON = 1;
    public static final int INI = 2;

    Context mContext;

    public StoreData(Activity activity) {
        mContext = activity.getApplicationContext();
    }

    public void write(int format, Map<String, String> data){

        switch(format) {
            case XML: toXML(data); break;
            case JSON: toJSON(data); break;
            case INI: toINI("scanner", data); break;
        }
    }

    public void toXML(Map<String, String> data){


        Log.d(TAG,">>>>> Writing XML file <<<<<");

        Log.d(TAG,""+mContext.getFilesDir());
        Log.d(TAG,""+mContext.getExternalFilesDir(null));

        File file = new File(mContext.getExternalFilesDir(null), filename+".xml");

        try {
                FileOutputStream fos = new FileOutputStream(file);
                OutputStreamWriter myOutWriter = new OutputStreamWriter(fos);
                myOutWriter.append("<scanner>").append("\n");

                for (Map.Entry<String, String> entry : data.entrySet()) {
                    myOutWriter.append("<" + entry.getKey().toLowerCase() + ">" + entry.getValue() + "</" + entry.getKey().toLowerCase() + ">").append("\n");
                }
                myOutWriter.append("</scanner>").append("\n");
                myOutWriter.close();

                fos.flush();
                fos.close();

        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public void toJSON(Map<String, String> data){
        Gson gson = new Gson();
        String json = gson.toJson(data);
        Log.d(TAG,json);

        File file = new File(mContext.getExternalFilesDir(null), filename+".json");

        // Save your stream, don't forget to flush() it before closing it.
        try
        {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(json);
            myOutWriter.close();
            fOut.flush();
            fOut.close();
        }
        catch (IOException e)
        {
            Log.e("Exception", "File write failed: " + e.toString());
        }

    }


    public void toINI(String section, Map<String, String> data){
        File file = new File(mContext.getExternalFilesDir(null), filename+".ini");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fos);
            myOutWriter.append("[scanner]").append("\r\n");

            for (Map.Entry<String, String> entry : data.entrySet()) {
                myOutWriter.append(entry.getKey() + "=" + entry.getValue()).append("\r\n");
            }
            myOutWriter.close();

            fos.flush();
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();

        }

    }
}

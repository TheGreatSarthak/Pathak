package com.example.pathak;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String Tag = MainActivity.class.getSimpleName();
    public static final String TESS_DATA = "/tessdata";
    private TextView textView;
    private TessBaseAPI tessBaseAPI;
    private Uri outputFileDir;
    private static final String DATA_PATH = Environment.getExternalStorageDirectory().toString()+"/Tess";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) this.findViewById(R.id.textView);

        this.findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCameraActivity();
            }
        });
    }
    private void startCameraActivity(){
        try {
            String imagePath = DATA_PATH+ "/imgs";
            File dir = new File(imagePath);
            if(!dir.exists()){
                dir.mkdir();
            }
            String imageFilePath = imagePath+ "/ocr.jpg";
            outputFileDir = Uri.fromFile(new File(imageFilePath));
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                outputFileDir = FileProvider.getUriForFile(getApplicationContext(),BuildConfig.APPLICATION_ID+".provider", new File(imageFilePath));
            }
            final Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileDir);
            if(pictureIntent.resolveActivity(getPackageManager() ) !=null){
                startActivityForResult(pictureIntent,100);
            }
        } catch (Exception e){
            Log.e(Tag, e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==100 && resultCode== Activity.RESULT_OK){
            prepareTessData();
            startOCR(outputFileDir);
        }else{
            Toast.makeText(getApplicationContext(),"Image Problem",Toast.LENGTH_SHORT).show();
        }
    }
    private void prepareTessData(){
        try {
            File dir = new File(DATA_PATH+TESS_DATA);
            if(!dir.exists()){
                dir.mkdir();
            }
            String fileList[] = getAssets().list("");
            for(String fileName : fileList){
                String pathToDataFile = DATA_PATH+TESS_DATA+"/"+fileName;
                if(!(new File(pathToDataFile)).exists()){
                    InputStream in = getAssets().open(fileName);
                    OutputStream out = new FileOutputStream(pathToDataFile);
                    byte [] buff = new byte[1024];
                    int len;
                    while((len = in.read(buff))>0){
                        out.write(buff,0,len);
                    }
                    in.close();
                    out.close();
                }
            }
        } catch (IOException e) {
            Log.e(Tag, e.getMessage());
        }
    }
    private void startOCR(Uri imageUri){
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 7;
            Bitmap bitmap = BitmapFactory.decodeFile(imageUri.getPath(),options);
            String result=this.getText(bitmap);
            textView.setText(result);

        }catch (Exception e){
            Log.e(Tag, e.getMessage());
        }
    }
    private String getText(Bitmap bitmap){
        try {
            tessBaseAPI = new TessBaseAPI();
        }catch (Exception e){
            Log.e(Tag, e.getMessage());
        }
        tessBaseAPI.init(DATA_PATH,"hin");
        tessBaseAPI.setImage(bitmap);
        String retStr = "no result";
        try {
            retStr = tessBaseAPI.getUTF8Text();
        }catch (Exception e){
            Log.e(Tag, e.getMessage());
        }
        tessBaseAPI.end();
        return retStr;
    }
}
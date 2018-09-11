package com.example.administrator.volleydongnao15;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.administrator.volleydongnao15.http.download.FileDownManager;

public class MainActivity extends com.lythonliu.Linkcom.lythonliu.LinkAppCompatActivity {

    @Override
    public String getAppName(){
        return BuildConfig.APP_NAME;
    }

    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView= (TextView) findViewById(R.id.content);

    }

    public  void login(View view)
    {
        FileDownManager fileDownManager =new FileDownManager();
        fileDownManager.download("http://gdown.baidu.com/data/wisegame/8be18d2c0dc8a9c9/WPSOffice_177.apk");
    }
}

package com.example.android_review;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        threadPoolTest();
    }

    private void threadPoolTest() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 30, 1,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(128));
        for (int i = 0; i < 30; i++) {
            final int index = i;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        String threadName = Thread.currentThread().getName();
                        Log.v(TAG, "线程：" + threadName + ",正在执行第" + index + "个任务");
                        Thread.currentThread().sleep(2000);

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            executor.execute(runnable);
        }
    }
}

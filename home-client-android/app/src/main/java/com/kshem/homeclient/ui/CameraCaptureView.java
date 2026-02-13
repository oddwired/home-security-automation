package com.kshem.homeclient.ui;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kshem.homeclient.CctvCapture;
import com.kshem.homeclient.R;
import com.kshem.homeclient.beans.RestDataSource;

import java.io.IOException;
import java.util.List;

public class CameraCaptureView extends AppCompatActivity {

    private static final String TAG = CameraCaptureView.class.getSimpleName();

    private GridView gridView;
    private ProgressBar loadingProgressBar;
    protected TextView noDataView;

    private int limit = 50;
    private int currentPage = 0;
    private boolean reachedBottom = false;
    private boolean dataLoading = false;

    private PictureViewAdapter cctvViewAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_capture_view);

        noDataView = findViewById(R.id.noData);
        gridView = findViewById(R.id.pictureView);

        loadingProgressBar = findViewById(R.id.loading);

        cctvViewAdapter = new PictureViewAdapter(this);

        gridView.setAdapter(cctvViewAdapter);

        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                Log.i(TAG, "onScrollStateChanged");
            }

            @Override
            public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                Log.i(TAG, String.format("firstVisibleItem: %d, visibleItemCount: %d, totalItemCount: %d", firstVisibleItem, visibleItemCount, totalItemCount));
                if(firstVisibleItem + visibleItemCount + 1 > totalItemCount){
                    loadData(currentPage);
                }

                if(cctvViewAdapter.getCount() == 0){
                    noDataView.setVisibility(View.VISIBLE);
                }else{
                    noDataView.setVisibility(View.GONE);
                }
            }
        });
    }

    protected void loadData(int offset){
        if(dataLoading || reachedBottom){
            return;
        }

        dataLoading = true;
        loadingProgressBar.setVisibility(View.VISIBLE);

        loadData(offset, limit);
    }

    protected void loadData(int page, int limit) {
        try {
            RestDataSource.getCctvCaptures(page, limit, new RestDataSource.RestCallback<List<CctvCapture>>() {
                @Override
                public void onData(List<CctvCapture> data) {

                    setData(data);
                }

                @Override
                public void onError(String error) {
                    onErrorLoadingData();
                    runOnUiThread(() -> Toast.makeText(CameraCaptureView.this, "Error Fetching data", Toast.LENGTH_LONG).show());

                }
            });
        } catch (IOException e) {
            e.printStackTrace();

            Toast.makeText(CameraCaptureView.this, "Error Fetching data", Toast.LENGTH_LONG).show();
        }
    }

    protected void setData(List<CctvCapture> data){
        if(data.size() == 0){
            reachedBottom = true;
        }

        cctvViewAdapter.addItems(data);

        dataLoading = false;
        currentPage++;

        runOnUiThread(()-> loadingProgressBar.setVisibility(View.GONE));

    }

    protected void onErrorLoadingData(){
        dataLoading = false;

        runOnUiThread(()-> loadingProgressBar.setVisibility(View.GONE));
    }
}
package com.ghealth;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    String step_data = "Step Data\n";
    private static final int REQUEST_CODE_RESOLUTION = 100;
    SimpleDateFormat formatter = new SimpleDateFormat("dd MM yyyy");


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.e("XXXX", "Start Dud");

        App.getGoogleApiHelper().setConnectionListener(new GoogleApiHelper.ConnectionListener() {
            @Override
            public void onConnected(Bundle bundle) {
                Log.e("XXXX", "onConnected:45");
                readStepCountData();
            }

            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                Log.e("XXXX", "onConnectionFailed:51 " + connectionResult.getErrorMessage() + " " + connectionResult.getErrorCode() + " ");
                try {
                    connectionResult.startResolutionForResult(MainActivity.this, REQUEST_CODE_RESOLUTION);
                } catch (IntentSender.SendIntentException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onConnectionSuspended(int i) {
                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                    Log.i("XXXX", "Connection lost.  Cause: Network Lost.");
                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                    Log.i("XXXX", "Connection lost.  Reason: Service Disconnected");
                } else {
                    Log.e("XXXX", "onConnectionSuspended:61 " + i);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Log.e("onActivityResult", "RESULT_OK");
            App.getGoogleApiHelper().connect();
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Log.e("onActivityResult", "RESULT_CANCELED");
        } else {
            Log.e("onActivityResult", ">" + resultCode);
        }
    }

    private void readStepCountData() {
        // Specify the date you want to retrieve step count data for
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.MONTH, -1);
        long startTime = cal.getTimeInMillis();
        Log.e("XXXX", "DATE===>" + startTime + "-" + endTime);

        // Create a data request to fetch daily step count data
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .build();

        Fitness.getHistoryClient(this, GoogleSignIn.getAccountForExtension(this, fitnessOptions))
                .readData(readRequest)
                .addOnSuccessListener(response -> {
                    // The aggregate query puts datasets into buckets, so convert to a
                    // single list of datasets
                    Log.i("XXXX", "getBuckets size: " + response.getBuckets().size());
                    for (Bucket bucket : response.getBuckets()) {
                        Log.i("XXXX", "getDataSets size: " + bucket.getDataSets().size());
                        for (DataSet dataSet : bucket.getDataSets()) {
                            dumpDataSet(dataSet);
                        }
                    }
                    ((TextView) findViewById(R.id.tvInfo)).setText(step_data);
                })
                .addOnFailureListener(e -> {
                    Log.e("XXXX", "onConnectionSuspended " + e.toString());
                });
    }

    private void dumpDataSet(DataSet dataSet) {
        if (dataSet.getDataPoints().size() == 0) {
            Log.i("XXXX", "getDataPoints : No Data for the day");
        } else {
            for (DataPoint dataPoint : dataSet.getDataPoints()) {
                long stepCount = dataPoint.getValue(Field.FIELD_STEPS).asInt();
                long startTime = dataPoint.getStartTime(TimeUnit.MILLISECONDS);
                long endTime = dataPoint.getEndTime(TimeUnit.MILLISECONDS);
                long time = dataPoint.getTimestamp(TimeUnit.MILLISECONDS);
                Log.e("stepCount=====>", formatter.format(new Date(time)) + "=" + stepCount);
                step_data += formatter.format(new Date(time)) + "=" + stepCount +"\n";
            }
        }
    }
}
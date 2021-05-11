package bytes.sync.vaccinechecker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Map;

import bytes.sync.vaccinechecker.model.Session;

public class ResultActivity extends AppCompatActivity {

    private static final String TAG = ResultActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        Button stopButton = findViewById(R.id.stop_button);
        stopButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, APICallService.class);
            intent.setAction("SERVICE_ACTION_STOP");
            startService(intent);
            Toast.makeText(this, "availability checker service stopped", Toast.LENGTH_LONG).show();
        });

        TextView textView = findViewById(R.id.result_textView);
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        Map<String, List<Session>> stringListMap = readAvailableMap();
        if(stringListMap != null && stringListMap.size() > 0) {
            String jsonString = gson.toJson(stringListMap);
            textView.setText(jsonString);
        } else {
            textView.setText("Vaccine availability is 0");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, APICallService.class);
        intent.setAction("MUSIC_ACTION_STOP");
        startService(intent);
    }

    private Map<String, List<Session>> readAvailableMap() {
        try {
            FileInputStream fileInputStream = openFileInput("available_map");
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            return (Map<String, List<Session>>) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }

}
package bytes.sync.vaccinechecker;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputLayout;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextInputLayout districtInputLayout = findViewById(R.id.districtId_textInput);
        TextInputLayout dateInputLayout = findViewById(R.id.date_textInput);
        TextInputLayout minAgeInputLayout = findViewById(R.id.minAge_textInput);
        TextInputLayout delayInputLayout = findViewById(R.id.delay_textInput);


        Button startButton = findViewById(R.id.start_button);
        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, APICallService.class);
            intent.setAction("SERVICE_ACTION_START");
            String minAge = minAgeInputLayout.getEditText().getText().toString().isEmpty() ? "18" : minAgeInputLayout.getEditText().getText().toString();
            intent.putExtra("MIN_AGE_LIMIT", Integer.parseInt(minAge));
            intent.putExtra("DISTRICT_ID", districtInputLayout.getEditText().getText().toString());
            intent.putExtra("DATE", dateInputLayout.getEditText().getText().toString());
            String minDelay = delayInputLayout.getEditText().getText().toString().isEmpty() ? "5" : delayInputLayout.getEditText().getText().toString();
            intent.putExtra("DELAY", Long.parseLong(minDelay));
            startService(intent);
            Toast.makeText(this, "availability checker service started", Toast.LENGTH_LONG).show();
        });

    }
}
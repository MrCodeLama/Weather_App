package com.codelama.weather_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class City extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city);

        final Button button=findViewById(R.id.id_button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText city=findViewById(R.id.id_city);
                String citySearched=city.getText().toString();

                Intent returnIntent=new Intent();
                returnIntent.putExtra("result",citySearched);
                setResult(RESULT_OK,returnIntent);
                finish();
            }
        });


    }
}

package com.example.texttocsv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int OPEN_DIRECTORY_REQUEST_CODE = 1;
    private static final String PREFS_NAME = "TextToCSVPrefs";
    private static final String PREF_URI = "directory_uri";
    private static final String FILE_NAME = "Test.csv";
    private EditText editTextNumber1;
    private EditText editTextNumber2;
    private EditText editTextNumber3;
    private EditText editTextCharacter;
    private TextView textViewNumber1;
    private TextView textViewNumber2;
    private TextView textViewNumber3;
    private TextView textViewCharacter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextNumber1 = findViewById(R.id.editTextNumber1);
        editTextNumber2 = findViewById(R.id.editTextNumber2);
        editTextNumber3 = findViewById(R.id.editTextNumber3);
        editTextCharacter = findViewById(R.id.editTextCharacter);
        textViewNumber1 = findViewById(R.id.textViewNumber1);
        textViewNumber2 = findViewById(R.id.textViewNumber2);
        textViewNumber3 = findViewById(R.id.textViewNumber3);
        textViewCharacter = findViewById(R.id.textViewCharacter);
        Button buttonSave = findViewById(R.id.buttonSave);

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("MainActivity", "Save button clicked");
                saveToCSV();
            }
        });
    }

    private void saveToCSV() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String uriString = prefs.getString(PREF_URI, null);

        if (uriString == null) {
            openDirectoryPicker();
        } else {
            Uri treeUri = Uri.parse(uriString);
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
            if (pickedDir != null && pickedDir.canWrite()) {
                Uri fileUri = createOrGetFile(pickedDir, FILE_NAME);
                if (fileUri != null) {
                    appendToCSV(fileUri);
                }
            } else {
                openDirectoryPicker();
            }
        }
    }

    private void openDirectoryPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        startActivityForResult(intent, OPEN_DIRECTORY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == OPEN_DIRECTORY_REQUEST_CODE && resultCode == RESULT_OK) {
            if (resultData != null) {
                Uri treeUri = resultData.getData();
                getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().putString(PREF_URI, treeUri.toString()).apply();
                Uri fileUri = createOrGetFile(DocumentFile.fromTreeUri(this, treeUri), FILE_NAME);
                if (fileUri != null) {
                    appendToCSV(fileUri);
                }
            }
        }
    }

    private Uri createOrGetFile(DocumentFile pickedDir, String fileName) {
        DocumentFile existingFile = pickedDir.findFile(fileName);
        if (existingFile != null && existingFile.exists()) {
            return existingFile.getUri();
        } else {
            DocumentFile newFile = pickedDir.createFile("text/csv", fileName);
            return newFile != null ? newFile.getUri() : null;
        }
    }

    private void appendToCSV(Uri uri) {
        try {
            StringBuilder existingContent = new StringBuilder();
            boolean isFileNew = true;

            try (InputStream inputStream = getContentResolver().openInputStream(uri);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    existingContent.append(line).append("\n");
                    isFileNew = false;
                }
            } catch (IOException e) {
                Log.d("MainActivity", "Error reading file: " + e.getMessage());
            }

            if (isFileNew) {
                String header = String.format("Date,%s,%s,%s,%s",
                        textViewNumber1.getText().toString(),
                        textViewNumber2.getText().toString(),
                        textViewNumber3.getText().toString(),
                        textViewCharacter.getText().toString());
                existingContent.append(header).append("\n");
            }

            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            int number1 = getIntFromEditText(editTextNumber1);
            int number2 = getIntFromEditText(editTextNumber2);
            int number3 = getIntFromEditText(editTextNumber3);
            char character = getCharFromEditText(editTextCharacter);
            double number4 = 789.01;

            String newText = String.format("%s,%d,%d,%d,%c,%.2f", date, number1, number2, number3, character, number4);
            existingContent.append(newText).append("\n");

            try (OutputStream outputStream = getContentResolver().openOutputStream(uri, "wt");
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
                writer.write(existingContent.toString());
                writer.flush();
                Log.d("MainActivity", "File saved successfully");
                Toast.makeText(this, "File saved successfully", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Log.d("MainActivity", "Error saving file: " + e.getMessage());
                Toast.makeText(this, "Error saving file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.d("MainActivity", "An error occurred: " + e.getMessage());
            Toast.makeText(this, "An error occurred", Toast.LENGTH_SHORT).show();
        }
    }

    private int getIntFromEditText(EditText editText) {
        try {
            return Integer.parseInt(editText.getText().toString());
        } catch (NumberFormatException e) {
            Log.d("MainActivity", "Invalid input: " + editText.getText().toString());
            Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
            return 0;
        }
    }

    private char getCharFromEditText(EditText editText) {
        String text = editText.getText().toString();
        if (text.length() != 1) {
            Log.d("MainActivity", "Invalid character input: " + text);
            Toast.makeText(this, "Invalid character input", Toast.LENGTH_SHORT).show();
            return ' ';
        }
        return text.charAt(0);
    }
}

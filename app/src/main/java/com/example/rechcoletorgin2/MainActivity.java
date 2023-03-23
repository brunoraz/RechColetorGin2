package com.example.rechcoletorgin2;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.Request;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import java.util.prefs.Preferences;


public class MainActivity extends AppCompatActivity {

    private EditText operadorEditText, talaoEditText;
    private String empresa, token, link;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.readIni();

        operadorEditText = findViewById(R.id.operadorEditText);
        talaoEditText = findViewById(R.id.talaoEditText);

        // set listener to operadorEditText to move focus to talaoEditText when ENTER is pressed
        operadorEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    talaoEditText.requestFocus();
                    return true;
                }
                return false;
            }
        });

        // set listener to talaoEditText to validate and make the API request when ENTER is pressed
        talaoEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    String talao = talaoEditText.getText().toString().trim();
                    if (talao.length() != 22) {
                        talaoEditText.setError("Código de barras inválido");
                        return true;
                    }
                    makeRequest(operadorEditText.getText().toString(), talaoEditText.getText().toString());
                    return true;
                }
                return false;
            }
        });
    }

    private void makeRequest(String operador, String talao) {
        String url = this.link;
        String organization = this.empresa;// empresaEditText.getText().toString();
        String token = this.token; // tokenEditText.getText().toString();

        String productionOrder = talao.substring(0, 7);
        String sequence = talao.substring(7, 11);
        String productionSlipNumber = talao.substring(11, 18);
        String costCenter = talao.substring(18, 22);

        JSONObject payload = new JSONObject();
        try {
            payload.put("productionOrder", productionOrder);
            payload.put("sequence", sequence);
            payload.put("productionSlipNumber", productionSlipNumber);
            payload.put("costCenter", costCenter);
            payload.put("operator", operador);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url + "?organization=" + organization + "&entity=productionRegistry",
                payload, response -> {
            Toast.makeText(this, "Registro realizado com sucesso!", Toast.LENGTH_SHORT).show();
            clearFields();

        }, error -> {
            Toast.makeText(this, "Erro ao realizar registro: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            talaoEditText.setError("Erro na requisição");
            Log.e("TAG", "Error on make request: " + error.getMessage());
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("accept", "application/json");
                headers.put("Authorization", "Bearer " + token);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void clearFields() {
        operadorEditText.setText("");
        talaoEditText.setText("");
    }


        public void readIni() {
            try {
                Preferences prefs = new IniPreferences(new Ini(new File("config.ini")));
                link = prefs.node("properties").get("link", ""); //"https://api.siger.com.br/api/v1/insert"
                token = prefs.node("properties").get("token", "");
                empresa = prefs.node("properties").get("empresa", "");
                System.out.println(link);
                System.out.println(token);
                System.out.println(empresa);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

}
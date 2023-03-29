package com.example.rechcoletorgin2;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;


public class MainActivity extends AppCompatActivity {

    private EditText operadorEditText, talaoEditText;
    private TextView msgTextView;
    private String empresa, token, link;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        operadorEditText = findViewById(R.id.operadorEditText);
        talaoEditText = findViewById(R.id.talaoEditText);
        msgTextView = findViewById(R.id.msgTextView);

        this.readIni();

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
            payload.put("productionStartDate", "2023-03-28");
            payload.put("productionStartTime", "15:00");
            payload.put("productionFinalDate", "2023-03-28");
            payload.put("productionFinalTime", "15:00");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject payloadPai = new JSONObject();
        try {
            payloadPai.put("payload", payload);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url + "?organization=" + organization + "&entity=productionRegistry", payloadPai, response -> {
            try {
                this.exibeMensagem("Registro realizado com sucesso!\nID do Registro de Produção: " + response.getJSONArray("content").getJSONObject(0).getString("identifier"), false);
            } catch (Exception e) {
                this.exibeMensagem("Erro ao realizar registro: Retorno inválido", false);
            }
            clearFields();
        }, error -> {
            try {
                String mensagemRetorno =  new String(error.networkResponse.data,
                        HttpHeaderParser.parseCharset(error.networkResponse.headers, "utf-8"));
                JSONObject json = new JSONObject(mensagemRetorno);
                String errorMessage = json.getJSONObject("status").getString("message");
                this.exibeMensagem("Erro ao realizar registro: " + errorMessage, true);
            } catch (Exception e) {
                this.exibeMensagem("Erro ao realizar registro: Retorno inválido", false);
            }
            Log.e("TAG", "Error on make request: " + error.networkResponse.headers);

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

        request.setRetryPolicy(new DefaultRetryPolicy(
                0,
                -1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void clearFields() {
        operadorEditText.setText("");
        talaoEditText.setText("");
    }


        public void readIni() {
            File path = getExternalFilesDir("Rech2");
            if (!path.exists()) {
                path.mkdirs();
            }
            File file = new File(path.toString(), "config.ini");
            System.out.println(file.toString());
            try {
                this.readIniRead(file);
            } catch (IOException e) {

                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                    fileOutputStream.write("[properties]\nlink=http://192.168.255.142/api/v1/insert\ntoken=eyJhbGciOiJIUzUxMiJ9.eyJwYXlsb2FkIjp7ImNvZGlnb0NsaWVudGUiOjI3OTQsInNlcXVlbmNpYUluc3RhbGFjYW8iOjAsImNvbnRleHRvIjoiYXBvbnQifX0.wJENzg7bL8-JyLXpOFRgO0E8zCwkIMc-E3x1ajnmdz5COy7QNvmucITC7U5VzJ-LMkqBXb8v40RySfs0JVBuuA\nempresa=N03".getBytes());
                    fileOutputStream.close();
                    this.readIniRead(file);
                } catch (IOException e2) {
                    String error = "Erro na leitura do arquivo de configuração em: " + getFilesDir().getAbsolutePath() + "config.ini - " + e2.getMessage() ;
                    this.exibeMensagem(error, true);
                    e.printStackTrace();
                }
            }
        }

        private void readIniRead(File file) throws IOException {

            Ini ini = new Ini(file);
            IniPreferences iniPref = new IniPreferences(ini);
            Preferences prefs = new IniPreferences(new Ini(file));
            link = prefs.node("properties").get("link", ""); //"https://api.siger.com.br/api/v1/insert"
            token = prefs.node("properties").get("token", "");
            empresa = prefs.node("properties").get("empresa", "");
        }

        public void exibeMensagem(String msg, boolean error) {

            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            this.msgTextView.setText(msg);
            if(error) {
                this.msgTextView.setTextColor(Color.parseColor("#FF0000"));
            } else {
                this.msgTextView.setTextColor(Color.parseColor("#FF00FF00"));
            }
        }

}
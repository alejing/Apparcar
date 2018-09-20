package com.proyecto.android.apparcar;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class PreferenciasActivity extends AppCompatActivity {

    private SeekBar seekBar_cercania, seekBar_precio;
    private TextView tv_cercania, tv_precio;
    private Switch switch_ofertas, switch_servicios, switch_dejar_llaves;
    private Button btn_guardar_preferencias;

    // Archivo de preferencias basicas del parqueadero
    public static final String PREFS_NAME = "MisPreferenciasParqueadero";
    private int cercania, precio;
    private boolean conOfertas, conServicios, dejarLLaves;
    // Precio mínimo y máximo del valor de minuto de los parqueaderos para vehículos
    private int MIN_PRECIO = 48;  // en pesos
    private int MAX_PRECIO = 105; // en pesos
    //private int STEP_PRECIO = 1;
    // Cercanía mínima y máxima del parqueadero a buscar para vehículos
    private int MIN_CERCANIA = 200;   // en mts
    private int MAX_CERCANIA = 5000; // en mts

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferencias);

        // Pone la flecha de atrás en la barra de navegación a donde diga la activity en el AndroidManifiest.xml
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Manejo de las seekBar
        seekBar_cercania = (SeekBar)findViewById(R.id.seekBar_cercania);
        seekBar_cercania.setMax((MAX_CERCANIA - MIN_CERCANIA));
        tv_cercania = (TextView) findViewById(R.id.tv_cercania);

        seekBar_precio = (SeekBar)findViewById(R.id.seekBar_precio);
        seekBar_precio.setMax((MAX_PRECIO - MIN_PRECIO));
        tv_precio = (TextView) findViewById(R.id.tv_precio);

        // Manejo de los botones on-off
        switch_ofertas = (Switch) findViewById(R.id.switch_ofertas);
        switch_servicios = (Switch) findViewById(R.id.switch_servicios);
        switch_dejar_llaves = (Switch) findViewById(R.id.switch_dejar_llaves);

        // Se recuperan las preferencias de usuario
        recuperarPreferenciasDeUsuario_Parqueaderos();

        // Se actualiza la UI con las preferencias de usuario que trae el archivo
        tv_cercania.setText("Cercanía: " + cercania + " mts -> " + (double)((cercania)/1000.0) + " km");
        seekBar_cercania.setProgress(cercania - MIN_CERCANIA);

        tv_precio.setText("Precio: $" + precio + " Pesos");
        seekBar_precio.setProgress(precio - MIN_PRECIO);

        switch_ofertas.setChecked(conOfertas);
        switch_servicios.setChecked(conServicios);
        switch_dejar_llaves.setChecked(dejarLLaves);

        // Escuchador del seekBar_cercania
        seekBar_cercania.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = cercania;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = MIN_CERCANIA + progress;
                tv_cercania.setText("Cercanía: " + progressChangedValue + " mts -> " + (double)((progressChangedValue)/1000.0) + " km");
                cercania = progressChangedValue;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(PreferenciasActivity.this, "Seek bar progress is :" + progressChangedValue, Toast.LENGTH_SHORT).show();
                tv_cercania.setText("Cercanía: " + progressChangedValue + " mts -> " + (double)((progressChangedValue)/1000.0) + " km");
                cercania = progressChangedValue;
            }
        });

        // Escuchador del seekBar_precio
        seekBar_precio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = precio;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //Toast.makeText(PreferenciasActivity.this, "progress is :" + String.valueOf(progress), Toast.LENGTH_SHORT).show();
                progressChangedValue = MIN_PRECIO + progress;
                tv_precio.setText("Precio: $" + progressChangedValue + " Pesos");
                precio = progressChangedValue;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(PreferenciasActivity.this, "Seek bar progress is :" + progressChangedValue, Toast.LENGTH_SHORT).show();
                tv_precio.setText("Precio: $" + progressChangedValue + " Pesos");
                precio = progressChangedValue;
            }
        });

        // Funcionalidad del botón guardar preferencias
        btn_guardar_preferencias = (Button) findViewById(R.id.btn_guardar_preferencias);

        btn_guardar_preferencias.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (switch_ofertas.isChecked()) {
                    conOfertas = true;
                }else {
                    conOfertas = false;
                }
                if (switch_servicios.isChecked()) {
                    conServicios = true;
                }else {
                    conServicios = false;
                }
                if(switch_dejar_llaves.isChecked()){
                    dejarLLaves = true;
                }else{
                    dejarLLaves = false;
                }
                // Guarda el nuevo valor de referencia para cercanía
                // cercania = seekBar_cercania.getProgress();
                // Guarda el nuevo valor de referencia para precio
                // precio = seekBar_precio.getProgress();

                // Modifica el archivo de preferencias de usuario
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt(getString(R.string.save_cercania), cercania);
                editor.putInt(getString(R.string.save_precio), precio);
                editor.putBoolean(getString(R.string.save_con_ofertas), conOfertas);
                editor.putBoolean(getString(R.string.save_con_servicios), conServicios);
                editor.putBoolean(getString(R.string.save_dejar_llaves), dejarLLaves);
                // Commit the edits!
                editor.commit();

                Toast.makeText(PreferenciasActivity.this, "Preferencias guardadas con éxito.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Metodo para recuperar las preferencias del usuario según caracteristicas al buscar un parqueadero
    public void recuperarPreferenciasDeUsuario_Parqueaderos(){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        cercania = settings.getInt(getString(R.string.save_cercania), 500);
        precio = settings.getInt(getString(R.string.save_precio), 48);
        conOfertas = settings.getBoolean(getString(R.string.save_con_ofertas), false);
        conServicios = settings.getBoolean(getString(R.string.save_con_servicios), false);
        dejarLLaves = settings.getBoolean(getString(R.string.save_dejar_llaves), false);

        /*
        String pref = "Cercanía: "+ String.valueOf(cercania) +
                      "\nPrecio: " + String.valueOf(precio) +
                      "\nconOfertas: " + String.valueOf(conOfertas) +
                      "\nconServicios: " + String.valueOf(conServicios) +
                      "\ndejarLLaves: " + String.valueOf(dejarLLaves);

        Toast.makeText(MainActivity.this, pref, Toast.LENGTH_LONG).show();
        */
    }

    private int calculateProgress(int value, int MIN, int MAX, int STEP) {
        return (100 * (value - MIN)) / (MAX - MIN);
    }
}

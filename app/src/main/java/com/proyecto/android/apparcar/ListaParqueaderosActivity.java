package com.proyecto.android.apparcar;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class ListaParqueaderosActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    ListView list;
    ArrayList<DetallesParqueadero> listaParqueaderosEncontrados;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_parqueaderos);

        // captura la lista de parqueaderos encontrados desde la visa principal
        Bundle bundle = getIntent().getExtras();
        listaParqueaderosEncontrados = bundle.getParcelableArrayList("listaParqueaderosEncontrados");

        if(listaParqueaderosEncontrados.size() == 0){
            Toast.makeText(ListaParqueaderosActivity.this,
                    R.string.parqueaderos_no_encontrados,
                    Toast.LENGTH_LONG)
                    .show();
        }

        list = (ListView) findViewById(R.id.listView);

        // Adaptador personalizado para la lista
        AdvancedCustomArrayAdapter adapter = new AdvancedCustomArrayAdapter(this, listaParqueaderosEncontrados);
        list.setAdapter(adapter);

        // Implementando el escuchador para los items de la lista
        list.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        // Puedo implemetar el escuchador según el item presionado en la lista
        Toast.makeText(ListaParqueaderosActivity.this, listaParqueaderosEncontrados.get(position).getDireccion(), Toast.LENGTH_LONG).show();
    }
}

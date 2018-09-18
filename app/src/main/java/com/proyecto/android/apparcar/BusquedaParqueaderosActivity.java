package com.proyecto.android.apparcar;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.maps.model.LatLng;

public class BusquedaParqueaderosActivity extends AppCompatActivity {

    private final int AUTO_COMP_REQ_CODE = 1;
    private int tv_seleccionado = 0;
    private TextView tv_origen, tv_destino;
    private LatLng latLngOrigen, latLngDdestino;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_busqueda_parqueaderos);

        // Se instancian los TextView para poner información en ellos
        tv_origen = (TextView)findViewById(R.id.tv_origen);
        tv_destino = (TextView)findViewById(R.id.tv_destino);


    }

    public void onClick(View v)
    {
        switch (v.getId()){
            case R.id.tv_origen:
                tv_seleccionado = 1; // Se ha presionado el TextView Origen
                intentBuscarLugar();
                //Toast.makeText(BusquedaParqueaderosActivity.this, "tv_origen" , Toast.LENGTH_SHORT).show();
                break;
            case R.id.imb_mi_ubicacion:
                Toast.makeText(BusquedaParqueaderosActivity.this, "imb_mi_ubicación" , Toast.LENGTH_SHORT).show();
                break;
            case R.id.tv_destino:
                tv_seleccionado = 2; // Se ha presionado el TextView Destino
                intentBuscarLugar();
                break;
            case R.id.imb_agregar_destino:
                tv_seleccionado = 3; // Se ha presionado el Botón Destino
                intentBuscarLugar();
                //Toast.makeText(BusquedaParqueaderosActivity.this, "imb_agregar_destino" , Toast.LENGTH_SHORT).show();
                break;
        }
    }

    // Abre el cuadro de busqueda por filtro de direcciones de Google
    public void intentBuscarLugar(){
        try {
            // Filtra teniendo en cuenta el país
            AutocompleteFilter.Builder filterBuilder = new AutocompleteFilter.Builder();
            filterBuilder.setCountry("COL");
            //filterBuilder.setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS);

            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY)
                    .setFilter(filterBuilder.build())
                    .build(BusquedaParqueaderosActivity.this);

            startActivityForResult(intent, AUTO_COMP_REQ_CODE);

        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AUTO_COMP_REQ_CODE) {

            if (resultCode == RESULT_OK) {

                // Recibe los datos por medio del metodo getPlace()
                Place place = PlaceAutocomplete.getPlace(this, data);

                if(tv_seleccionado == 1){
                    tv_origen.setText(place.getAddress().toString());
                    // Captura la Lat y Long de Origen
                    latLngOrigen = place.getLatLng();
                }else if(tv_seleccionado == 2){
                    tv_destino.setText(place.getAddress().toString());
                    // Captura la Lat y Long de Destino
                    latLngDdestino = place.getLatLng();
                }else if(tv_seleccionado == 3){
                    tv_destino.setText(place.getAddress().toString());
                    // Captura la Lat y Long de Destino
                    latLngDdestino = place.getLatLng();
                }else{
                    Toast.makeText(BusquedaParqueaderosActivity.this, "No hay resultado que mostrar" , Toast.LENGTH_SHORT).show();
                }

                //Toast.makeText(BusquedaParqueaderosActivity.this, place.getAddress().toString() , Toast.LENGTH_SHORT).show();

            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {

                Status status = PlaceAutocomplete.getStatus(this, data);
                //Log.e("Tag ERROR:", status.getStatusMessage());
                Toast.makeText(BusquedaParqueaderosActivity.this, status.getStatusMessage() , Toast.LENGTH_SHORT).show();

            } else if (resultCode == RESULT_CANCELED) {
                // El usuario cancela la operación
                //Toast.makeText(BusquedaParqueaderosActivity.this, "Operación cancelada por el usuario" , Toast.LENGTH_SHORT).show();
            }
        }
    }
}

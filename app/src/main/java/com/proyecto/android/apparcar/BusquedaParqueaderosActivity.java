package com.proyecto.android.apparcar;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class BusquedaParqueaderosActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final int AUTO_COMP_REQ_CODE = 1;
    private int tv_seleccionado = 0;
    private TextView tv_origen, tv_destino;
    private LatLng latLngOrigen, latLngDdestino = null;

    // GoogleMap mMapView;
    private GoogleMap mMap;
    private MapView mMapView;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    // Realacionado a la posición original de latitud y longitud
    private GPSTrack gpsTracker;
    private double miLatitud;
    private double miLongitud;
    // variables relacionadas a los marcadores del mapa
    Marker markerOrigen, markerDestino = null;
    // Variable relacionada con la linea recta entre dos puntos a buscar
    Polyline rutaEnLineaRecta = null;
    // Direcciones de origen y destino obtenidas cada vez que se consulta un posible lugar
    String dirOrigen = "Mi Ubicación", dirDestino = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_busqueda_parqueaderos);

        // Se instancian los TextView para poner información en ellos
        tv_origen = (TextView) findViewById(R.id.tv_origen);
        tv_destino = (TextView) findViewById(R.id.tv_destino);

        // Manejo de permisos para acceso a localización por parte del usuario
        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Activación del seguimiento por medio del GPS y sus posibles combinaciones
        gpsTracker = new GPSTrack(BusquedaParqueaderosActivity.this);
        if (gpsTracker.canGetLocation()) {
            // Guarda los valores de latitud y longitud por defecto (donde esta el usuario actualmente)
            miLatitud = gpsTracker.getLatitude();
            miLongitud = gpsTracker.getLongitude();

            // Variable de clase con la posición inicial de origen con la posición del usuario
            latLngOrigen = new LatLng(gpsTracker.getLatitude(), gpsTracker.getLongitude());

        } else {
            gpsTracker.showSettingsAlert();
        }

        //Se crean los procedimientos y métodos para crear un mapa o una vista del mismo
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }
        // El mapa se visualiza en un MapView dentro de la activity principal (ojo no es Fragment)
        mMapView = (MapView) findViewById(R.id.map);
        mMapView.onCreate(mapViewBundle);
        mMapView.getMapAsync(this);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_origen:
                tv_seleccionado = 1; // Se ha presionado el TextView Origen
                intentBuscarLugar();
                //Toast.makeText(BusquedaParqueaderosActivity.this, "tv_origen" , Toast.LENGTH_SHORT).show();
                break;
            case R.id.imb_mi_ubicacion:
                // Quita el marcador puesto anteriormente
                markerOrigen.remove();
                // Vuelve y ubica el marcador en la posición de origen
                miUbicacion(mMap);
                // Pone el texto en la caja de su ubicación
                tv_origen.setText("Mi ubicación");
                dirOrigen = "Mi Ubicación";
                // Pone una liena recta que une los dos puntos
                ponerRutaEnLineaRecta();
                //Toast.makeText(BusquedaParqueaderosActivity.this, "imb_mi_ubicación" , Toast.LENGTH_SHORT).show();
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
    public void intentBuscarLugar() {
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

                if (tv_seleccionado == 1) {
                    tv_origen.setText(place.getAddress().toString());
                    dirOrigen = place.getAddress().toString();
                    // Captura la Lat y Long de Origen
                    latLngOrigen = place.getLatLng();
                    // Quita el marcador puesto anteriormente
                    markerOrigen.remove();
                    // Debe ubicar el marcador nuevo en la nueva posición
                    ponerMarcador(latLngOrigen, mMap, 1);

                    // Pone una liena recta que une los dos puntos
                    ponerRutaEnLineaRecta();

                } else if (tv_seleccionado == 2) {
                    tv_destino.setText(place.getAddress().toString());
                    dirDestino = place.getAddress().toString();
                    // Captura la Lat y Long de Destino
                    latLngDdestino = place.getLatLng();

                    // Quita el marcador puesto anteriormente
                    if (markerDestino != null) {
                        markerDestino.remove();// Debe ubicar el marcador nuevo en la nueva posición
                    }
                    // Pone el nuevo marcador en el nuevo lugar
                    ponerMarcador(latLngDdestino, mMap, 2);

                    // Pone una liena recta que une los dos puntos
                    ponerRutaEnLineaRecta();

                } else if (tv_seleccionado == 3) {
                    tv_destino.setText(place.getAddress().toString());
                    dirDestino = place.getAddress().toString();
                    // Captura la Lat y Long de Destino
                    latLngDdestino = place.getLatLng();
                    // Quita el marcador puesto anteriormente
                    if (markerDestino != null) {
                        markerDestino.remove();// Debe ubicar el marcador nuevo en la nueva posición
                    }
                    // Debe ubicar el marcador nuevo en la nueva posición
                    ponerMarcador(latLngDdestino, mMap, 2);
                    // Pone una liena recta que une los dos puntos
                    ponerRutaEnLineaRecta();
                } else {
                    Toast.makeText(BusquedaParqueaderosActivity.this, "No hay resultado que mostrar", Toast.LENGTH_SHORT).show();
                }

                //Toast.makeText(BusquedaParqueaderosActivity.this, place.getAddress().toString() , Toast.LENGTH_SHORT).show();

            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {

                Status status = PlaceAutocomplete.getStatus(this, data);
                //Log.e("Tag ERROR:", status.getStatusMessage());
                Toast.makeText(BusquedaParqueaderosActivity.this, status.getStatusMessage(), Toast.LENGTH_SHORT).show();

            } else if (resultCode == RESULT_CANCELED) {
                // El usuario cancela la operación
                //Toast.makeText(BusquedaParqueaderosActivity.this, "Operación cancelada por el usuario" , Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        //mMap.setMyLocationEnabled(true);
        miUbicacion(mMap);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Guarda las configuraciones del mapa
        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mMapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    // Método que me permite ubicar según mi ubicación GPS
    public void miUbicacion(GoogleMap map){

        // Add a marker en el lugar donde esta el usuario, moviendo la camara.
        LatLng miUbicacion = new LatLng(miLatitud, miLongitud);

        latLngOrigen = miUbicacion;

        markerOrigen = map.addMarker(new MarkerOptions()
                .position(miUbicacion)
                .title("Origen")
                .snippet(dirOrigen)
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_map_carro)));

        // Elimina las indicaciones de zoom, giros y demás
        map.getUiSettings().setMapToolbarEnabled(false);
        // Posiciona la cama del mapa según la vista deseada
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(miUbicacion)       // Sets the center of the map to Bogotá
                .zoom(12)                   // Sets the zoom
                .bearing(90)                // Sets the orientation of the camera to east
                .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    public void ponerMarcador(LatLng latLng, GoogleMap map, int tipo_marcador){

        CameraPosition cameraPosition;

        if(tipo_marcador == 1){ // Marcador de origen
            markerOrigen = map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Origen")
                    .snippet(dirOrigen)
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_map_carro)));

            cameraPosition = new CameraPosition.Builder()
                    .target(latLng)       // Sets the center of the map to Bogotá
                    .zoom(12)                   // Sets the zoom
                    .bearing(90)                // Sets the orientation of the camera to east
                    .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        }else if(tipo_marcador == 2){ // marcador de destino
            markerDestino = map.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Destino")
                    .snippet(dirDestino)
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_img_destino_busqueda)));

            cameraPosition = new CameraPosition.Builder()
                    .target(latLng)       // Sets the center of the map to Bogotá
                    .zoom(15)                   // Sets the zoom
                    .bearing(90)                // Sets the orientation of the camera to east
                    .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

        // Elimina las indicaciones de zoom, giros y demás
        map.getUiSettings().setMapToolbarEnabled(false);



    }
    public void ponerRutaEnLineaRecta(){
        // Quita la linea anterior
        if(rutaEnLineaRecta != null){
            rutaEnLineaRecta.remove();// Debe ubicar el marcador nuevo en la nueva posición
        }

        if(latLngDdestino != null){
            // Pone la nueva line aque une origen y destino
            rutaEnLineaRecta = mMap.addPolyline(new PolylineOptions()
                    .add(latLngOrigen, latLngDdestino)
                    .width(7)
                    .color(Color.BLACK));
        }
    }
}

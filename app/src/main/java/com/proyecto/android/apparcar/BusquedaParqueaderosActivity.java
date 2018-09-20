package com.proyecto.android.apparcar;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.annotation.NonNull;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class BusquedaParqueaderosActivity extends AppCompatActivity implements OnMapReadyCallback {

    private final int AUTO_COMP_REQ_CODE = 1;
    private int tv_seleccionado = 0;
    private TextView tv_origen, tv_destino;
    private LatLng latLngOrigen, latLngDestino = null;

    // GoogleMap mMapView;
    private GoogleMap mMap;
    private MapView mMapView;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";
    // Realacionado a la posición original de latitud y longitud
    private GPSTrack gpsTracker;
    private double miLatitud;
    private double miLongitud;
    // variables relacionadas a los marcadores del mapa
    private Marker markerOrigen, markerDestino = null;
    // Variable relacionada con la linea recta entre dos puntos a buscar
    private Polyline rutaEnLineaRecta = null;
    // Direcciones de origen y destino obtenidas cada vez que se consulta un posible lugar
    private String dirOrigen = "Mi Ubicación", dirDestino = "";

    // Lista de parqueaderos completa y filtrada
    private ArrayList<DetallesParqueadero> listaParqueaderosCompleta;
    private ArrayList<DetallesParqueadero> listaParqueaderosEncontrados;
    // Lista de marcadores puestos en el mapa distintos al de origen y destino
    private ArrayList<Marker> listaDeMarcadoresMapa;

    // Archivo de preferencias basicas del parqueadero
    public static final String PREFS_NAME = "MisPreferenciasParqueadero";
    private int cercania, precio;
    private boolean conServicios, conOfertas, dejarLLaves;

    // Bitmap iconos
    BitmapDescriptor iconMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_busqueda_parqueaderos);

        // captura la lista de parqueaderos totales desde la visa principal
        Bundle bundle = getIntent().getExtras();
        listaParqueaderosCompleta = bundle.getParcelableArrayList("listaParqueaderosCompleta");

        // Inicializa las listas utiles
        listaParqueaderosEncontrados = new ArrayList<>();
        listaDeMarcadoresMapa = new ArrayList<>();

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

                // limpia la lista de parqueaderos encontrados para realizar una nueva busqueda
                if(listaParqueaderosEncontrados != null && listaParqueaderosEncontrados.size() != 0){
                    quitarMarcadoresMapaFiltrados();
                    listaParqueaderosEncontrados.clear();
                }

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

                    // limpia la lista de parqueaderos encontrados para realizar una nueva busqueda
                    if(listaParqueaderosEncontrados != null && listaParqueaderosEncontrados.size() != 0){
                        quitarMarcadoresMapaFiltrados();
                        listaParqueaderosEncontrados.clear();
                    }

                } else if (tv_seleccionado == 2) {
                    tv_destino.setText(place.getAddress().toString());
                    dirDestino = place.getAddress().toString();
                    // Captura la Lat y Long de Destino
                    latLngDestino = place.getLatLng();

                    // Quita el marcador puesto anteriormente
                    if (markerDestino != null) {
                        markerDestino.remove();// Debe ubicar el marcador nuevo en la nueva posición
                    }
                    // Pone el nuevo marcador en el nuevo lugar
                    ponerMarcador(latLngDestino, mMap, 2);

                    // Pone una liena recta que une los dos puntos
                    ponerRutaEnLineaRecta();

                    // Observamos los parqueaderos filtrados
                    filtrarParqueaderos();

                } else if (tv_seleccionado == 3) {
                    tv_destino.setText(place.getAddress().toString());
                    dirDestino = place.getAddress().toString();
                    // Captura la Lat y Long de Destino
                    latLngDestino = place.getLatLng();
                    // Quita el marcador puesto anteriormente
                    if (markerDestino != null) {
                        markerDestino.remove();// Debe ubicar el marcador nuevo en la nueva posición
                    }
                    // Debe ubicar el marcador nuevo en la nueva posición
                    ponerMarcador(latLngDestino, mMap, 2);

                    // Pone una liena recta que une los dos puntos
                    ponerRutaEnLineaRecta();

                    // Observamos los parqueaderos filtrados
                    filtrarParqueaderos();

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

    // Metodo que pone el marcador según la selección del mismo (origen o destino)
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
                    .zoom(14)                   // Sets the zoom
                    .bearing(90)                // Sets the orientation of the camera to east
                    .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

        // Elimina las indicaciones de zoom, giros y demás
        map.getUiSettings().setMapToolbarEnabled(false);
    }

    // Metodo que pone una linea recta desde el origen hasta el destino
    public void ponerRutaEnLineaRecta(){
        // Quita la linea anterior
        if(rutaEnLineaRecta != null){
            rutaEnLineaRecta.remove();// Debe ubicar el marcador nuevo en la nueva posición
        }

        if(latLngDestino != null){
            // Pone la nueva line aque une origen y destino
            rutaEnLineaRecta = mMap.addPolyline(new PolylineOptions()
                    .add(latLngOrigen, latLngDestino)
                    .width(7)
                    .color(Color.BLACK));
        }
    }

    // Metodo para recuperar las preferencias del usuario según caracteristicas al buscar un parqueadero
    public void recuperarPreferenciasDeUsuario_Parqueaderos(){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        cercania = settings.getInt(getString(R.string.save_cercania), 500);
        precio = settings.getInt(getString(R.string.save_precio), 48);
        conOfertas = settings.getBoolean(getString(R.string.save_con_ofertas), false);
        conServicios = settings.getBoolean(getString(R.string.save_con_servicios), false);
        dejarLLaves = settings.getBoolean(getString(R.string.save_dejar_llaves), false);
    }

    // Algoritmo voraz para la busqueda de parqueadeos
    public void filtrarParqueaderos(){

        // Filtrar y seleccionar en una nueva lista según preferencias de usuario
        recuperarPreferenciasDeUsuario_Parqueaderos(); // Se recuperan las preferencias del usuario

        // limpia la lista de parqueaderos encontrados para realizar una nueva busqueda
        if(listaParqueaderosEncontrados != null && listaParqueaderosEncontrados.size() != 0){
            quitarMarcadoresMapaFiltrados();
            listaParqueaderosEncontrados.clear();
        }

        // Itera la lista de parqueaderos de forma voraz
        for(int i = 0; i < listaParqueaderosCompleta.size(); i ++){

            // Instancia un nuevo parqueadero
            DetallesParqueadero parqueadero = listaParqueaderosCompleta.get(i);

            // Se procede a calcular distancia en linea recta (G(s) algorítmo voraz)
            double d_km = (double)(cercania / 1000.0);
            double d;

            // Calculo de la distancia en linea recta por medio de coordenadas Lat - Lng
            d = (6371 * Math.acos( Math.cos(Math.toRadians(latLngDestino.latitude)) * Math.cos(Math.toRadians(parqueadero.getLatitud())) * Math.cos( Math.toRadians(parqueadero.getLongitud()) - Math.toRadians(latLngDestino.longitude)) + Math.sin(Math.toRadians(latLngDestino.latitude)) * Math.sin( Math.toRadians(parqueadero.getLatitud()))));

            // Verifica que el parqueadero encontrado se encuentre en la distancia establecida como aceptable por el usuario
            if(d <= d_km) {
                // TODO por hacer: verificar otras caracteristicas de la busqueda ... precio, ofertas, servicio y dejar llaves
                listaParqueaderosEncontrados.add(parqueadero);
            }
        }

        // Ver todos los parqueaderos encontrados según prefrencias de usuario
        if(listaParqueaderosEncontrados.size() == 0){
            Toast.makeText(BusquedaParqueaderosActivity.this,
                    R.string.parqueaderos_no_encontrados,
                    Toast.LENGTH_LONG)
                    .show();
        }else{
            verTodosLosParqueaderos(listaParqueaderosEncontrados);
            Toast.makeText(BusquedaParqueaderosActivity.this,"Encontrados: " + String.valueOf(listaParqueaderosEncontrados.size()), Toast.LENGTH_SHORT).show();
        }
    }

    // Método para ver todos los parqueaderos y ponerlos en el mapa
    public void verTodosLosParqueaderos(List<DetallesParqueadero> list){

        /*
        // Quitamos los marcadores puestos antes de una nueva busqueda y postura de marcadores
        if(listaDeMarcadoresMapa != null && listaDeMarcadoresMapa.size() != 0){
            for (int i = 0; i < listaDeMarcadoresMapa.size(); i++){
                listaDeMarcadoresMapa.get(i).remove();
            }
        }
        */
        // Limpia la lista de marcadores para una nueva postura en una nueva busqueda
        listaDeMarcadoresMapa.clear();

        if(mMap != null) {
            for (int i = 0; i < list.size(); i++) {
                DetallesParqueadero parqueadero;
                parqueadero = list.get(i);

                LatLng parqueaderoLarLng = new LatLng(parqueadero.getLatitud(), parqueadero.getLongitud());

                // Pone el icono del mapa según el estado de ocupación
                switch(obtenerEstadoParqueadero(parqueadero.getCupos(), parqueadero.getCuposDisponibles())){
                    case 0:
                        iconMap = BitmapDescriptorFactory.fromResource(R.mipmap.ic_map_parqueadero_azul);
                        break;
                    case 1:
                        iconMap = BitmapDescriptorFactory.fromResource(R.mipmap.ic_map_parqueadero_rojo);
                        break;
                    case 2:
                        iconMap = BitmapDescriptorFactory.fromResource(R.mipmap.ic_map_parqueadero_rosa);
                        break;
                    case 3:
                        iconMap = BitmapDescriptorFactory.fromResource(R.mipmap.ic_map_parqueadero_naranja);
                        break;
                    case 4:
                        iconMap = BitmapDescriptorFactory.fromResource(R.mipmap.ic_map_parqueadero_verde_claro);
                        break;
                    case 5:
                        iconMap = BitmapDescriptorFactory.fromResource(R.mipmap.ic_map_parqueadero_verde_oscuro);
                        break;
                }

                // Visualiza el marcador
                Marker marker = mMap.addMarker(new MarkerOptions().
                        position(parqueaderoLarLng).
                        title(String.valueOf(parqueadero.getIdParqueadero())).
                        icon(iconMap));

                listaDeMarcadoresMapa.add(marker);
            }
        }
    }

    // Retorna un valor numerico de 1 a 5 según la ocupación del parqueadero 1 muy pocos cupos disponibles y 5 muchos cupos disponibles
    public int obtenerEstadoParqueadero(int cuposTotales, int cuposDisponibles){

        int estado = 0; // Retorna estado por defecto -> color azul

        int porcentaje = (cuposDisponibles * 100) / cuposTotales; // calcula el porcentaje de cupos disponibles

        if(porcentaje >= 0 && porcentaje <= 10){ // Retorna rojo (muy pocos cupos) 0 - 10%
            estado = 1;
        }else if(porcentaje >= 11 && porcentaje <= 30){ // Retorna rosa (pocos cupos) 11 - 30%
            estado = 2;
        }else if(porcentaje >= 31 && porcentaje <= 50){ // Retorna naranja (algunos cupos) 31 - 50%
            estado = 3;
        }else if(porcentaje >= 51 && porcentaje <= 80){ // Retorna verde claro (suficientes cupos) 51 - 80%
            estado = 4;
        }else if(porcentaje >= 81 && porcentaje <= 100){ // Retorna verde oscuro (muchos cupos) 81 - 100%
            estado = 5;
        }else{
            estado = 0;
        }

        return estado;
    }

    // Limpiar marcadores puestos despues de la busqueda
    public void quitarMarcadoresMapaFiltrados(){
        for (int i = 0; i < listaDeMarcadoresMapa.size(); i++){
            listaDeMarcadoresMapa.get(i).remove();
        }
    }
}

package com.proyecto.android.apparcar;

import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    // GoogleMap mMapView;
    private GoogleMap mMap;
    private MapView mMapView;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    // URL API Parqueaderos
    private String apiUrl; //"http://192.168.0.7/apparcar/getParqueaderos.php";
    private ProgressDialog pDialog;
    private JSONArray parqueaderoArray;


    // Variables correspondientes a la localización del usuario
    private double miLatitud;
    private double miLongitud;
    private GPSTrack gpsTracker;

    // Archivo de preferencias basicas del parqueadero
    public static final String PREFS_NAME = "MisPreferenciasParqueadero";
    private int cercania, precio;
    private boolean conServicios, conOfertas, dejarLLaves;

    // Listas de los parqueaderos que hay o se han encontrado en el sistema
    private List<DetallesParqueadero> listaParqueaderosTotal;
    private List<DetallesParqueadero> listaParqueaderosEncontrados;

    // Bitmap iconos
    BitmapDescriptor iconMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // URL del servicio WEB -> Importante saber a que servidor apunta la app
        apiUrl = getString(R.string.urlApi_getParqueaderos);

        FloatingActionButton miUbicacion = (FloatingActionButton) findViewById(R.id.miUbicacion);
        miUbicacion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Vuelve y centra el mapa en la teniendo en cuenta la ubicación del usuario
                miUbicacion(mMap);
            }
        });

        FloatingActionButton miBuscar = (FloatingActionButton) findViewById(R.id.miBuscar);
        miBuscar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        FloatingActionButton miLista = (FloatingActionButton) findViewById(R.id.miLista);
        miLista.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Se configura la apertura de la vista tipo lista de parqueaderos, enviando el array de parqueaderos encontrados según filtro de usuario
                Intent intent = new Intent(MainActivity.this, ListaParqueaderosActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList("listaParqueaderosEncontrados", (ArrayList<? extends Parcelable>) listaParqueaderosEncontrados);
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        // Se capturan por defecto las preferencias del usuario al buscar parqueaderos
        recuperarPreferenciasDeUsuario_Parqueaderos();

        // Manejo de permisos para acceso a localización por parte del usuario
        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        // Activación del seguimiento por medio del GPS y sus posibles combinaciones
        gpsTracker = new GPSTrack(MainActivity.this);
        if(gpsTracker.canGetLocation()){
            // Variables de clase de con el valor de mi longitud y mi latitud
            miLatitud = gpsTracker.getLatitude();
            miLongitud = gpsTracker.getLongitude();

        }else{
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

        //Toast.makeText(MainActivity.this, pref, Toast.LENGTH_LONG).show();
        */

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
        // Descarga asíncrona de parqueaderos según petición de usuario
        TareaAsincronaPeticionParqueaderos myAsyncTasks = new TareaAsincronaPeticionParqueaderos();
        myAsyncTasks.execute();
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
        mMapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    // Este metodo se carga cuando esta listo el mapa que se desea visualizar
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.setOnMarkerClickListener(this);
        // Agregó mi ubicación en un marcador negro en forma de carro
        miUbicacion(map);
    }

    // Método que me permite ubicar según mi ubicación GPS
    public void miUbicacion(GoogleMap map){

        // Add a marker en el lugar donde esta el usuario, moviendo la camara.
        LatLng miUbicacion = new LatLng(miLatitud, miLongitud);

        map.addMarker(new MarkerOptions().
                position(miUbicacion).
                title("Yo!")
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_map_carro)));

        // Elimina las indicaciones de zoom, giros y demás
        map.getUiSettings().setMapToolbarEnabled(false);
        // Posiciona la cama del mapa según la vista deseada
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(miUbicacion)        // Sets the center of the map to Bogotá
                .zoom(12)                   // Sets the zoom
                .bearing(90)                // Sets the orientation of the camera to east
                .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                .build();                   // Creates a CameraPosition from the builder
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

    }

    // Método para ver todos los parqueaderos y ponerlos en el mapa
    public void verTodosLosParqueaderos(List<DetallesParqueadero> list){
        if(mMap != null) {
            for (int i = 0; i < list.size(); i++) {
                DetallesParqueadero parqueadero;
                parqueadero = list.get(i);

                LatLng parqueaderoLarLng = new LatLng(parqueadero.getLatitud(), parqueadero.getLongitud());

                // Pone el icono del mapa según esl estado de ocupación
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
                mMap.addMarker(new MarkerOptions().
                        position(parqueaderoLarLng).
                        title(String.valueOf(parqueadero.getIdParqueadero())).
                        icon(iconMap));
            }
        }
    }

    // Retorna un valor numerico de 1 a 5 según la ocupación del parqueadero 1 muy pocos cupos disponibles y 5 muchos cupos disponibles
    public int obtenerEstadoParqueadero(int cuposTotales, int cuposDisponibles){

        int estado = 0; // Retorna estado por defecto -> color azul

        if(0 <= cuposDisponibles && cuposDisponibles <= (cuposTotales * 0.1)){ // Retorna rojo (muy pocos cupos) 0 - 10%
            estado = 1;
        }else if((cuposTotales * 0.11) < cuposDisponibles && cuposDisponibles <= (cuposTotales * 0.3)){ // Retorna rosa (pocos cupos) 11 - 30%
            estado = 2;
        }else if((cuposTotales * 0.31) < cuposDisponibles && cuposDisponibles <= (cuposTotales * 0.5)){ // Retorna naranja (algunos cupos) 31 - 50%
            estado = 3;
        }else if((cuposTotales * 0.51) < cuposDisponibles && cuposDisponibles <= (cuposTotales * 0.8)){ // Retorna verde claro (suficientes cupos) 51 - 80%
            estado = 4;
        }else if((cuposTotales * 0.81) < cuposDisponibles && cuposDisponibles <= cuposTotales){ // Retorna verde oscuro (muchos cupos) 81 - 100%
            estado = 5;
        }else{
            estado = 0;
        }
        return estado;
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            // Se deshabilita el botón para ir atrás y no se permite ir al inicio de sesión
            // super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_recargar) {

            // Recarga las peticiones al servidor de parqueaderos
            TareaAsincronaPeticionParqueaderos myAsyncTasks = new TareaAsincronaPeticionParqueaderos();
            myAsyncTasks.execute();
            // Se vuelve a centrar el mapa teniendo en cuenta mi ubicación
            miUbicacion(mMap);
            //
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_preferencias) {
            // Ir a preferencias del usuario
            Intent intent = new Intent(MainActivity.this, PreferenciasActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_cerrar_sesion) {

            // Se muestra un dialogo de alerta que indica el posible cierre de sesión

            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            // Add the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK button
                    Intent homeIntent = new Intent(MainActivity.this, LoginActivity.class);
                    startActivity(homeIntent);
                }
            });
            builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                    dialog.dismiss();
                }
            });
            // Set other dialog properties

            builder.setMessage(R.string.mensaje_dialogo_cerrar_sesión)
                    .setTitle("");


            // Create the AlertDialog
            AlertDialog dialog = builder.create();
            dialog.show();
        }

        // FIN del dialogo de cierre de sesión

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // Escuchador de la presión de on click de los marcadores
    @Override
    public boolean onMarkerClick(Marker marker) {
        //Toast.makeText(MainActivity.this, "marker: " + marker.getTitle(), Toast.LENGTH_SHORT).show();
        marker.hideInfoWindow(); // se oculta la información del marker
        verResumenParqueadero(marker.getTitle());
        return true;
    }

    // Metodo que despliega un dialogo personalizado con la información del parqueadero
    public void verResumenParqueadero(String idParqueadero){
        int id = Integer.parseInt(idParqueadero);
        for(int p = 0; p < listaParqueaderosEncontrados.size();p++){
            DetallesParqueadero parqueadero = listaParqueaderosEncontrados.get(p);
            // Encontro el parqueadero, ahora muestre el dialogo
            if(id == parqueadero.getIdParqueadero()) {
                //Toast.makeText(MainActivity.this, "siiiii: " + id, Toast.LENGTH_SHORT).show();
                dialogoPersonalizadoResumenParqueadero();
                break;
            }

        }
    }

    // Dialogo personalizado con las caracteristicas del parqueadero
    public void dialogoPersonalizadoResumenParqueadero(){
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        // Get the layout inflater
        LayoutInflater inflater = MainActivity.this.getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(inflater.inflate(R.layout.item_list_view_parqueadero, null))
                // Add action buttons
                .setPositiveButton("Cerrar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // sign in the user ...
                        dialog.dismiss();
                    }
                });

        builder.setTitle("Características del Parqueadero");
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Tarea asíncrona para realizar la petición al servidor de parqueaderos
    private class TareaAsincronaPeticionParqueaderos extends AsyncTask<String, String, String> {

        private String response = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //Display progress bar
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage(getString(R.string.descargando_datos));
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();

            //Toast.makeText(MainActivity.this, "onPre", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(String... params) {
            HttpJsonParser jsonParser = new HttpJsonParser();
            response = jsonParser.makeHttpRequest(apiUrl,"GET",null);
            return response;
        }

        protected void onPostExecute(String result) {

            //Toast.makeText(MainActivity.this,"onPost", Toast.LENGTH_SHORT).show();

            pDialog.dismiss();

            runOnUiThread(new Runnable() {
                public void run() {

                    try {
                        if(mMap != null){
                            parqueaderoArray = new JSONArray(response);
                            listaParqueaderosTotal = new ArrayList<>();
                            listaParqueaderosEncontrados = new ArrayList<>();
                            for(int p = 0; p < parqueaderoArray.length(); p++){

                                DetallesParqueadero parqueadero = new DetallesParqueadero();
                                JSONObject arrayParqueadero = parqueaderoArray.getJSONObject(p);

                                parqueadero.setIdParqueadero(arrayParqueadero.getInt("idParqueadero"));
                                parqueadero.setCupos(arrayParqueadero.getInt("cupos"));
                                parqueadero.setCuposDiscapacitados(arrayParqueadero.getInt("cuposDiscapacitados"));
                                parqueadero.setCuposDisponibles(arrayParqueadero.getInt("cuposDisponibles"));


                                parqueadero.setDescripcion(arrayParqueadero.getString("descripcion"));
                                parqueadero.setDireccion(arrayParqueadero.getString("direccion"));
                                parqueadero.setCalle(arrayParqueadero.getString("calle"));
                                parqueadero.setCarrera(arrayParqueadero.getString("carrera"));
                                parqueadero.setBarrio(arrayParqueadero.getString("barrio"));
                                parqueadero.setCiudad(arrayParqueadero.getString("ciudad"));

                                parqueadero.setLatitud(arrayParqueadero.getDouble("latitud"));
                                parqueadero.setLongitud(arrayParqueadero.getDouble("longitud"));

                                parqueadero.setValorTarifaPlana(arrayParqueadero.getDouble("valorTarifaPlana"));
                                parqueadero.setValorTarifaCarro(arrayParqueadero.getDouble("valorTarifaCarro"));
                                parqueadero.setValorTarifaMoto(arrayParqueadero.getDouble("valorTarifaMoto"));
                                parqueadero.setValorTarifaBici(arrayParqueadero.getDouble("valorTarifaBici"));

                                if(arrayParqueadero.getString("esSobrecupo").equals("1")){
                                    parqueadero.setEsSobrecupo(1);
                                }else if(arrayParqueadero.getString("esSobrecupo").equals("0")){
                                    parqueadero.setEsSobrecupo(0);
                                }else{
                                    parqueadero.setEsSobrecupo(0);
                                }

                                if(arrayParqueadero.getString("esTarifaPlana").equals("1")){
                                    parqueadero.setEsTarifaPlana(1);
                                }else if(arrayParqueadero.getString("esTarifaPlana").equals("0")){
                                    parqueadero.setEsTarifaPlana(0);
                                }else{
                                    parqueadero.setEsTarifaPlana(0);
                                }

                                listaParqueaderosTotal.add(parqueadero);

                                // Filtrar y seleccionar en una nueva lista según preferencias de usuario
                                recuperarPreferenciasDeUsuario_Parqueaderos(); // Se recuperan las preferencias del usuario

                                // Se procede a calcular distancia en linea recta (G(s) algorítmo voraz)
                                double d_km = (double)(cercania / 1000.0);
                                double d;

                                // Calculo de la distancia en linea recta por medio de coordenadas Lat - Lng
                                d = (6371 * Math.acos( Math.cos(Math.toRadians(miLatitud)) * Math.cos(Math.toRadians(parqueadero.getLatitud())) * Math.cos( Math.toRadians(parqueadero.getLongitud()) - Math.toRadians(miLongitud)) + Math.sin(Math.toRadians(miLatitud)) * Math.sin( Math.toRadians(parqueadero.getLatitud()))));

                                // Verifica que el parqueadero encontrado se encuentre en la distancia establecida como aceptable por el usuario
                                if(d <= d_km) {
                                    // verificar otras caracteristicas de la busqueda ... precio, ofertas, servicio y dejar llaves

                                    listaParqueaderosEncontrados.add(parqueadero);
                                }
                            }

                            // Ver todos los parqueaderos en mapa
                            // verTodosLosParqueaderos(listaParqueaderosTotal);

                            // Ver todos los parqueaderos encontrados según prefrencias de usuario
                            if(listaParqueaderosEncontrados.size() == 0){
                                Toast.makeText(MainActivity.this,
                                                R.string.parqueaderos_no_encontrados,
                                                Toast.LENGTH_LONG)
                                                .show();
                            }else{
                                verTodosLosParqueaderos(listaParqueaderosEncontrados);
                                Toast.makeText(MainActivity.this,"Encontrados: " + String.valueOf(listaParqueaderosEncontrados.size()), Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        // Muestra error de conexión al no poder traer los datos
                        Toast.makeText(MainActivity.this,
                                R.string.error_conexion_servidor,
                                Toast.LENGTH_LONG)
                                .show();
                        Log.e("Exception", "Error al trabajar con el JSONArray " + e.toString());
                    }
                }
            });
        }
    }

}

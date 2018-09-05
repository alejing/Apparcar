package com.proyecto.android.apparcar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
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
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    // GoogleMap mMapView;
    private GoogleMap mMap;
    private MapView mMapView;
    private static final String MAPVIEW_BUNDLE_KEY = "MapViewBundleKey";

    // URL API Parqueaderos
    private String apiUrl = "http://192.168.0.8/apparcar/getParqueaderos.php";
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

    //
    private List<DetallesParqueadero> listaParqueaderosTotal;
    private List<DetallesParqueadero> listaParqueaderosEncontrados;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
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

        Toast.makeText(MainActivity.this, pref, Toast.LENGTH_LONG).show();
        */
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Gusrada las configuraciones del mapa
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
        // Agregó mi ubicación en un marcador negro en forma de carro
        miUbicacion(map);

        // Descarga asíncrona de parqueaderos según petición de usuario
        //TareaAsincronaPeticionParqueaderos myAsyncTasks = new TareaAsincronaPeticionParqueaderos();
        //myAsyncTasks.execute();

        // 0. Revisar como poner el mapa fuera de esta función, en la AsynTask .......
        // 1. Hay que iterar el JSONArray para convertirlo en una lista con el fin de pasarla vista a vista
        // 2. Agregar los parqueaderos mas cercanos según preferencias del usuario
        // 3. Visualizar los parqueaderos según el estado que trae de la base de datos real (simulación)
        /*
        try {
            if(parqueaderoArray != null){
                for(int p = 0; p < parqueaderoArray.length(); p++){
                    JSONObject parqueadero = null;

                    parqueadero = parqueaderoArray.getJSONObject(p);

                    LatLng parqueaderoLarLng = new LatLng(parqueadero.getDouble("latitud"), parqueadero.getDouble("longitud"));

                    map.addMarker(new MarkerOptions().
                            position(parqueaderoLarLng).
                            title(String.valueOf(parqueadero.getInt("idParqueadero")))
                            .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_map_parqueadero_azul)));
                }
            }else{
                mMap = map;
                Toast.makeText(MainActivity.this,
                        "Aquí hay un error al ir atrás en la app",
                        Toast.LENGTH_LONG).show();
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        */
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

    // Método para todos los parqueaderos
    public void verTodosLosParqueaderos(List<DetallesParqueadero> list){
        if(mMap != null) {
            for (int i = 0; i < list.size(); i++) {
                DetallesParqueadero parqueadero;
                parqueadero = list.get(i);

                LatLng parqueaderoLarLng = new LatLng(parqueadero.getLatitud(), parqueadero.getLongitud());

                mMap.addMarker(new MarkerOptions().
                        position(parqueaderoLarLng).
                        title(String.valueOf(parqueadero.getIdParqueadero()))
                        .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_map_parqueadero_azul)));
            }
        }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            // 4. Aquí falta poner el mensaje y códio si desea salir de la sesión, si es así cerrarla

            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory( Intent.CATEGORY_HOME );
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);

            //
            super.onBackPressed();
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
            // Preguntar y cerrar sesión
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    // Tarea asíncrona para realizar la petición al servidor de parqueaderos
    private class TareaAsincronaPeticionParqueaderos extends AsyncTask<String, String, String> {

        private String response;

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

                                if(arrayParqueadero.getString("esSobrecupo").equals("0")){
                                    parqueadero.setEsSobrecupo(false);
                                }else if(arrayParqueadero.getString("esSobrecupo").equals("1")){
                                    parqueadero.setEsSobrecupo(true);
                                }else{
                                    parqueadero.setEsSobrecupo(false);
                                }

                                if(arrayParqueadero.getString("esTarifaPlana").equals("0")){
                                    parqueadero.setEsTarifaPlana(false);
                                }else if(arrayParqueadero.getString("esTarifaPlana").equals("1")){
                                    parqueadero.setEsTarifaPlana(true);
                                }else{
                                    parqueadero.setEsTarifaPlana(false);
                                }

                                listaParqueaderosTotal.add(parqueadero);

                                // Filtrar y seleccionar en una nueva lista según preferencias de usuario
                                recuperarPreferenciasDeUsuario_Parqueaderos(); // Se recuperan las preferencias del usuario
                                // Se procede a calcular distancia en linea recta (G(s) algorítmo voraz)
                                double d_km = (double)(cercania / 1000.0);
                                double d;

                                d = (6371 * Math.acos( Math.cos(Math.toRadians(miLatitud)) * Math.cos(Math.toRadians(parqueadero.getLatitud())) * Math.cos( Math.toRadians(parqueadero.getLongitud()) - Math.toRadians(miLongitud)) + Math.sin(Math.toRadians(miLatitud)) * Math.sin( Math.toRadians(parqueadero.getLatitud()))));

                                if(d <= d_km) {
                                    listaParqueaderosEncontrados.add(parqueadero);
                                }
                            }

                            // Ver todos los parqueaderos en mapa
                            // verTodosLosParqueaderos(listaParqueaderosTotal);

                            // ver todos los parqueaderos encontrados según prefrencias de usuario
                            if(listaParqueaderosEncontrados.size() == 0){
                                Toast.makeText(MainActivity.this,
                                                "No se han encontrado Parqueaderos con las características deseadas.",
                                                Toast.LENGTH_LONG)
                                                .show();
                            }else{
                                verTodosLosParqueaderos(listaParqueaderosEncontrados);
                                Toast.makeText(MainActivity.this,"Encontrados: " + String.valueOf(listaParqueaderosEncontrados.size()), Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e("Exception", "Error al trabajar con el JSONArray " + e.toString());
                    }


                    /*
                    ListView listView =(ListView)findViewById(R.id.employeeList);
                    if (success == 1) {
                        try {
                            JSONArray employeeArray =  response.getJSONArray(KEY_DATA);
                            List<EmployeeDetails> employeeList = new ArrayList<>();
                            //Populate the EmployeeDetails list from response
                            for (int i = 0; i<employeeArray.length();i++){
                                EmployeeDetails employeeDetails = new EmployeeDetails();
                                JSONObject employeeObj = employeeArray.getJSONObject(i);
                                employeeDetails.setEmployeeId(employeeObj.getInt(KEY_EMPLOYEE_ID));
                                employeeDetails.setName(employeeObj.getString(KEY_NAME));
                                employeeDetails.setDob(employeeObj.getString(KEY_DOB));
                                employeeDetails.setDesignation(employeeObj.getString(KEY_DESIGNATION));
                                employeeDetails.setContactNumber(employeeObj.getString(KEY_CONTACT_NUMBER));
                                employeeDetails.setEmail(employeeObj.getString(KEY_EMAIL));
                                employeeDetails.setSalary(employeeObj.getString(KEY_SALARY));
                                employeeList.add(employeeDetails);
                            }
                            //Create an adapter with the EmployeeDetails List and set it to the LstView
                            adapter = new EmployeeAdapter(employeeList,getApplicationContext());
                            listView.setAdapter(adapter);


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    } else {
                        Toast.makeText(MainActivity.this,
                                "Some error occurred while loading data",
                                Toast.LENGTH_LONG).show();

                    }*/

                }
            });
        }
    }

}

package com.proyecto.android.apparcar;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class AdvancedCustomArrayAdapter extends ArrayAdapter<DetallesParqueadero> {

    private  Activity context;
    private  List<DetallesParqueadero> parqueaderos = null;


    public AdvancedCustomArrayAdapter(Activity context, List<DetallesParqueadero> parqueaderos) {

        super(context, R.layout.item_list_view_parqueadero, parqueaderos);
        this.context = context;
        this.parqueaderos = parqueaderos;

    }

    static class ViewContainer{

        public ImageView iv_icono_estado_parqueadero;
        public TextView tv_cupos_totales;
        public TextView tv_cupos_disponibles;
        public TextView tv_tarifa_plana;
        public TextView tv_valor_tarifa_plana;
        public TextView tv_dejar_laves;
        public TextView tv_direccion;
        public TextView tv_v_tarifa_carro;
        public TextView tv_v_tarifa_moto;
        public TextView tv_v_tarifa_bici;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent){

        ViewContainer viewContainer;
        View rowView = view;
        // Log.d("AdvancedView",String.valueOf(position));
        if(rowView == null){
            //Log.d("AdvancedView","Nueva");
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.item_list_view_parqueadero, parent, false);

            viewContainer = new ViewContainer();

            viewContainer.iv_icono_estado_parqueadero = (ImageView) rowView.findViewById(R.id.iv_item_list_view_parqueaderos);
            viewContainer.tv_cupos_totales = (TextView) rowView.findViewById(R.id.tv_item_list_view_parqueaderos_cupos_totales);
            viewContainer.tv_cupos_disponibles = (TextView) rowView.findViewById(R.id.tv_item_list_view_parqueaderos_cupos_disponibles);
            viewContainer.tv_tarifa_plana = (TextView) rowView.findViewById(R.id.tv_item_list_view_parqueaderos_tarifa_plana);
            viewContainer.tv_valor_tarifa_plana = (TextView) rowView.findViewById(R.id.tv_item_list_view_parqueaderos_valor_tarifa_plana);
            viewContainer.tv_dejar_laves = (TextView) rowView.findViewById(R.id.tv_item_list_view_parqueaderos_dejar_llaves);
            viewContainer.tv_direccion = (TextView) rowView.findViewById(R.id.tv_item_list_view_parqueaderos_direccion);
            viewContainer.tv_v_tarifa_carro = (TextView) rowView.findViewById(R.id.tv_item_list_view_v_carro);
            viewContainer.tv_v_tarifa_moto = (TextView) rowView.findViewById(R.id.tv_item_list_view_v_moto);
            viewContainer.tv_v_tarifa_bici = (TextView) rowView.findViewById(R.id.tv_item_list_view_v_bici);

            //viewContainer.iconDog    = (ImageView) rowView.findViewById(R.id.icon);

            rowView.setTag(viewContainer);

        }else{
            //Log.d("AdvancedView","Reciclo");
            viewContainer = (ViewContainer) rowView.getTag();
        }

        // Pone en detalle los valores de cada celda del listView

        viewContainer.tv_cupos_totales.setText("Cupos Totales: " + String.valueOf(parqueaderos.get(position).getCupos()));
        viewContainer.tv_cupos_disponibles.setText("Cupos Disponibles: " + String.valueOf(parqueaderos.get(position).getCuposDisponibles()));

        if(parqueaderos.get(position).getTarifaPlana() == 1){
            viewContainer.tv_tarifa_plana.setText("Tarifa Plana: SI");
            viewContainer.tv_valor_tarifa_plana.setText("Valor Tarifa Plana: $" + String.valueOf(parqueaderos.get(position).getValorTarifaPlana()));
        }else{
            viewContainer.tv_tarifa_plana.setText("Tarifa Plana: NO");
            viewContainer.tv_valor_tarifa_plana.setText("Valor Tarifa Plana: $ -");
        }

        if(parqueaderos.get(position).getEsSobrecupo() == 1){
            viewContainer.tv_dejar_laves.setText("Posibilidad de dejar Llaves: SI");
        }else{
            viewContainer.tv_dejar_laves.setText("Posibilidad de dejar Llaves: NO");
        }

        viewContainer.tv_direccion.setText("Dirección: " + String.valueOf(parqueaderos.get(position).getDireccion()));

        if(parqueaderos.get(position).getValorTarifaCarro() < 0){
            viewContainer.tv_v_tarifa_carro.setText("$ -");
        }else{
            viewContainer.tv_v_tarifa_carro.setText("$" + String.valueOf(parqueaderos.get(position).getValorTarifaCarro()));
        }

        if(parqueaderos.get(position).getValorTarifaMoto() < 0){
            viewContainer.tv_v_tarifa_moto.setText("$ -");
        }else{
            viewContainer.tv_v_tarifa_moto.setText("$" + String.valueOf(parqueaderos.get(position).getValorTarifaMoto()));
        }

        if(parqueaderos.get(position).getValorTarifaBici() < 0){
            viewContainer.tv_v_tarifa_bici.setText("$ -");
        }else{
            viewContainer.tv_v_tarifa_bici.setText("$" + String.valueOf(parqueaderos.get(position).getValorTarifaBici()));
        }

        // Pone el icono de la lista según esl estado de ocupación
        switch(obtenerEstadoParqueadero(parqueaderos.get(position).getCupos(), parqueaderos.get(position).getCuposDisponibles())){
            case 0:
                viewContainer.iv_icono_estado_parqueadero.setImageResource(R.mipmap.ic_map_parqueadero_azul);
                break;
            case 1:
                viewContainer.iv_icono_estado_parqueadero.setImageResource(R.mipmap.ic_map_parqueadero_rojo);
                break;
            case 2:
                viewContainer.iv_icono_estado_parqueadero.setImageResource(R.mipmap.ic_map_parqueadero_rosa);
                break;
            case 3:
                viewContainer.iv_icono_estado_parqueadero.setImageResource(R.mipmap.ic_map_parqueadero_naranja);
                break;
            case 4:
                viewContainer.iv_icono_estado_parqueadero.setImageResource(R.mipmap.ic_map_parqueadero_verde_claro);
                break;
            case 5:
                viewContainer.iv_icono_estado_parqueadero.setImageResource(R.mipmap.ic_map_parqueadero_verde_oscuro);
                break;
        }

        return rowView;
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
}


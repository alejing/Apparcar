package com.proyecto.android.apparcar;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

        public TextView tv_id;
        public TextView tv_direccion;
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

            viewContainer.tv_id = (TextView) rowView.findViewById(R.id.tv_item_list_view_parqueaderos_id);
            viewContainer.tv_direccion = (TextView) rowView.findViewById(R.id.tv_item_list_view_parqueaderos_direccion);
            //viewContainer.iconDog    = (ImageView) rowView.findViewById(R.id.icon);

            rowView.setTag(viewContainer);

        }else{
            //Log.d("AdvancedView","Reciclo");
            viewContainer = (ViewContainer) rowView.getTag();
        }

        viewContainer.tv_id.setText(String.valueOf(parqueaderos.get(position).getIdParqueadero()));
        viewContainer.tv_direccion.setText(String.valueOf(parqueaderos.get(position).getDireccion()));
        //viewContainer.iconDog.setImageResource(imgIDs[position]);

        return rowView;
    }
}


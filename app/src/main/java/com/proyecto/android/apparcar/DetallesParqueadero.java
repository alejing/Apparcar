package com.proyecto.android.apparcar;

import android.os.Parcel;
import android.os.Parcelable;

public class DetallesParqueadero implements Parcelable {

    private int idParqueadero, cupos, cuposDiscapacitados, cuposDisponibles;
    private String descripcion, direccion, calle, carrera, barrio, ciudad;
    private double latitud, longitud;
    private int esSobrecupo, esTarifaPlana;
    private double valorTarifaPlana, valorTarifaCarro, valorTarifaMoto, valorTarifaBici;

    public DetallesParqueadero() {
    }

    public int getIdParqueadero() {
        return idParqueadero;
    }

    public void setIdParqueadero(int idParqueadero) {
        this.idParqueadero = idParqueadero;
    }

    public int getCupos() {
        return cupos;
    }

    public void setCupos(int cupos) {
        this.cupos = cupos;
    }

    public int getCuposDiscapacitados() {
        return cuposDiscapacitados;
    }

    public void setCuposDiscapacitados(int cuposDiscapacitados) {
        this.cuposDiscapacitados = cuposDiscapacitados;
    }

    public int getCuposDisponibles() {
        return cuposDisponibles;
    }

    public void setCuposDisponibles(int cuposDisponibles) {
        this.cuposDisponibles = cuposDisponibles;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getCalle() {
        return calle;
    }

    public void setCalle(String calle) {
        this.calle = calle;
    }

    public String getCarrera() {
        return carrera;
    }

    public void setCarrera(String carrera) {
        this.carrera = carrera;
    }

    public String getBarrio() {
        return barrio;
    }

    public void setBarrio(String barrio) {
        this.barrio = barrio;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

    public int isEsSobrecupo() {
        return esSobrecupo;
    }

    public void setEsSobrecupo(int esSobrecupo) {
        this.esSobrecupo = esSobrecupo;
    }

    public int isEsTarifaPlana() {
        return esTarifaPlana;
    }

    public void setEsTarifaPlana(int esTarifaPlana) {
        this.esTarifaPlana = esTarifaPlana;
    }

    public double getValorTarifaPlana() {
        return valorTarifaPlana;
    }

    public void setValorTarifaPlana(double valorTarifaPlana) {
        this.valorTarifaPlana = valorTarifaPlana;
    }

    public double getValorTarifaCarro() {
        return valorTarifaCarro;
    }

    public void setValorTarifaCarro(double valorTarifaCarro) {
        this.valorTarifaCarro = valorTarifaCarro;
    }

    public double getValorTarifaMoto() {
        return valorTarifaMoto;
    }

    public void setValorTarifaMoto(double valorTarifaMoto) {
        this.valorTarifaMoto = valorTarifaMoto;
    }

    public double getValorTarifaBici() {
        return valorTarifaBici;
    }

    public void setValorTarifaBici(double valorTarifaBici) {
        this.valorTarifaBici = valorTarifaBici;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeInt(idParqueadero);
        dest.writeInt(cupos);
        dest.writeInt(cuposDiscapacitados);
        dest.writeInt(cuposDisponibles);

        dest.writeString(descripcion);
        dest.writeString(direccion);
        dest.writeString(calle);
        dest.writeString(carrera);
        dest.writeString(barrio);
        dest.writeString(ciudad);

        dest.writeDouble(latitud);
        dest.writeDouble(longitud);
        dest.writeDouble(valorTarifaPlana);
        dest.writeDouble(valorTarifaCarro);
        dest.writeDouble(valorTarifaMoto);
        dest.writeDouble(valorTarifaBici);

        dest.writeInt(esSobrecupo);
        dest.writeInt(esTarifaPlana);

    }

    public DetallesParqueadero(Parcel in) {

        idParqueadero = in.readInt();
        cupos = in.readInt();
        cuposDiscapacitados = in.readInt();
        cuposDisponibles = in.readInt();

        descripcion = in.readString();
        direccion = in.readString();
        calle = in.readString();
        carrera = in.readString();
        barrio = in.readString();
        ciudad = in.readString();

        latitud = in.readDouble();
        longitud = in.readDouble();
        valorTarifaPlana = in.readDouble();
        valorTarifaCarro = in.readDouble();
        valorTarifaMoto = in.readDouble();
        valorTarifaBici = in.readDouble();

        esSobrecupo = in.readInt();
        esTarifaPlana = in.readInt();
    }

    public static final Parcelable.Creator<DetallesParqueadero> CREATOR = new Parcelable.Creator<DetallesParqueadero>() {
        public DetallesParqueadero createFromParcel(Parcel in) {
            return new DetallesParqueadero(in);
        }

        public DetallesParqueadero[] newArray(int size) {
            return new DetallesParqueadero[size];
        }
    };
}

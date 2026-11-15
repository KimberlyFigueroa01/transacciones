package uptc.banco.transacciones.Model;

import java.util.List;

public class Usuario {

    private int id;
    private String nombre;
    private String apellido;
    private String telefono;
    private double saldo;
    private List<Movimiento> historial;

    public Usuario(){

    }

    public Usuario(int id, String nombre, String apellido, String telefono, double saldo, List<Movimiento> historial){
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.telefono = telefono;
        this.saldo = saldo;
        this.historial = historial;  
    }  

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public double getSaldo() {
        return saldo;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }

    public List<Movimiento> getHistorial() {
        return historial;
    }

    public void setHistorial(List<Movimiento> historial) {
        this.historial = historial;
    }

    
    
}

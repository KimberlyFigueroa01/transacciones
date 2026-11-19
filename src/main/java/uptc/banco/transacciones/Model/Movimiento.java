package uptc.banco.transacciones.Model;

import java.util.Date;

public class Movimiento {

    private Date fecha;
    private String tipo;
    private double monto;
    private String estado;
    private double saldoResultante;
    /** Descripción libre: destino de la transferencia, servicio pagado, motivo del rechazo... */
    private String detalle;

public Movimiento(){

}

public Movimiento(Date fecha, String tipo, double monto, String estado, double saldoResultante){
    this(fecha, tipo, monto, estado, saldoResultante, null);
}

public Movimiento(Date fecha, String tipo, double monto, String estado, double saldoResultante, String detalle){
    this.fecha = fecha;
    this.tipo = tipo;
    this.monto = monto;
    this.estado = estado;
    this.saldoResultante = saldoResultante;
    this.detalle = detalle;
}

public String getDetalle() {
    return detalle;
}

public void setDetalle(String detalle) {
    this.detalle = detalle;
}

public Date getFecha(){return this.fecha;
}

public void setFecha(Date fecha) {
    this.fecha = fecha;
}

public String getTipo() {
    return tipo;
}

public void setTipo(String tipo) {
    this.tipo = tipo;
}

public double getMonto() {
    return monto;
}

public void setMonto(double monto) {
    this.monto = monto;
}

public String getEstado() {
    return estado;
}

public void setEstado(String estado) {
    this.estado = estado;
}

public double getSaldoResultante() {
    return saldoResultante;
}

public void setSaldoResultante(double saldoResultante) {
    this.saldoResultante = saldoResultante;
}

@Override
public String toString() {
    return "Movimiento [fecha=" + fecha + ", tipo=" + tipo + ", monto=" + monto + ", estado=" + estado
            + ", saldoResultante=" + saldoResultante + ", detalle=" + detalle + "]";
}

    
}

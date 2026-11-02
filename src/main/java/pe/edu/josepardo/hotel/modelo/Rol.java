package pe.edu.josepardo.hotel.modelo;

public enum Rol {
    CLIENTE("Cliente"), TRABAJADOR("Trabajador"), ADMINISTRADOR("Administrador");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}

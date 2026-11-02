package pe.edu.josepardo.hotel.modelo;

public enum Actor {
    CLIENTE("Cliente"), TRABAJADOR("Trabajador"), ADMINISTRADOR("Administrador"), SISTEMA("Sistema");

    private final String etiqueta;

    Actor(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static Actor desde(Rol rol) {
        return valueOf(rol.name());
    }
}

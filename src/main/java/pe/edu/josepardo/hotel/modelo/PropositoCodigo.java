package pe.edu.josepardo.hotel.modelo;

public enum PropositoCodigo {
    RECUPERAR_CONTRASENA("Recuperar contraseña");

    private final String etiqueta;

    PropositoCodigo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}

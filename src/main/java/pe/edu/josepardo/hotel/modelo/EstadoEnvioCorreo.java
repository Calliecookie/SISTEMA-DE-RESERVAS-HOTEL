package pe.edu.josepardo.hotel.modelo;

public enum EstadoEnvioCorreo {
    PENDIENTE("Pendiente"), ENVIADO("Enviado"), FALLIDO("Fallido"), OMITIDO("Correo desactivado");

    private final String etiqueta;

    EstadoEnvioCorreo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}

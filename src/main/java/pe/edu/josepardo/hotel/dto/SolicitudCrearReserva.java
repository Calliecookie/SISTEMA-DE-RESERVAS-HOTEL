package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.Size;

public record SolicitudCrearReserva(
        @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres.") String observaciones) {
}

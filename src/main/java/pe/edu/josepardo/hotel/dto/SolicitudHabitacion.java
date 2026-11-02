package pe.edu.josepardo.hotel.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;

public record SolicitudHabitacion(
        @NotBlank(message = "Ingresa el número de habitación.") @Size(max = 10, message = "El número no puede superar 10 caracteres.") String numero,
        @NotBlank(message = "Ingresa el nombre.") @Size(max = 120, message = "El nombre no puede superar 120 caracteres.") String nombre,
        @NotNull(message = "Selecciona una categoría.") Long categoriaId,
        @NotBlank(message = "Ingresa la descripción.") @Size(max = 600, message = "La descripción no puede superar 600 caracteres.") String descripcion,
        @NotNull(message = "Ingresa la capacidad.") @Min(value = 1, message = "La capacidad mínima es 1.") @Max(value = 10, message = "La capacidad máxima es 10.") Integer capacidad,
        @NotNull(message = "Ingresa el precio por noche.") @DecimalMin(value = "1.00", message = "El precio debe ser mayor a cero.") @Digits(integer = 8, fraction = 2, message = "El precio no es válido.") BigDecimal precioNoche,
        @NotBlank(message = "Selecciona el estado operativo.") String estadoOperativo,
        @Size(max = 500, message = "La URL de imagen es demasiado larga.") String imagenUrl,
        Set<Long> caracteristicaIds) {
}

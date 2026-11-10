package pe.edu.josepardo.hotel.dto;

import java.util.List;

public record PisoDto(String piso, List<PuertaDto> habitaciones) {
}

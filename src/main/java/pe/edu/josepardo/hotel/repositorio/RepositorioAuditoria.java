package pe.edu.josepardo.hotel.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.josepardo.hotel.modelo.RegistroAuditoria;

public interface RepositorioAuditoria extends JpaRepository<RegistroAuditoria, Long> {
}

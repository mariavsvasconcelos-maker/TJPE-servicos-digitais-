package br.org.cesar.tjpe.validador.repository;

import br.org.cesar.tjpe.validador.domain.model.ProcuracaoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProcuracaoDocumentoRepository extends JpaRepository<ProcuracaoDocumento, Long> {
    List<ProcuracaoDocumento> findAllByOrderByDataUploadDesc();
}

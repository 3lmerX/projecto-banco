package mz.banco.transaccoes;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransaccaoRepository extends JpaRepository<Transaccao, Long> {

    @Query("select t from Transaccao t "
            + "where (t.contaOrigem = :numero or t.contaDestino = :numero) "
            + "and t.estado = '" + Transaccao.CONCLUIDA + "' "
            + "order by t.dataHora desc, t.id desc")
    List<Transaccao> extracto(@Param("numero") String numero, Pageable pageable);
}
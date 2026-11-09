package mz.banco.contas;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface ContaRepository extends JpaRepository<Conta, Long> {

    List<Conta> findByClienteId(Long clienteId);

    boolean existsByNumero(String numero);

    Optional<Conta> findByNumero(String numero);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conta c where c.numero = :numero")
    Optional<Conta> buscarParaAlterar(@Param("numero") String numero);

    @Query("""
            select c from Conta c
            where lower(c.numero) like lower(concat('%', :q, '%'))
               or lower(c.cliente.nome) like lower(concat('%', :q, '%'))
            order by c.id
            """)
    List<Conta> pesquisar(@Param("q") String q);
}
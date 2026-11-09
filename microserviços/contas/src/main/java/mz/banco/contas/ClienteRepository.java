package mz.banco.contas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository <Cliente, Long> {
    boolean existsByNumeroBi(String numeroBi);

    boolean existsByNumeroBiAndIdNot(String numeroBi, Long id);

    @Query("""
            select c from Cliente c
            where lower(c.nome) like lower(concat('%', :q, '%'))
               or lower(c.numeroBi) like lower(concat('%', :q, '%'))
               or lower(c.telefone) like lower(concat('%', :q, '%'))
               or lower(c.email) like lower(concat('%', :q, '%'))
            """)
    java.util.List<Cliente> pesquisar(@Param("q") String q);
}

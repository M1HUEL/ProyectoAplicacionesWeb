package com.itson.ProyectoAplicacionesWeb.repository;


import com.itson.ProyectoAplicacionesWeb.model.MovimientoInventario;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    /**
     * Recupera los ajustes de una presentación comercial,
     * del más reciente al más antiguo.
     *
     * @param formatoId identificador de la presentación.
     * @param limite    cantidad máxima de registros (Pageable).
     */
    @Query("""
            SELECT m
            FROM MovimientoInventario m
            WHERE m.itemStock.formatoLibro.id = :formatoId
            ORDER BY m.fecha DESC, m.id DESC
            """)
    List<MovimientoInventario> listarPorFormato(
            @Param("formatoId") Long formatoId,
            Pageable limite);
}

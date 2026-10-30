package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.AjusteInventarioDTO;
import com.itson.ProyectoAplicacionesWeb.model.FormatoLibro;
import com.itson.ProyectoAplicacionesWeb.model.Formato;
import com.itson.ProyectoAplicacionesWeb.model.ItemStock;
import com.itson.ProyectoAplicacionesWeb.model.MotivoAjuste;
import com.itson.ProyectoAplicacionesWeb.model.MovimientoInventario;
import com.itson.ProyectoAplicacionesWeb.repository.FormatoLibroRepository;
import com.itson.ProyectoAplicacionesWeb.repository.ItemStockRepository;
import com.itson.ProyectoAplicacionesWeb.repository.MovimientoInventarioRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class InventarioService {
    /** Máximo de unidades que puede mover un solo ajuste. */
    static final int DIFERENCIA_MAXIMA = 100_000;

    /** Longitud máxima de la nota (coincide con la columna). */
    static final int NOTA_MAXIMA = 255;

    /** Cantidad de movimientos que muestra la pantalla de ajuste. */
    static final int MOVIMIENTOS_VISIBLES = 50;

    private final ItemStockRepository itemStockRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final FormatoLibroRepository formatoLibroRepository;

    public InventarioService(
            ItemStockRepository itemStockRepository,
            MovimientoInventarioRepository movimientoRepository,
            FormatoLibroRepository formatoLibroRepository) {
        this.itemStockRepository = itemStockRepository;
        this.movimientoRepository = movimientoRepository;
        this.formatoLibroRepository = formatoLibroRepository;
    }

    /**
     * Recupera el inventario de una presentación junto con su libro.
     *
     * @throws IllegalArgumentException si la presentación no existe,
     *                                  es digital o no tiene inventario.
     */
    @Transactional(readOnly = true)
    public ItemStock buscarInventarioConLibro(Long formatoId) {
        validarIdentificador(formatoId);

        return itemStockRepository.buscarConLibroPorFormato(formatoId)
                .orElseThrow(() -> motivoSinInventario(formatoId));
    }

    /**
     * Lista los últimos ajustes de una presentación.
     */
    @Transactional(readOnly = true)
    public List<MovimientoInventario> listarMovimientos(Long formatoId) {
        validarIdentificador(formatoId);

        return movimientoRepository.listarPorFormato(
                formatoId,
                PageRequest.of(0, MOVIMIENTOS_VISIBLES)
        );
    }

    /**
     * Aplica un ajuste al inventario y registra el movimiento.
     *
     * Reglas:
     * - la diferencia es obligatoria y distinta de cero;
     * - REPOSICION solo admite diferencias positivas;
     * - MERMA solo admite diferencias negativas;
     * - el inventario nunca puede quedar en negativo.
     *
     * @param formatoId   presentación física que se ajusta.
     * @param ajuste      datos capturados en el formulario.
     * @param responsable correo del administrador autenticado.
     * @return movimiento registrado.
     */
    @Transactional
    public MovimientoInventario ajustar(Long formatoId,
                                        AjusteInventarioDTO ajuste,
                                        String responsable) {
        validarIdentificador(formatoId);
        validarAjuste(ajuste);

        if (responsable == null || responsable.isBlank()) {
            throw new IllegalArgumentException(
                    "No se pudo identificar al administrador que realiza el ajuste."
            );
        }

        ItemStock inventario = itemStockRepository
                .findByFormatoLibroId(formatoId)
                .orElseThrow(() -> motivoSinInventario(formatoId));

        int diferencia = ajuste.getDiferencia();
        int cantidadAnterior = inventario.getCantidad();
        long cantidadNueva = (long) cantidadAnterior + diferencia;

        if (cantidadNueva < 0) {
            throw new IllegalArgumentException(
                    "El ajuste dejaría el inventario en negativo. "
                            + "Existencias actuales: " + cantidadAnterior + "."
            );
        }

        if (cantidadNueva > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "El ajuste supera la cantidad máxima de existencias."
            );
        }

        inventario.setCantidad((int) cantidadNueva);

        MovimientoInventario movimiento = new MovimientoInventario(
                inventario,
                ajuste.getMotivo(),
                diferencia,
                cantidadAnterior,
                (int) cantidadNueva,
                normalizarNota(ajuste.getNota()),
                responsable.trim()
        );

        return movimientoRepository.save(movimiento);
    }

    private void validarAjuste(AjusteInventarioDTO ajuste) {
        if (ajuste == null) {
            throw new IllegalArgumentException(
                    "Los datos del ajuste son obligatorios."
            );
        }

        if (ajuste.getMotivo() == null) {
            throw new IllegalArgumentException(
                    "Debes seleccionar el motivo del ajuste."
            );
        }

        Integer diferencia = ajuste.getDiferencia();

        if (diferencia == null || diferencia == 0) {
            throw new IllegalArgumentException(
                    "La diferencia debe ser un número entero distinto de cero."
            );
        }

        if (Math.abs((long) diferencia) > DIFERENCIA_MAXIMA) {
            throw new IllegalArgumentException(
                    "Un ajuste no puede mover más de " + DIFERENCIA_MAXIMA + " unidades."
            );
        }

        if (ajuste.getMotivo() == MotivoAjuste.REPOSICION && diferencia < 0) {
            throw new IllegalArgumentException(
                    "Una reposición debe sumar unidades (diferencia positiva)."
            );
        }

        if (ajuste.getMotivo() == MotivoAjuste.MERMA && diferencia > 0) {
            throw new IllegalArgumentException(
                    "Una merma debe restar unidades (diferencia negativa)."
            );
        }

        if (ajuste.getNota() != null
                && ajuste.getNota().trim().length() > NOTA_MAXIMA) {
            throw new IllegalArgumentException(
                    "La nota no puede superar los " + NOTA_MAXIMA + " caracteres."
            );
        }
    }

    private String normalizarNota(String nota) {
        if (nota == null || nota.isBlank()) {
            return null;
        }
        return nota.trim();
    }

    private void validarIdentificador(Long formatoId) {
        if (formatoId == null || formatoId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador de la presentación no es válido."
            );
        }
    }

    /**
     * Explica por qué una presentación no tiene inventario ajustable.
     */
    private IllegalArgumentException motivoSinInventario(Long formatoId) {
        FormatoLibro formato = formatoLibroRepository
                .findById(formatoId)
                .orElse(null);

        if (formato == null) {
            return new IllegalArgumentException(
                    "La presentación comercial solicitada no existe."
            );
        }

        if (formato.getFormato() == Formato.DIGITAL) {
            return new IllegalArgumentException(
                    "Las presentaciones digitales no manejan inventario físico."
            );
        }

        return new IllegalArgumentException(
                "La presentación física no tiene inventario registrado."
        );
    }

}

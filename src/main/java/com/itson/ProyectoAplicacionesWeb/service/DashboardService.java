package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.DashboardResumenDTO;
import com.itson.ProyectoAplicacionesWeb.dto.ProductoMasVendidoDTO;
import com.itson.ProyectoAplicacionesWeb.dto.StockBajoDTO;
import com.itson.ProyectoAplicacionesWeb.model.EstadoOrden;
import com.itson.ProyectoAplicacionesWeb.model.FormatoLibro;
import com.itson.ProyectoAplicacionesWeb.model.ItemStock;
import com.itson.ProyectoAplicacionesWeb.model.Libro;
import com.itson.ProyectoAplicacionesWeb.repository.DetalleOrdenRepository;
import com.itson.ProyectoAplicacionesWeb.repository.ItemStockRepository;
import com.itson.ProyectoAplicacionesWeb.repository.LibroRepository;
import com.itson.ProyectoAplicacionesWeb.repository.OrdenCompraRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class DashboardService {
    static final int TOP_MAS_VENDIDOS = 5;

    private final LibroRepository libroRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final ItemStockRepository itemStockRepository;
    private final DetalleOrdenRepository detalleOrdenRepository;

    public DashboardService(
            LibroRepository libroRepository,
            OrdenCompraRepository ordenCompraRepository,
            ItemStockRepository itemStockRepository,
            DetalleOrdenRepository detalleOrdenRepository) {
        this.libroRepository = libroRepository;
        this.ordenCompraRepository = ordenCompraRepository;
        this.itemStockRepository = itemStockRepository;
        this.detalleOrdenRepository = detalleOrdenRepository;
    }

    /**
     * Construye el resumen del panel.
     *
     * @param desde primer día del periodo; si es null se usa el
     *              primer día del mes actual.
     * @param hasta último día del periodo (inclusive); si es null
     *              se usa la fecha de hoy.
     * @throws IllegalArgumentException si desde es posterior a hasta.
     */
    @Transactional(readOnly = true)
    public DashboardResumenDTO obtenerResumen(LocalDate desde, LocalDate hasta) {

        LocalDate hoy = LocalDate.now();
        LocalDate inicio = (desde != null) ? desde : hoy.withDayOfMonth(1);
        LocalDate fin = (hasta != null) ? hasta : hoy;

        if (inicio.isAfter(fin)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la fecha final."
            );
        }

        var inicioPeriodo = inicio.atStartOfDay();
        var finPeriodo = fin.atTime(LocalTime.MAX);

        long ordenesHoy = ordenCompraRepository.countByFechaBetween(
                hoy.atStartOfDay(),
                hoy.atTime(LocalTime.MAX)
        );

        long ordenesPendientes =
                ordenCompraRepository.countByEstado(EstadoOrden.PENDIENTE);

        BigDecimal ventas = ordenCompraRepository.sumarVentas(
                inicioPeriodo, finPeriodo, EstadoOrden.CANCELADO
        );

        if (ventas == null) {
            ventas = BigDecimal.ZERO;
        }

        List<StockBajoDTO> stockBajo = itemStockRepository
                .listarConStockBajo()
                .stream()
                .map(this::aStockBajoDTO)
                .toList();

        List<ProductoMasVendidoDTO> masVendidos = detalleOrdenRepository
                .listarMasVendidos(
                        inicioPeriodo,
                        finPeriodo,
                        EstadoOrden.CANCELADO,
                        PageRequest.of(0, TOP_MAS_VENDIDOS)
                );

        return new DashboardResumenDTO(
                libroRepository.count(),
                ordenesHoy,
                ordenesPendientes,
                ventas,
                inicio,
                fin,
                stockBajo,
                masVendidos
        );
    }

    private StockBajoDTO aStockBajoDTO(ItemStock inventario) {
        FormatoLibro formato = inventario.getFormatoLibro();
        Libro libro = formato.getLibro();

        return new StockBajoDTO(
                libro.getId(),
                formato.getId(),
                libro.getTitulo(),
                formato.getFormato().name(),
                inventario.getCantidad(),
                inventario.getStockMinimo()
        );
    }
}
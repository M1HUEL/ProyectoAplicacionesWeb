package com.itson.ProyectoAplicacionesWeb.dto;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


public record DashboardResumenDTO(long totalLibros,
                                  long ordenesHoy,
                                  long ordenesPendientes,
                                  BigDecimal ventasPeriodo,
                                  LocalDate desde,
                                  LocalDate hasta,
                                  List<StockBajoDTO> stockBajo,
                                  List<ProductoMasVendidoDTO> masVendidos) {
}

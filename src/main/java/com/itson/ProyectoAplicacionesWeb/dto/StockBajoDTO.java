package com.itson.ProyectoAplicacionesWeb.dto;

public record StockBajoDTO(Long libroId,
                           Long formatoId,
                           String titulo,
                           String formato,
                           int cantidad,
                           int stockMinimo) {
}

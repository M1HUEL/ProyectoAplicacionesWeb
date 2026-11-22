
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.PagoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @InjectMocks
    private PagoService pagoService;

    private OrdenCompra orden;

    @BeforeEach
    void configurar() {

        orden = mock(OrdenCompra.class);

        lenient().when(orden.getId()).thenReturn(10L);

        lenient().when(orden.getTotal())
                .thenReturn(new BigDecimal("499.00"));
    }

    @Test
    void registrarPago_datosValidos_creaPagoPendiente() {

        when(pagoRepository.existsByOrdenCompraId(10L))
                .thenReturn(false);

        when(pagoRepository.save(any(Pago.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0));

        Pago pago = pagoService.registrarPago(
                orden,
                MetodoPago.TRANSFERENCIA
        );

        assertNotNull(pago);
        assertEquals(orden, pago.getOrdenCompra());
        assertEquals(
                0,
                new BigDecimal("499.00")
                        .compareTo(pago.getMonto())
        );
        assertEquals(
                MetodoPago.TRANSFERENCIA,
                pago.getMetodo()
        );
        assertEquals(
                EstadoPago.PENDIENTE,
                pago.getEstado()
        );
        assertNotNull(pago.getFechaRegistro());

        verify(pagoRepository).save(any(Pago.class));
    }

    @Test
    void registrarPago_metodoNulo_lanzaExcepcion() {

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> pagoService.registrarPago(orden, null)
        );

        assertEquals(
                "Debe seleccionar un método de pago.",
                excepcion.getMessage()
        );

        verify(pagoRepository, never()).save(any());
    }

    @Test
    void registrarPago_montoInvalido_lanzaExcepcion() {

        when(orden.getTotal()).thenReturn(BigDecimal.ZERO);

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> pagoService.registrarPago(
                        orden,
                        MetodoPago.EFECTIVO
                )
        );

        assertEquals(
                "El monto del pago debe ser mayor que cero.",
                excepcion.getMessage()
        );

        verify(pagoRepository, never()).save(any());
    }

    @Test
    void registrarPago_pagoDuplicado_lanzaExcepcion() {

        when(pagoRepository.existsByOrdenCompraId(10L))
                .thenReturn(true);

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> pagoService.registrarPago(
                        orden,
                        MetodoPago.TARJETA
                )
        );

        assertEquals(
                "La orden ya tiene un registro de pago.",
                excepcion.getMessage()
        );

        verify(pagoRepository, never()).save(any());
    }

    /**
     * Comprueba que el servicio recupera el registro
     * de pago asociado a una orden existente.
     *
     * La consulta no debe modificar información
     * persistida ni generar otro registro de pago.
     */
    @Test
    void consultarPagoPorOrden_pagoExistente_retornaPago() {

        // 1. Preparar el identificador de la orden.
        Long ordenId = 10L;

        // 2. Simular un registro de pago existente.
        Pago pago = mock(Pago.class);

        when(pagoRepository.findByOrdenCompraId(ordenId))
                .thenReturn(Optional.of(pago));

        // 3. Ejecutar el método de consulta.
        Optional<Pago> resultado =
                pagoService.consultarPagoPorOrden(ordenId);

        // 4. Comprobar que se recuperó el registro esperado.
        assertTrue(resultado.isPresent());
        assertSame(pago, resultado.orElseThrow());

        // 5. Verificar la consulta realizada.
        verify(pagoRepository)
                .findByOrdenCompraId(ordenId);

        // 6. Comprobar que no se generaron operaciones
        // adicionales sobre el repositorio.
        verifyNoMoreInteractions(pagoRepository);
    }

    /**
     * Comprueba que una orden anterior al módulo
     * de pagos puede no tener un registro asociado.
     *
     * La ausencia de un pago no debe impedir
     * la consulta administrativa de la orden.
     */
    @Test
    void consultarPagoPorOrden_sinPago_retornaVacio() {

        Long ordenId = 1L;

        // Simular una orden sin registro de pago.
        when(pagoRepository.findByOrdenCompraId(ordenId))
                .thenReturn(Optional.empty());

        // Ejecutar la consulta.
        Optional<Pago> resultado =
                pagoService.consultarPagoPorOrden(ordenId);

        // Comprobar que no existe un pago asociado.
        assertTrue(resultado.isEmpty());

        verify(pagoRepository)
                .findByOrdenCompraId(ordenId);

        verifyNoMoreInteractions(pagoRepository);
    }

    /**
     * Comprueba que los identificadores inválidos
     * son rechazados antes de consultar el repositorio.
     *
     * Incluye identificadores nulos, iguales a cero
     * y negativos.
     */
    @Test
    void consultarPagoPorOrden_idInvalido_lanzaExcepcion() {

        // Validar un identificador nulo.
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> pagoService.consultarPagoPorOrden(null)
        );

        assertEquals(
                "El identificador de la orden no es válido.",
                excepcion.getMessage()
        );

        // Validar un identificador igual a cero.
        assertThrows(
                IllegalArgumentException.class,
                () -> pagoService.consultarPagoPorOrden(0L)
        );

        // Validar un identificador negativo.
        assertThrows(
                IllegalArgumentException.class,
                () -> pagoService.consultarPagoPorOrden(-1L)
        );

        // Ningún identificador inválido debe provocar
        // una consulta a la base de datos.
        verifyNoInteractions(pagoRepository);
    }

}

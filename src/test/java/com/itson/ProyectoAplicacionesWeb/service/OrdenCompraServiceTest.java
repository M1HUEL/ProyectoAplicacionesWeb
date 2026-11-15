
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.OrdenCompraResponseDTO;
import com.itson.ProyectoAplicacionesWeb.model.DetalleOrden;
import com.itson.ProyectoAplicacionesWeb.model.EstadoOrden;
import com.itson.ProyectoAplicacionesWeb.model.Formato;
import com.itson.ProyectoAplicacionesWeb.model.FormatoLibro;
import com.itson.ProyectoAplicacionesWeb.model.Libro;
import com.itson.ProyectoAplicacionesWeb.model.OrdenCompra;
import com.itson.ProyectoAplicacionesWeb.repository.OrdenCompraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Pruebas unitarias del servicio de órdenes de compra.
 */
class OrdenCompraServiceTest {

    @Mock
    private OrdenCompraRepository ordenCompraRepository;

    private OrdenCompraService ordenCompraService;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        ordenCompraService = new OrdenCompraService(ordenCompraRepository);
    }

    /**
     * Comprueba que el servicio devuelve la cantidad
     * de órdenes cuyo estado es PENDIENTE.
     *
     * Utiliza Mockito para simular el conteo
     * sin establecer una conexión con MySQL.
     */
    @Test
    void contarOrdenesPendientes_debeRetornarCantidadPendiente() {

        // Simula que existen tres órdenes pendientes.
        when(ordenCompraRepository.countByEstado(EstadoOrden.PENDIENTE))
                .thenReturn(3L);

        // Ejecuta el método que estamos probando.
        long resultado = ordenCompraService.contarOrdenesPendientes();

        // Comprueba que devuelve la cantidad esperada.
        assertEquals(3L, resultado);

        // Verifica que únicamente se consultaron las órdenes pendientes.
        verify(ordenCompraRepository)
                .countByEstado(EstadoOrden.PENDIENTE);

        // Comprueba que no se recuperó el listado completo.
        verify(ordenCompraRepository, never()).listarConUsuarios();
    }

    @Test
    void listarTodas_sinOrdenes_retornaListaVacia() {

        // Simula una base de datos sin órdenes registradas.
        when(ordenCompraRepository.listarConUsuarios()).thenReturn(List.of());

        // Ejecuta el método que estamos probando.
        List<OrdenCompra> resultado = ordenCompraService.listarTodas();

        // Comprueba que el resultado sea una lista vacía.
        assertTrue(resultado.isEmpty());
    }

    @Test
    void listarTodas_conOrdenes_retornaOrdenesRegistradas() {

        // Simula dos órdenes existentes.
        OrdenCompra orden1 = mock(OrdenCompra.class);
        OrdenCompra orden2 = mock(OrdenCompra.class);

        when(ordenCompraRepository.listarConUsuarios())
                .thenReturn(List.of(orden1, orden2));

        // Recupera las órdenes mediante el servicio.
        List<OrdenCompra> resultado = ordenCompraService.listarTodas();

        // Verifica que se recuperaron ambas órdenes.
        assertEquals(2, resultado.size());
    }

    /**
     * Comprueba que un cliente sin compras recibe
     * un historial vacío.
     */
    @Test
    void consultarHistorial_sinOrdenes_retornaListaVacia() {

        String correo = "cliente@raudskinna.com";

        when(ordenCompraRepository.listarPorCorreoUsuario(correo))
                .thenReturn(List.of());

        List<OrdenCompraResponseDTO> resultado =
                ordenCompraService.consultarHistorial(correo);

        assertTrue(resultado.isEmpty());

        verify(ordenCompraRepository).listarPorCorreoUsuario(correo);
    }

    /**
     * Comprueba que el historial incluye los datos de la orden
     * y de sus presentaciones compradas.
     */
    @Test
    void consultarHistorial_conOrdenes_retornaDetallesDeCompra() {

        String correo = "cliente@raudskinna.com";

        Libro libro = mock(Libro.class);
        when(libro.getTitulo()).thenReturn("Drácula");

        FormatoLibro formato = mock(FormatoLibro.class);
        when(formato.getId()).thenReturn(10L);
        when(formato.getLibro()).thenReturn(libro);
        when(formato.getFormato()).thenReturn(Formato.PASTA_DURA);

        DetalleOrden detalle = mock(DetalleOrden.class);
        when(detalle.getId()).thenReturn(100L);
        when(detalle.getFormatoLibro()).thenReturn(formato);
        when(detalle.getCantidad()).thenReturn(2);
        when(detalle.getPrecioUnitario())
                .thenReturn(new BigDecimal("899.00"));

        OrdenCompra orden = mock(OrdenCompra.class);
        when(orden.getId()).thenReturn(5L);
        when(orden.getFecha())
                .thenReturn(LocalDateTime.of(2026, 11, 13, 10, 0));
        when(orden.getEstado()).thenReturn(EstadoOrden.PENDIENTE);
        when(orden.getTotal()).thenReturn(new BigDecimal("1798.00"));
        when(orden.getDetalles()).thenReturn(List.of(detalle));

        when(ordenCompraRepository.listarPorCorreoUsuario(correo))
                .thenReturn(List.of(orden));

        List<OrdenCompraResponseDTO> resultado =
                ordenCompraService.consultarHistorial(correo);

        assertEquals(1, resultado.size());

        OrdenCompraResponseDTO respuesta = resultado.get(0);

        assertEquals(5L, respuesta.ordenId());
        assertEquals("PENDIENTE", respuesta.estado());
        assertEquals(new BigDecimal("1798.00"), respuesta.total());
        assertEquals(1, respuesta.detalles().size());

        var detalleRespuesta = respuesta.detalles().get(0);

        assertEquals(10L, detalleRespuesta.formatoLibroId());
        assertEquals("Drácula", detalleRespuesta.titulo());
        assertEquals("PASTA_DURA", detalleRespuesta.formato());
        assertEquals(2, detalleRespuesta.cantidad());
        assertEquals(new BigDecimal("899.00"), detalleRespuesta.precioUnitario());
        assertEquals(new BigDecimal("1798.00"), detalleRespuesta.subtotal());
    }

    /**
     * Comprueba que no se consulta el repositorio
     * cuando el correo del cliente no es válido.
     */
    @Test
    void consultarHistorial_correoVacio_lanzaExcepcion() {

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> ordenCompraService.consultarHistorial("  ")
        );

        assertEquals(
                "El correo del cliente autenticado no es válido.",
                excepcion.getMessage()
        );

        verifyNoInteractions(ordenCompraRepository);
    }

    /**
     * Comprueba que el servicio recupera una orden existente.
     */
    @Test
    void buscarPorId_ordenExistente_retornaOrden() {

        // Simula una orden registrada.
        Long ordenId = 1L;
        OrdenCompra orden = mock(OrdenCompra.class);

        when(ordenCompraRepository.findById(ordenId))
                .thenReturn(Optional.of(orden));

        // Ejecuta la búsqueda mediante el servicio.
        OrdenCompra resultado = ordenCompraService.buscarPorId(ordenId);

        // Comprueba que devuelve la misma orden.
        assertSame(orden, resultado);
    }

    /**
     * Comprueba que el servicio rechaza un ID nulo.
     */
    @Test
    void buscarPorId_idNulo_lanzaExcepcion() {

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> ordenCompraService.buscarPorId(null)
        );

        assertEquals(
                "El identificador de la orden no es válido.",
                excepcion.getMessage()
        );

        // Un ID inválido debe rechazarse antes de consultar MySQL.
        verifyNoInteractions(ordenCompraRepository);
    }

    /**
     * Comprueba que el servicio rechaza una orden inexistente.
     */
    @Test
    void buscarPorId_ordenInexistente_lanzaExcepcion() {

        Long ordenId = 999L;

        // Simula que el repositorio no encuentra la orden.
        when(ordenCompraRepository.findById(ordenId))
                .thenReturn(Optional.empty());

        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> ordenCompraService.buscarPorId(ordenId)
        );

        assertEquals(
                "No existe una orden con el identificador indicado.",
                excepcion.getMessage()
        );

        // Comprueba que se intentó consultar el identificador.
        verify(ordenCompraRepository).findById(ordenId);
    }

    /**
     * Comprueba que una orden pendiente puede pasar
     * al estado PROCESANDO.
     */
    @Test
    void actualizarEstado_dePendienteAProcesando_actualizaEstado() {

        Long ordenId = 1L;

        // Simula una orden que actualmente está pendiente.
        OrdenCompra orden = mock(OrdenCompra.class);

        when(ordenCompraRepository.findById(ordenId))
                .thenReturn(Optional.of(orden));

        when(orden.getEstado())
                .thenReturn(EstadoOrden.PENDIENTE);

        // Ejecuta la transición.
        OrdenCompra resultado = ordenCompraService.actualizarEstado(
                ordenId,
                EstadoOrden.PROCESANDO
        );

        // Verifica que el servicio modificó el estado.
        assertSame(orden, resultado);

        verify(orden).setEstado(EstadoOrden.PROCESANDO);
    }

    /**
     * Comprueba que una orden enviada no puede cancelarse.
     */
    @Test
    void actualizarEstado_deEnviadoACancelado_lanzaExcepcion() {

        Long ordenId = 1L;

        // Simula una orden que ya fue enviada.
        OrdenCompra orden = mock(OrdenCompra.class);

        when(ordenCompraRepository.findById(ordenId))
                .thenReturn(Optional.of(orden));

        when(orden.getEstado())
                .thenReturn(EstadoOrden.ENVIADO);

        // Intenta realizar una transición prohibida.
        IllegalArgumentException excepcion = assertThrows(
                IllegalArgumentException.class,
                () -> ordenCompraService.actualizarEstado(
                        ordenId,
                        EstadoOrden.CANCELADO
                )
        );

        // Verifica el mensaje de la regla de negocio.
        assertEquals(
                "No se permite cambiar la orden de ENVIADO a CANCELADO.",
                excepcion.getMessage()
        );

        // Comprueba que el estado original no fue modificado.
        verify(orden, never()).setEstado(any(EstadoOrden.class));
    }

    /**
     * Comprueba que una orden en procesamiento
     * puede cancelarse antes de ser enviada.
     */
    @Test
    void actualizarEstado_deProcesandoACancelado_actualizaEstado() {

        Long ordenId = 1L;

        // Simula una orden en procesamiento.
        OrdenCompra orden = mock(OrdenCompra.class);

        when(ordenCompraRepository.findById(ordenId))
                .thenReturn(Optional.of(orden));

        when(orden.getEstado())
                .thenReturn(EstadoOrden.PROCESANDO);

        // Solicita la cancelación.
        OrdenCompra resultado = ordenCompraService.actualizarEstado(
                ordenId,
                EstadoOrden.CANCELADO
        );

        // Comprueba que se aceptó la transición.
        assertSame(orden, resultado);
        verify(orden).setEstado(EstadoOrden.CANCELADO);
    }

}

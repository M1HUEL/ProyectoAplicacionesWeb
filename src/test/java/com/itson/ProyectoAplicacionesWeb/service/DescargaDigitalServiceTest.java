
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.DetalleOrdenResponseDTO;
import com.itson.ProyectoAplicacionesWeb.dto.OrdenCompraResponseDTO;
import com.itson.ProyectoAplicacionesWeb.storage.AlmacenamientoArchivos;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de la descarga digital simulada.
 *
 * Verifica la autorización antes de entregar
 * el archivo PDF de demostración.
 */
@ExtendWith(MockitoExtension.class)
class DescargaDigitalServiceTest {

    private static final String CORREO = "cliente@ejemplo.com";
    private static final Long ORDEN_ID = 1L;
    private static final Long FORMATO_ID = 23L;

    @Mock
    private OrdenCompraService ordenCompraService;

    @Mock
    private AlmacenamientoArchivos almacenamiento;

    @InjectMocks
    private DescargaDigitalService descargaDigitalService;

    /**
     * Construye una orden simulada que contiene
     * una presentación digital previamente adquirida.
     */
    private OrdenCompraResponseDTO crearOrden(String estado) {

        DetalleOrdenResponseDTO detalle =
                new DetalleOrdenResponseDTO(
                        FORMATO_ID,
                        "Clean Code",
                        "DIGITAL",
                        1,
                        new BigDecimal("599.00"),
                        new BigDecimal("599.00")
                );

        return new OrdenCompraResponseDTO(
                ORDEN_ID,
                LocalDateTime.now(),
                estado,
                new BigDecimal("599.00"),
                List.of(detalle)
        );
    }

    /**
     * Una orden entregada permite recuperar
     * el PDF de demostración.
     */
    @Test
    void descargarLibroDigitalEntregado() {

        when(ordenCompraService.consultarOrdenPropia(
                CORREO, ORDEN_ID
        )).thenReturn(crearOrden("ENTREGADO"));

        when(almacenamiento.leer(
                DescargaDigitalService.CLAVE_PDF_DEMO
        )).thenReturn(Optional.of(
                "%PDF-1.4 contenido de prueba".getBytes(
                        java.nio.charset.StandardCharsets.US_ASCII
                )
        ));

        byte[] archivo = descargaDigitalService.descargar(
                CORREO, ORDEN_ID, FORMATO_ID
        );

        assertNotNull(archivo);
        assertTrue(archivo.length > 0);

        assertEquals(
                "%PDF",
                new String(
                        archivo,
                        0,
                        4,
                        java.nio.charset.StandardCharsets.US_ASCII
                )
        );
    }

    /**
     * Si el proveedor no encuentra el archivo, se informa un error
     * del servidor sin exponer detalles del almacenamiento.
     */
    @Test
    void rechazarDescargaSiElArchivoNoExiste() {

        when(ordenCompraService.consultarOrdenPropia(
                CORREO, ORDEN_ID
        )).thenReturn(crearOrden("ENTREGADO"));

        when(almacenamiento.leer(
                DescargaDigitalService.CLAVE_PDF_DEMO
        )).thenReturn(Optional.empty());

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> descargaDigitalService.descargar(
                        CORREO, ORDEN_ID, FORMATO_ID
                )
        );

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                excepcion.getStatusCode());
    }

    /**
     * Una orden pendiente no permite descargar.
     */
    @Test
    void rechazarDescargaDeOrdenPendiente() {

        when(ordenCompraService.consultarOrdenPropia(
                CORREO, ORDEN_ID
        )).thenReturn(crearOrden("PENDIENTE"));

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> descargaDigitalService.descargar(
                        CORREO, ORDEN_ID, FORMATO_ID
                )
        );

        assertEquals(HttpStatus.FORBIDDEN,
                excepcion.getStatusCode());
    }

    /**
     * Las órdenes canceladas tampoco permiten descargar.
     */
    @Test
    void rechazarDescargaDeOrdenCancelada() {

        when(ordenCompraService.consultarOrdenPropia(
                CORREO, ORDEN_ID
        )).thenReturn(crearOrden("CANCELADO"));

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> descargaDigitalService.descargar(
                        CORREO, ORDEN_ID, FORMATO_ID
                )
        );

        assertEquals(HttpStatus.FORBIDDEN,
                excepcion.getStatusCode());
    }

    /**
     * No se puede descargar una presentación
     * que no forma parte de la orden.
     */
    @Test
    void rechazarFormatoNoAdquirido() {

        when(ordenCompraService.consultarOrdenPropia(
                CORREO, ORDEN_ID
        )).thenReturn(crearOrden("ENTREGADO"));

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> descargaDigitalService.descargar(
                        CORREO, ORDEN_ID, 999L
                )
        );

        assertEquals(HttpStatus.NOT_FOUND,
                excepcion.getStatusCode());
    }

    /**
     * No permite descargar presentaciones físicas.
     */
    @Test
    void rechazarFormatoFisico() {

        DetalleOrdenResponseDTO detalleFisico =
                new DetalleOrdenResponseDTO(
                        FORMATO_ID,
                        "Rauðskinna",
                        "PASTA_DURA",
                        1,
                        new BigDecimal("666.00"),
                        new BigDecimal("666.00")
                );

        OrdenCompraResponseDTO orden =
                new OrdenCompraResponseDTO(
                        ORDEN_ID,
                        LocalDateTime.now(),
                        "ENTREGADO",
                        new BigDecimal("666.00"),
                        List.of(detalleFisico)
                );

        when(ordenCompraService.consultarOrdenPropia(
                CORREO, ORDEN_ID
        )).thenReturn(orden);

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> descargaDigitalService.descargar(
                        CORREO, ORDEN_ID, FORMATO_ID
                )
        );

        assertEquals(HttpStatus.NOT_FOUND,
                excepcion.getStatusCode());
    }

    /**
     * El servicio de órdenes debe rechazar
     * compras pertenecientes a otros clientes.
     */
    @Test
    void rechazarOrdenDeOtroCliente() {

        when(ordenCompraService.consultarOrdenPropia(
                CORREO, ORDEN_ID
        )).thenThrow(new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "No se encontró la orden solicitada."
        ));

        ResponseStatusException excepcion = assertThrows(
                ResponseStatusException.class,
                () -> descargaDigitalService.descargar(
                        CORREO, ORDEN_ID, FORMATO_ID
                )
        );

        assertEquals(HttpStatus.NOT_FOUND,
                excepcion.getStatusCode());
    }
}

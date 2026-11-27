
package com.itson.ProyectoAplicacionesWeb.controller;

import com.itson.ProyectoAplicacionesWeb.dto.CatalogoPublicoDTO;
import com.itson.ProyectoAplicacionesWeb.dto.FormatoPublicoDTO;
import com.itson.ProyectoAplicacionesWeb.dto.LibroCatalogoDTO;
import com.itson.ProyectoAplicacionesWeb.dto.LibroPublicoDTO;
import com.itson.ProyectoAplicacionesWeb.model.Formato;
import com.itson.ProyectoAplicacionesWeb.service.LibroService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración MVC del detalle público
 * de libros de Librería Raudskinna.
 *
 * Verifican el acceso anónimo mediante Spring Security
 * y el renderizado de la plantilla Thymeleaf.
 *
 * Utilizan el perfil integrationtest con H2 y simulan
 * LibroService para controlar los datos comerciales.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integrationtest")
class CatalogoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LibroService libroService;

    /**
     * Comprueba que un visitante sin sesión pueda
     * consultar la información bibliográfica y comercial
     * de un libro activo.
     *
     * La vista debe mostrar los formatos, sus precios
     * y la disponibilidad correspondiente.
     */
    @Test
    void mostrarDetalleLibro_sinSesion_renderizaInformacionComercial()
            throws Exception {

        // ARRANGE: preparar la información bibliográfica.
        LibroPublicoDTO libro = new LibroPublicoDTO(
                1L,
                "La llamada de Cthulhu",
                "Relato de terror cósmico.",
                null,
                "Terror",
                "Editorial de prueba",
                List.of("H. P. Lovecraft")
        );

        // Preparar tres presentaciones comerciales.
        List<FormatoPublicoDTO> formatos = List.of(
                new FormatoPublicoDTO(
                        10L,
                        Formato.PASTA_DURA,
                        new BigDecimal("350.00"),
                        true
                ),
                new FormatoPublicoDTO(
                        20L,
                        Formato.PASTA_BLANDA,
                        new BigDecimal("250.00"),
                        false
                ),
                new FormatoPublicoDTO(
                        30L,
                        Formato.DIGITAL,
                        new BigDecimal("149.00"),
                        true
                )
        );

        // Simular las respuestas de LibroService.
        when(libroService.consultarLibroPublico(1L))
                .thenReturn(libro);

        when(libroService.listarFormatosPublicos(1L))
                .thenReturn(formatos);

        // ACT y ASSERT: solicitar el detalle sin autenticación.
        mockMvc.perform(get("/detalle-libro/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("detalle-libro"))

                // Información bibliográfica.
                .andExpect(content().string(
                        containsString("La llamada de Cthulhu")
                ))
                .andExpect(content().string(
                        containsString("H. P. Lovecraft")
                ))
                .andExpect(content().string(
                        containsString("Relato de terror cósmico.")
                ))
                .andExpect(content().string(
                        containsString("Editorial de prueba")
                ))

                // Presentaciones comerciales.
                .andExpect(content().string(
                        containsString("Pasta dura")
                ))
                .andExpect(content().string(
                        containsString("Pasta blanda")
                ))
                .andExpect(content().string(
                        containsString("Digital")
                ))

                // Precios con formato monetario mexicano.
                .andExpect(content().string(
                        containsString("$350.00 MXN")
                ))
                .andExpect(content().string(
                        containsString("$250.00 MXN")
                ))
                .andExpect(content().string(
                        containsString("$149.00 MXN")
                ))

                // Estados de disponibilidad.
                .andExpect(content().string(
                        containsString("Disponible")
                ))
                .andExpect(content().string(
                        containsString("Agotado")
                ));

        // Confirmar que la solicitud llegó al servicio.
        verify(libroService).consultarLibroPublico(1L);
        verify(libroService).listarFormatosPublicos(1L);
    }

    /**
     * Comprueba que un visitante no pueda consultar
     * el detalle de un libro inactivo o inexistente.
     *
     * La aplicación debe responder con HTTP 404
     * sin recuperar sus presentaciones comerciales.
     */
    @Test
    void mostrarDetalleLibro_libroInactivo_retorna404()
            throws Exception {

        // ARRANGE: simular un libro no disponible públicamente.
        when(libroService.consultarLibroPublico(99L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El libro solicitado no está disponible."
                ));

        // ACT y ASSERT: solicitar el detalle sin autenticación.
        mockMvc.perform(get("/detalle-libro/99"))
                .andExpect(status().isNotFound());

        // Confirmar que se realizó la consulta bibliográfica.
        verify(libroService).consultarLibroPublico(99L);

        // No deben consultarse las presentaciones comerciales.
        verify(libroService, never())
                .listarFormatosPublicos(anyLong());

        verifyNoMoreInteractions(libroService);
    }

    /**
     * Comprueba que un libro activo sin presentaciones
     * comerciales pueda consultarse públicamente.
     *
     * La vista debe mostrar su información bibliográfica
     * e informar que todavía no existen formatos registrados,
     * sin presentar precios ni disponibilidad ficticios.
     */
    @Test
    void mostrarDetalleLibro_sinFormatos_muestraMensajeInformativo()
            throws Exception {

        // ARRANGE: preparar un libro activo.
        LibroPublicoDTO libro = new LibroPublicoDTO(
                2L,
                "En las montañas de la locura",
                "Relato de terror cósmico.",
                null,
                "Terror",
                "Editorial de prueba",
                List.of("H. P. Lovecraft")
        );

        when(libroService.consultarLibroPublico(2L))
                .thenReturn(libro);

        // El libro todavía no tiene presentaciones comerciales.
        when(libroService.listarFormatosPublicos(2L))
                .thenReturn(List.of());

        // ACT y ASSERT: consultar el detalle sin autenticación.
        mockMvc.perform(get("/detalle-libro/2"))
                .andExpect(status().isOk())
                .andExpect(view().name("detalle-libro"))

                // La información bibliográfica debe permanecer visible.
                .andExpect(content().string(
                        containsString("En las montañas de la locura")
                ))
                .andExpect(content().string(
                        containsString("H. P. Lovecraft")
                ))

                // Informar la ausencia de presentaciones comerciales.
                .andExpect(content().string(
                        containsString(
                                "Este libro todavía no tiene "
                                        + "presentaciones comerciales registradas."
                        )
                ))

                // No deben mostrarse formatos ni precios ficticios.
                .andExpect(content().string(
                        not(containsString("class=\"formato-libro\""))
                ))
                .andExpect(content().string(
                        not(containsString("class=\"formato-disponible\""))
                ));

        // Confirmar las consultas realizadas al servicio.
        verify(libroService).consultarLibroPublico(2L);
        verify(libroService).listarFormatosPublicos(2L);

        verifyNoMoreInteractions(libroService);
    }

    /**
     * Comprueba que un visitante sin autenticación pueda
     * consultar el catálogo público de Librería Raudskinna.
     *
     * Verifica el renderizado de libros disponibles y agotados,
     * sus datos bibliográficos, precios y enlaces hacia
     * las correspondientes páginas de detalle.
     */
    @Test
    void mostrarCatalogo_sinSesion_renderizaLibros()
            throws Exception {

        // ARRANGE: preparar un libro disponible.
        LibroPublicoDTO libroDisponible = new LibroPublicoDTO(
                1L,
                "La llamada de Cthulhu",
                "Relato de terror cósmico.",
                null,
                "Terror",
                "Editorial de prueba",
                List.of("H. P. Lovecraft")
        );

        // Preparar un libro agotado.
        LibroPublicoDTO libroAgotado = new LibroPublicoDTO(
                2L,
                "Drácula",
                "Novela de terror.",
                null,
                "Terror",
                "Editorial de prueba",
                List.of("Bram Stoker")
        );

        // Preparar las tarjetas con información comercial.
        List<LibroCatalogoDTO> libros = List.of(
                new LibroCatalogoDTO(
                        libroDisponible,
                        new BigDecimal("350.00"),
                        true
                ),
                new LibroCatalogoDTO(
                        libroAgotado,
                        null,
                        false
                )
        );

        // Simular la respuesta del servicio.
        // Preparar los resultados y las opciones dinámicas.
        CatalogoPublicoDTO catalogo = new CatalogoPublicoDTO(
                libros,
                List.of("Terror"),
                List.of("Bram Stoker", "H. P. Lovecraft")
        );

        // Simular la consulta del catálogo público.
        when(libroService.consultarCatalogoPublico(
                null, null, null, "relevancia"
        )).thenReturn(catalogo);

        // ACT y ASSERT: consultar el catálogo sin autenticación.
        mockMvc.perform(get("/catalogo"))
                .andExpect(status().isOk())
                .andExpect(view().name("catalogo"))

                // Verificar el conteo dinámico.
                .andExpect(content().string(
                        containsString("2 libros")
                ))

                // Verificar la información del libro disponible.
                .andExpect(content().string(
                        containsString("La llamada de Cthulhu")
                ))
                .andExpect(content().string(
                        containsString("H. P. Lovecraft")
                ))
                .andExpect(content().string(
                        containsString("$350.00 MXN")
                ))

                // Verificar la información del libro agotado.
                .andExpect(content().string(
                        containsString("Drácula")
                ))
                .andExpect(content().string(
                        containsString("Bram Stoker")
                ))
                .andExpect(content().string(
                        containsString("Agotado")
                ))

                // Ambos libros deben permitir consultar su detalle.
                .andExpect(content().string(
                        containsString("href=\"/detalle-libro/1\"")
                ))
                .andExpect(content().string(
                        containsString("href=\"/detalle-libro/2\"")
                ));

        // Confirmar la consulta realizada al servicio.
        verify(libroService).consultarCatalogoPublico(
                null, null, null, "relevancia"
        );

        verifyNoMoreInteractions(libroService);
    }

    /**
     * Comprueba que un visitante pueda consultar el catálogo
     * público cuando todavía no existen libros activos.
     *
     * La aplicación debe presentar un conteo de cero libros
     * y un mensaje informativo, sin generar tarjetas ficticias
     * ni errores durante el renderizado de Thymeleaf.
     */
    @Test
    void mostrarCatalogo_sinLibros_muestraMensajeInformativo()
            throws Exception {

        // Preparar un catálogo sin libros ni opciones de filtrado.
        CatalogoPublicoDTO catalogo = new CatalogoPublicoDTO(
                List.of(),
                List.of(),
                List.of()
        );

        // Simular la consulta del catálogo vacío.
        when(libroService.consultarCatalogoPublico(
                null, null, null, "relevancia"
        )).thenReturn(catalogo);

        // ACT y ASSERT: consultar el catálogo sin autenticación.
        mockMvc.perform(get("/catalogo"))
                .andExpect(status().isOk())
                .andExpect(view().name("catalogo"))

                // Verificar el conteo dinámico.
                .andExpect(content().string(
                        containsString("0 libros")
                ))

                // Verificar el mensaje informativo.
                .andExpect(content().string(
                        containsString(
                                "Todavía no hay libros disponibles en el catálogo."
                        )
                ))

                // No deben generarse tarjetas de libros ficticios.
                .andExpect(content().string(
                        not(containsString("class=\"libros\""))
                ))

                // No deben generarse enlaces hacia libros inexistentes.
                .andExpect(content().string(
                        not(containsString("href=\"/detalle-libro/"))
                ));

        // Confirmar la consulta realizada al servicio.
        verify(libroService).consultarCatalogoPublico(
                null, null, null, "relevancia"
        );

        verifyNoMoreInteractions(libroService);
    }

    /**
     * Comprueba que un visitante pueda realizar búsquedas
     * dentro del catálogo público sin iniciar sesión.
     *
     * Verifica que Spring MVC reciba el parámetro de búsqueda,
     * lo transmita a LibroService y conserve el término
     * introducido por el visitante en el modelo.
     */
    @Test
    void mostrarCatalogo_conBusqueda_renderizaCoincidencias()
            throws Exception {

        // ARRANGE: preparar un libro coincidente.
        LibroPublicoDTO libro = new LibroPublicoDTO(
                1L,
                "La llamada de Cthulhu",
                "Relato de terror cósmico.",
                null,
                "Terror",
                "Editorial de prueba",
                List.of("H. P. Lovecraft")
        );

        // Preparar su correspondiente información comercial.
        LibroCatalogoDTO tarjeta = new LibroCatalogoDTO(
                libro,
                new BigDecimal("350.00"),
                true
        );

        List<LibroCatalogoDTO> libros = List.of(tarjeta);

        // Preparar los resultados y las opciones de filtrado.
        CatalogoPublicoDTO catalogo = new CatalogoPublicoDTO(
                libros,
                List.of("Terror"),
                List.of("H. P. Lovecraft")
        );

        // Simular una búsqueda bibliográfica.
        when(libroService.consultarCatalogoPublico(
                "Lovecraft", null, null, "relevancia"
        )).thenReturn(catalogo);

        // ACT y ASSERT: realizar una búsqueda sin autenticación.
        mockMvc.perform(
                        get("/catalogo")
                                .param("busqueda", "Lovecraft")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("catalogo"))

                // Verificar los atributos entregados a Thymeleaf.
                .andExpect(model().attribute("busqueda", "Lovecraft"))
                .andExpect(model().attribute("libros", libros))

                // Verificar el conteo de resultados.
                .andExpect(content().string(
                        containsString("1 libro")
                ))

                // Verificar la información bibliográfica.
                .andExpect(content().string(
                        containsString("La llamada de Cthulhu")
                ))
                .andExpect(content().string(
                        containsString("H. P. Lovecraft")
                ))

                // Verificar la información comercial.
                .andExpect(content().string(
                        containsString("$350.00 MXN")
                ))

                // Conservar la navegación hacia el detalle.
                .andExpect(content().string(
                        containsString("href=\"/detalle-libro/1\"")
                ))
                // Verificar que exista el campo de búsqueda.
                .andExpect(content().string(
                        containsString("name=\"busqueda\"")
                ))

                // Verificar que se conserve el término introducido.
                .andExpect(content().string(
                        containsString("value=\"Lovecraft\"")
                ));

        // Confirmar que el término llegó correctamente al servicio.
        verify(libroService).consultarCatalogoPublico(
                "Lovecraft", null, null, "relevancia"
        );

        verifyNoMoreInteractions(libroService);
    }

    /**
     * Comprueba que una búsqueda sin coincidencias
     * presente un mensaje informativo al visitante.
     *
     * La ausencia de resultados no debe confundirse
     * con un catálogo completamente vacío ni generar
     * errores durante el renderizado de Thymeleaf.
     */
    @Test
    void mostrarCatalogo_busquedaSinCoincidencias_muestraMensaje()
            throws Exception {

        // Preparar una búsqueda sin resultados, conservando
// las opciones disponibles en el catálogo completo.
        CatalogoPublicoDTO catalogo = new CatalogoPublicoDTO(
                List.of(),
                List.of("Terror"),
                List.of("H. P. Lovecraft")
        );

        // Simular una búsqueda sin coincidencias.
        when(libroService.consultarCatalogoPublico(
                "Stephen King", null, null, "relevancia"
        )).thenReturn(catalogo);

        // ACT y ASSERT: realizar la búsqueda sin autenticación.
        mockMvc.perform(
                        get("/catalogo")
                                .param("busqueda", "Stephen King")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("catalogo"))

                // Conservar el término introducido.
                .andExpect(model().attribute(
                        "busqueda", "Stephen King"
                ))

                .andExpect(content().string(
                        containsString("value=\"Stephen King\"")
                ))

                // Verificar el conteo de resultados.
                .andExpect(content().string(
                        containsString("0 libros")
                ))

                // Mostrar el mensaje correspondiente a la búsqueda.
                .andExpect(content().string(
                        containsString(
                                "No se encontraron libros que "
                                        + "coincidan con tu búsqueda."
                        )
                ))

                // No confundir la búsqueda con un catálogo vacío.
                .andExpect(content().string(
                        not(containsString(
                                "Todavía no hay libros disponibles "
                                        + "en el catálogo."
                        ))
                ))

                // No deben generarse tarjetas ficticias.
                .andExpect(content().string(
                        not(containsString("class=\"libros\""))
                ));

        // Confirmar que el término llegó correctamente al servicio.
        verify(libroService).consultarCatalogoPublico(
                "Stephen King", null, null, "relevancia"
        );

        verifyNoMoreInteractions(libroService);
    }

    /**
     * Comprueba que un visitante pueda combinar búsquedas,
     * filtros bibliográficos y criterios de ordenamiento
     * desde el catálogo público sin iniciar sesión.
     *
     * Verifica la recepción de los parámetros HTTP,
     * su delegación a LibroService y la conservación
     * de las opciones seleccionadas en Thymeleaf.
     */
    @Test
    void mostrarCatalogo_conFiltrosYOrden_renderizaResultados()
            throws Exception {

        // ARRANGE: preparar un libro coincidente.
        LibroPublicoDTO libro = new LibroPublicoDTO(
                1L,
                "La llamada de Cthulhu",
                "Relato de terror cósmico.",
                null,
                "Terror",
                "Editorial de prueba",
                List.of("H. P. Lovecraft")
        );

        // Preparar su información comercial.
        LibroCatalogoDTO tarjeta = new LibroCatalogoDTO(
                libro,
                new BigDecimal("350.00"),
                true
        );

        List<LibroCatalogoDTO> libros = List.of(tarjeta);

        // Preparar las opciones del catálogo completo.
        List<String> categorias =
                List.of("Fantasía", "Terror");

        List<String> autores =
                List.of(
                        "Bram Stoker",
                        "H. P. Lovecraft",
                        "J. R. R. Tolkien"
                );

        // Agrupar los resultados y opciones de filtrado.
        CatalogoPublicoDTO catalogo = new CatalogoPublicoDTO(
                libros,
                categorias,
                autores
        );

        // Simular la respuesta proporcionada por LibroService.
        when(libroService.consultarCatalogoPublico(
                "Cthulhu",
                "Terror",
                "H. P. Lovecraft",
                "precio-asc"
        )).thenReturn(catalogo);

        // ACT y ASSERT: realizar una consulta combinada.
        mockMvc.perform(
                        get("/catalogo")
                                .param("busqueda", "Cthulhu")
                                .param("categoria", "Terror")
                                .param("autor", "H. P. Lovecraft")
                                .param("orden", "precio-asc")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("catalogo"))

                // Verificar los siete atributos del modelo.
                .andExpect(model().attribute("libros", libros))
                .andExpect(model().attribute("categorias", categorias))
                .andExpect(model().attribute("autores", autores))
                .andExpect(model().attribute("busqueda", "Cthulhu"))
                .andExpect(model().attribute("categoria", "Terror"))
                .andExpect(model().attribute("autor", "H. P. Lovecraft"))
                .andExpect(model().attribute("orden", "precio-asc"))

                // Verificar los controles del formulario.
                .andExpect(content().string(
                        containsString("name=\"busqueda\"")
                ))
                .andExpect(content().string(
                        containsString("name=\"categoria\"")
                ))
                .andExpect(content().string(
                        containsString("name=\"autor\"")
                ))
                .andExpect(content().string(
                        containsString("name=\"orden\"")
                ))
                // Verificar que se conserve el término de búsqueda.
                .andExpect(content().string(
                        containsString("value=\"Cthulhu\"")
                ))
                // Verificar la categoría seleccionada,
                // admitiendo espacios y saltos de línea entre atributos.
                .andExpect(content().string(
                        matchesPattern(
                                "(?s).*<option\\s+value=\"Terror\""
                                        + "\\s+selected=\"selected\"\\s*>.*"
                        )
                ))
                // Verificar el autor seleccionado.
                .andExpect(content().string(
                        matchesPattern(
                                "(?s).*<option\\s+value=\"H\\. P\\. Lovecraft\""
                                        + "\\s+selected=\"selected\"\\s*>.*"
                        )
                ))
                // Verificar el criterio de ordenamiento seleccionado.
                .andExpect(content().string(
                        matchesPattern(
                                "(?s).*<option\\s+value=\"precio-asc\""
                                        + "\\s+selected=\"selected\"\\s*>.*"
                        )
                ))
                // Las demás opciones deben permanecer disponibles.
                .andExpect(content().string(
                        containsString("value=\"Fantasía\"")
                ))
                .andExpect(content().string(
                        containsString("value=\"Bram Stoker\"")
                ))
                .andExpect(content().string(
                        containsString("value=\"J. R. R. Tolkien\"")
                ))
                // Verificar el resultado bibliográfico y comercial.
                .andExpect(content().string(
                        containsString("1 libro")
                ))
                .andExpect(content().string(
                        containsString("La llamada de Cthulhu")
                ))
                .andExpect(content().string(
                        containsString("$350.00 MXN")
                ))
                .andExpect(content().string(
                        containsString("href=\"/detalle-libro/1\"")
                ));

        // Confirmar la delegación de todos los criterios.
        verify(libroService).consultarCatalogoPublico(
                "Cthulhu",
                "Terror",
                "H. P. Lovecraft",
                "precio-asc"
        );

        verifyNoMoreInteractions(libroService);
    }

    /**
     * Comprueba que una combinación incompatible de
     * filtros presente un mensaje informativo al visitante.
     *
     * La ausencia de coincidencias no debe confundirse
     * con un catálogo completamente vacío.
     *
     * Las opciones dinámicas deben permanecer disponibles
     * y conservar los criterios seleccionados.
     */
    @Test
    void mostrarCatalogo_filtrosSinCoincidencias_muestraMensaje()
            throws Exception {

        // ARRANGE: preparar las opciones del catálogo completo.
        List<String> categorias =
                List.of("Fantasía", "Terror");

        List<String> autores =
                List.of(
                        "Bram Stoker",
                        "H. P. Lovecraft",
                        "J. R. R. Tolkien"
                );

        // Preparar un resultado sin libros coincidentes.
        CatalogoPublicoDTO catalogo = new CatalogoPublicoDTO(
                List.of(),
                categorias,
                autores
        );

        // Simular una combinación incompatible de filtros.
        when(libroService.consultarCatalogoPublico(
                null,
                "Fantasía",
                "H. P. Lovecraft",
                "relevancia"
        )).thenReturn(catalogo);

        // ACT y ASSERT: realizar una consulta sin autenticación.
        mockMvc.perform(
                        get("/catalogo")
                                .param("categoria", "Fantasía")
                                .param("autor", "H. P. Lovecraft")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("catalogo"))

                // Verificar los resultados y opciones disponibles.
                .andExpect(model().attribute("libros", List.of()))
                .andExpect(model().attribute("categorias", categorias))
                .andExpect(model().attribute("autores", autores))

                // Verificar los criterios conservados.
                .andExpect(model().attribute("categoria", "Fantasía"))
                .andExpect(model().attribute("autor", "H. P. Lovecraft"))
                .andExpect(model().attribute("orden", "relevancia"))

                // Verificar el conteo de resultados.
                .andExpect(content().string(
                        containsString("0 libros")
                ))

                // Mostrar el mensaje correspondiente al filtrado.
                .andExpect(content().string(
                        containsString(
                                "No se encontraron libros que "
                                        + "coincidan con tu búsqueda."
                        )
                ))

                // No confundir el resultado con un catálogo vacío.
                .andExpect(content().string(
                        not(containsString(
                                "Todavía no hay libros disponibles "
                                        + "en el catálogo."
                        ))
                ))

                // Verificar la categoría seleccionada.
                .andExpect(content().string(
                        matchesPattern(
                                "(?s).*<option\\s+value=\"Fantasía\""
                                        + "\\s+selected=\"selected\"\\s*>.*"
                        )
                ))

                // Verificar el autor seleccionado.
                .andExpect(content().string(
                        matchesPattern(
                                "(?s).*<option\\s+value=\"H\\. P\\. Lovecraft\""
                                        + "\\s+selected=\"selected\"\\s*>.*"
                        )
                ))

                // Las demás opciones deben permanecer disponibles.
                .andExpect(content().string(
                        containsString("value=\"Terror\"")
                ))
                .andExpect(content().string(
                        containsString("value=\"J. R. R. Tolkien\"")
                ))

                // No deben generarse tarjetas ficticias.
                .andExpect(content().string(
                        not(containsString("class=\"libros\""))
                ));

        // Confirmar la delegación de los criterios al servicio.
        verify(libroService).consultarCatalogoPublico(
                null,
                "Fantasía",
                "H. P. Lovecraft",
                "relevancia"
        );

        verifyNoMoreInteractions(libroService);
    }

}

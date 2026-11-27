package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias del catálogo de demostración.
 *
 * Los repositorios Mockito simulan una base persistente en memoria:
 * búsquedas repetidas recuperan las mismas entidades y sus IDs.
 * No se utiliza MySQL.
 */
@ExtendWith(MockitoExtension.class)
class CatalogoDemoServiceTest {

    private static final String ISBN_RAUDSKINNA = "DEMO-RAUD-001";

    @Mock private CategoriaRepository categoriaRepository;
    @Mock private AutorRepository autorRepository;
    @Mock private EditorialRepository editorialRepository;
    @Mock private LibroRepository libroRepository;
    @Mock private FormatoLibroRepository formatoLibroRepository;
    @Mock private ItemStockRepository itemStockRepository;

    @InjectMocks private CatalogoDemoService catalogoDemoService;

    private final Map<String, Categoria> categorias = new HashMap<>();
    private final Map<String, Autor> autores = new HashMap<>();
    private final Map<String, Editorial> editoriales = new HashMap<>();
    private final Map<String, Libro> libros = new HashMap<>();
    private final Map<String, FormatoLibro> formatos = new HashMap<>();
    private final Map<Long, ItemStock> inventario = new HashMap<>();

    private final AtomicLong siguienteCategoriaId = new AtomicLong(1);
    private final AtomicLong siguienteAutorId = new AtomicLong(1);
    private final AtomicLong siguienteEditorialId = new AtomicLong(1);
    private final AtomicLong siguienteLibroId = new AtomicLong(1);
    private final AtomicLong siguienteFormatoId = new AtomicLong(1);

    private String clave(String texto) {
        return texto.toLowerCase(Locale.ROOT);
    }

    private String claveFormato(Long libroId, Formato formato) {
        return libroId + ":" + formato.name();
    }

    /**
     * Simula los findBy y save de los seis repositorios. Cada save
     * almacena el mismo objeto que luego encontrarán los findBy.
     */
    private void configurarRepositoriosEnMemoria() {

        when(categoriaRepository.findByNombreIgnoreCase(any(String.class)))
                .thenAnswer(inv -> Optional.ofNullable(
                        categorias.get(clave(inv.getArgument(0)))));
        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(inv -> {
                    Categoria entidad = inv.getArgument(0);
                    ReflectionTestUtils.setField(entidad, "id", siguienteCategoriaId.getAndIncrement());
                    categorias.put(clave(entidad.getNombre()), entidad);
                    return entidad;
                });

        when(autorRepository.findByNombreIgnoreCase(any(String.class)))
                .thenAnswer(inv -> Optional.ofNullable(
                        autores.get(clave(inv.getArgument(0)))));
        when(autorRepository.save(any(Autor.class)))
                .thenAnswer(inv -> {
                    Autor entidad = inv.getArgument(0);
                    ReflectionTestUtils.setField(entidad, "id", siguienteAutorId.getAndIncrement());
                    autores.put(clave(entidad.getNombre()), entidad);
                    return entidad;
                });

        when(editorialRepository.findByNombreIgnoreCase(any(String.class)))
                .thenAnswer(inv -> Optional.ofNullable(
                        editoriales.get(clave(inv.getArgument(0)))));
        when(editorialRepository.save(any(Editorial.class)))
                .thenAnswer(inv -> {
                    Editorial entidad = inv.getArgument(0);
                    ReflectionTestUtils.setField(entidad, "id", siguienteEditorialId.getAndIncrement());
                    editoriales.put(clave(entidad.getNombre()), entidad);
                    return entidad;
                });

        when(libroRepository.findByIsbn(any(String.class)))
                .thenAnswer(inv -> Optional.ofNullable(libros.get(inv.getArgument(0))));
        when(libroRepository.save(any(Libro.class)))
                .thenAnswer(inv -> {
                    Libro entidad = inv.getArgument(0);
                    ReflectionTestUtils.setField(entidad, "id", siguienteLibroId.getAndIncrement());
                    libros.put(entidad.getIsbn(), entidad);
                    return entidad;
                });

        when(formatoLibroRepository.findByLibroIdAndFormato(anyLong(), any(Formato.class)))
                .thenAnswer(inv -> Optional.ofNullable(formatos.get(
                        claveFormato(inv.getArgument(0), inv.getArgument(1)))));
        when(formatoLibroRepository.save(any(FormatoLibro.class)))
                .thenAnswer(inv -> {
                    FormatoLibro entidad = inv.getArgument(0);
                    ReflectionTestUtils.setField(entidad, "id", siguienteFormatoId.getAndIncrement());
                    formatos.put(claveFormato(entidad.getLibro().getId(), entidad.getFormato()), entidad);
                    return entidad;
                });

        when(itemStockRepository.findByFormatoLibroId(anyLong()))
                .thenAnswer(inv -> Optional.ofNullable(inventario.get(inv.getArgument(0))));
        when(itemStockRepository.save(any(ItemStock.class)))
                .thenAnswer(inv -> {
                    ItemStock entidad = inv.getArgument(0);
                    inventario.put(entidad.getFormatoLibro().getId(), entidad);
                    return entidad;
                });
    }

    private FormatoLibro formatoRaudskinna() {
        Libro libro = libros.get(ISBN_RAUDSKINNA);
        assertNotNull(libro, "Debe existir Rauðskinna");
        FormatoLibro formato = formatos.get(claveFormato(libro.getId(), Formato.PASTA_DURA));
        assertNotNull(formato, "Debe existir el formato físico de Rauðskinna");
        return formato;
    }

    @Test
    void inicializarBaseVaciaCreaCatalogoCompleto() {
        configurarRepositoriosEnMemoria();

        catalogoDemoService.inicializarCatalogo();



        // Verifica el catálogo completo de demostración.
        assertEquals(23, libros.size(),
                "Deben existir veintitrés títulos diferentes");

        assertEquals(15, categorias.size(),
                "Se deben reutilizar las categorías por nombre");

        assertEquals(14, autores.size(),
                "Se deben reutilizar los autores existentes");

        assertEquals(2, editoriales.size(),
                "Se deben compartir las editoriales");

        assertEquals(23, formatos.size(),
                "Cada libro debe tener su presentación comercial");

        assertEquals(22, inventario.size(),
                "Los formatos digitales no generan inventario físico");

        verify(categoriaRepository, times(15))
                .save(any(Categoria.class));

        verify(autorRepository, times(14))
                .save(any(Autor.class));

        verify(editorialRepository, times(2))
                .save(any(Editorial.class));

        verify(libroRepository, times(23))
                .save(any(Libro.class));

        verify(formatoLibroRepository, times(23))
                .save(any(FormatoLibro.class));

        verify(itemStockRepository, times(22))
                .save(any(ItemStock.class));

        Libro raudskinna = libros.get(ISBN_RAUDSKINNA);
        assertNotNull(raudskinna);
        assertTrue(raudskinna.isActivo());
        assertEquals(1, raudskinna.getAutores().size());

        FormatoLibro formato = formatoRaudskinna();
        assertEquals(0, new BigDecimal("666.00").compareTo(formato.getPrecio()));
        ItemStock stock = inventario.get(formato.getId());
        assertNotNull(stock);
        assertEquals(13, stock.getCantidad());
        assertEquals(3, stock.getStockMinimo());
    }

    @Test
    void inicializarConDatosExistentesConservaInventario() {
        configurarRepositoriosEnMemoria();
        catalogoDemoService.inicializarCatalogo();

        FormatoLibro formato = formatoRaudskinna();
        inventario.put(formato.getId(), new ItemStock(formato, 11, 3));
        clearInvocations(categoriaRepository, autorRepository, editorialRepository,
                libroRepository, formatoLibroRepository, itemStockRepository);

        catalogoDemoService.inicializarCatalogo();

        assertEquals(23, libros.size());
        assertEquals(22, inventario.size());
        assertEquals(11, inventario.get(formato.getId()).getCantidad());
        assertEquals(3, inventario.get(formato.getId()).getStockMinimo());

        verify(categoriaRepository, never()).save(any());
        verify(autorRepository, never()).save(any());
        verify(editorialRepository, never()).save(any());
        verify(libroRepository, never()).save(any());
        verify(formatoLibroRepository, never()).save(any());
        verify(itemStockRepository, never()).save(any());
    }

    @Test
    void inicializarSinStockCreaSoloInventario() {
        configurarRepositoriosEnMemoria();
        catalogoDemoService.inicializarCatalogo();

        FormatoLibro formato = formatoRaudskinna();
        inventario.remove(formato.getId());
        clearInvocations(categoriaRepository, autorRepository, editorialRepository,
                libroRepository, formatoLibroRepository, itemStockRepository);

        catalogoDemoService.inicializarCatalogo();

        assertEquals(22, inventario.size());
        ItemStock stock = inventario.get(formato.getId());
        assertNotNull(stock);
        assertEquals(13, stock.getCantidad());
        assertEquals(3, stock.getStockMinimo());

        verify(itemStockRepository, times(1)).save(any(ItemStock.class));
        verify(categoriaRepository, never()).save(any());
        verify(autorRepository, never()).save(any());
        verify(editorialRepository, never()).save(any());
        verify(libroRepository, never()).save(any());
        verify(formatoLibroRepository, never()).save(any());
    }

    @Test
    void inicializarConCategoriaInactivaRechazaOperacion() {
        Categoria categoria = new Categoria("Ocultismo y Esoterismo");
        categoria.setActivo(false);

        when(categoriaRepository.findByNombreIgnoreCase(any(String.class)))
                .thenReturn(Optional.of(categoria));

        assertThrows(IllegalStateException.class,
                () -> catalogoDemoService.inicializarCatalogo());

        assertFalse(categoria.isActivo());
        verify(categoriaRepository, never()).save(any());
        verifyNoInteractions(autorRepository, editorialRepository, libroRepository,
                formatoLibroRepository, itemStockRepository);
    }
}


package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.model.*;
import com.itson.ProyectoAplicacionesWeb.repository.*;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Servicio encargado de preparar el catálogo inicial
 * de demostración de Librería Raudskinna.
 *
 * Se activa únicamente mediante el perfil demo.
 *
 * La inicialización es idempotente:
 * - Reutiliza los registros existentes.
 * - No duplica libros ni sus presentaciones.
 * - No modifica precios previamente registrados.
 * - No restablece inventarios después de las ventas.
 *
 * Las operaciones de persistencia permanecen en Service,
 * respetando el modelo anémico del proyecto.
 */
@Service
@Profile("demo")
public class CatalogoDemoService {

    private final CategoriaRepository categoriaRepository;
    private final AutorRepository autorRepository;
    private final EditorialRepository editorialRepository;
    private final LibroRepository libroRepository;
    private final FormatoLibroRepository formatoLibroRepository;
    private final ItemStockRepository itemStockRepository;

    public CatalogoDemoService(
            CategoriaRepository categoriaRepository,
            AutorRepository autorRepository,
            EditorialRepository editorialRepository,
            LibroRepository libroRepository,
            FormatoLibroRepository formatoLibroRepository,
            ItemStockRepository itemStockRepository) {

        this.categoriaRepository = categoriaRepository;
        this.autorRepository = autorRepository;
        this.editorialRepository = editorialRepository;
        this.libroRepository = libroRepository;
        this.formatoLibroRepository = formatoLibroRepository;
        this.itemStockRepository = itemStockRepository;
    }

    /**
     * Coordina la inicialización de los títulos demostrativos.
     *
     * Toda la operación se realiza en una transacción.
     * Posteriormente agregaremos aquí los demás libros.
     */
    @Transactional
    public void inicializarCatalogo() {

        // =====================================================
        // RAUÐSKINNA - EDICIÓN ESPECIAL DEL TOBARITO
        // =====================================================

        registrarLibroDemo(
                "Rauðskinna - Edición especial del Tobarito",
                "DEMO-RAUD-001",
                "Ocultismo y Esoterismo",
                "Gottskálk grimmi Nikulásson",
                "Ediciones El Tobarito",
                "Tobarito, Sonora, México",
                Formato.PASTA_DURA,
                new BigDecimal("666.00"),
                13,
                3,
                "Edición ficticia de demostración "
                        + "inspirada en el legendario grimorio "
                        + "islandés atribuido a Gottskálk "
                        + "grimmi Nikulásson."
        );

        // =====================================================
        // GRIMORIOS, DEMONOLOGÍA Y MANUSCRITOS ENIGMÁTICOS
        // =====================================================

        registrarLibroDemo(
                "Malleus Maleficarum",
                "DEMO-MALLEUS-001",
                "Demonología e Historia",
                "Heinrich Kramer",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_DURA,
                new BigDecimal("589.00"),
                8,
                2,
                "Tratado histórico publicado en 1487, "
                        + "relacionado con los procesos de persecución "
                        + "de la brujería en Europa. "
                        + "Edición ficticia para demostración."
        );

        registrarLibroDemo(
                "Galdrabók",
                "DEMO-GALDRA-001",
                "Ocultismo y Esoterismo",
                "Anónimo",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_DURA,
                new BigDecimal("479.00"),
                9,
                2,
                "Edición ficticia inspirada en el grimorio "
                        + "islandés de los siglos XVI y XVII, "
                        + "conocido por sus símbolos y fórmulas mágicas."
        );

        registrarLibroDemo(
                "Codex Gigas",
                "DEMO-GIGAS-001",
                "Manuscritos Medievales",
                "Anónimo",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_DURA,
                new BigDecimal("899.00"),
                5,
                1,
                "Edición ficticia inspirada en el célebre "
                        + "manuscrito medieval conocido como "
                        + "la Biblia del Diablo."
        );

        registrarLibroDemo(
                "El Gran Grimorio (El Dragón Rojo)",
                "DEMO-DRAGON-001",
                "Ocultismo y Esoterismo",
                "Anónimo",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_BLANDA,
                new BigDecimal("399.00"),
                12,
                3,
                "Edición ficticia inspirada en una obra "
                        + "de la tradición ocultista europea, "
                        + "asociada con la magia ceremonial."
        );

        registrarLibroDemo(
                "La llave menor de Salomón",
                "DEMO-SALOMON-001",
                "Magia Ceremonial",
                "Anónimo",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_BLANDA,
                new BigDecimal("429.00"),
                10,
                2,
                "Edición ficticia basada en la tradición "
                        + "de textos de magia ceremonial "
                        + "conocida como Lemegeton."
        );

        registrarLibroDemo(
                "El libro de San Cipriano",
                "DEMO-CIPRIANO-001",
                "Ocultismo y Esoterismo",
                "Anónimo",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_BLANDA,
                new BigDecimal("349.00"),
                11,
                3,
                "Edición ficticia inspirada en las tradiciones "
                        + "populares de grimorios atribuidos "
                        + "a San Cipriano."
        );

        registrarLibroDemo(
                "Oera Linda",
                "DEMO-OERA-001",
                "Manuscritos y Textos Enigmáticos",
                "Anónimo",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_DURA,
                new BigDecimal("549.00"),
                7,
                2,
                "Edición ficticia inspirada en el Oera Linda, "
                        + "texto presentado como una antigua crónica "
                        + "frisona y considerado una mistificación "
                        + "histórica del siglo XIX."
        );

        // =====================================================
        // HORROR CÓSMICO - H. P. LOVECRAFT
        // =====================================================

        registrarLibroDemo(
                "En las montañas de la locura",
                "DEMO-MONTANAS-001",
                "Horror Cósmico",
                "H. P. Lovecraft",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_BLANDA,
                new BigDecimal("389.00"),
                12,
                3,
                "Una expedición científica a la Antártida "
                        + "descubre vestigios de una civilización "
                        + "anterior a la humanidad y secretos "
                        + "que desafían la comprensión humana."
        );

        registrarLibroDemo(
                "El color que cayó del cielo",
                "DEMO-COLOR-001",
                "Horror Cósmico",
                "H. P. Lovecraft",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_BLANDA,
                new BigDecimal("299.00"),
                10,
                2,
                "Un meteorito cae cerca de una granja "
                        + "y provoca fenómenos inexplicables "
                        + "que transforman el entorno "
                        + "y amenazan a sus habitantes."
        );

        registrarLibroDemo(
                "Nyarlathotep",
                "DEMO-NYARLA-001",
                "Horror Cósmico",
                "H. P. Lovecraft",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_DURA,
                new BigDecimal("279.00"),
                9,
                2,
                "Relato de horror cósmico sobre una "
                        + "enigmática figura que recorre "
                        + "el mundo anunciando sucesos "
                        + "extraordinarios y perturbadores."
        );

        registrarLibroDemo(
                "El que acecha en la oscuridad",
                "DEMO-ACECHA-001",
                "Horror Cósmico",
                "H. P. Lovecraft",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_BLANDA,
                new BigDecimal("319.00"),
                8,
                2,
                "Un escritor investiga una iglesia "
                        + "abandonada en Providence y descubre "
                        + "un antiguo culto relacionado "
                        + "con una entidad que habita "
                        + "en la oscuridad."
        );

        // =====================================================
        // MITOS DE CTHULHU - FRANK BELKNAP LONG
        // =====================================================

        registrarLibroDemo(
                "Los perros de Tíndalos",
                "DEMO-TINDALOS-001",
                "Horror Cósmico",
                "Frank Belknap Long",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_BLANDA,
                new BigDecimal("329.00"),
                10,
                2,
                "Un investigador experimenta con "
                        + "la percepción del tiempo "
                        + "y descubre seres aterradores "
                        + "que acechan desde dimensiones "
                        + "ajenas a nuestra realidad."
        );

        // =====================================================
        // NECRONOMICÓN - EDICIÓN FICTICIA
        // =====================================================

        registrarLibroDemo(
                "Necronomicón - Edición ficticia",
                "DEMO-NECRON-001",
                "Horror Cósmico",
                "H. P. Lovecraft",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_DURA,
                new BigDecimal("666.00"),
                7,
                2,
                "Edición completamente ficticia "
                        + "inspirada en el Necronomicón, "
                        + "libro imaginario creado por "
                        + "H. P. Lovecraft dentro "
                        + "de su universo literario. "
                        + "No corresponde a un manuscrito "
                        + "histórico auténtico."
        );

        // =====================================================
        // MANGA Y HORROR - JUNJI ITO
        // =====================================================

        registrarLibroDemo(
                "Hellstar Remina",
                "DEMO-REMINA-001",
                "Manga y Horror",
                "Junji Ito",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_BLANDA,
                new BigDecimal("399.00"),
                10,
                3,
                "Una estrella errante descubierta "
                        + "por un astrónomo amenaza "
                        + "con destruir la Tierra, "
                        + "mientras su hija Remina "
                        + "se convierte en objeto "
                        + "de una persecución aterradora."
        );

        // =====================================================
        // LITERATURA INFANTIL Y CLÁSICOS
        // =====================================================

        registrarLibroDemo(
                "El principito",
                "DEMO-PRINCIPITO-001",
                "Literatura Infantil",
                "Antoine de Saint-Exupéry",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_BLANDA,
                new BigDecimal("179.00"),
                15,
                3,
                "Un pequeño príncipe viaja entre planetas "
                        + "y reflexiona sobre la amistad, "
                        + "el amor y la naturaleza humana. "
                        + "Edición ficticia de demostración."
        );

        registrarLibroDemo(
                "Orgullo y prejuicio",
                "DEMO-ORGULLO-001",
                "Romance Clásico",
                "Jane Austen",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_BLANDA,
                new BigDecimal("249.00"),
                12,
                3,
                "Elizabeth Bennet y Fitzwilliam Darcy "
                        + "enfrentan sus diferencias personales "
                        + "y los prejuicios sociales de su época."
        );

        // =====================================================
        // LITERATURA MEXICANA
        // =====================================================

        registrarLibroDemo(
                "Pedro Páramo",
                "DEMO-PARAMO-001",
                "Literatura Mexicana",
                "Juan Rulfo",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_BLANDA,
                new BigDecimal("229.00"),
                10,
                2,
                "Juan Preciado llega a Comala en busca "
                        + "de su padre y descubre un pueblo "
                        + "habitado por recuerdos, voces "
                        + "y presencias del pasado."
        );

        // =====================================================
        // CIENCIA FICCIÓN Y FANTASÍA
        // =====================================================

        registrarLibroDemo(
                "Dune",
                "DEMO-DUNE-001",
                "Ciencia Ficción",
                "Frank Herbert",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_DURA,
                new BigDecimal("499.00"),
                9,
                2,
                "Paul Atreides llega al planeta Arrakis, "
                        + "centro de conflictos políticos, "
                        + "religiosos y económicos relacionados "
                        + "con una sustancia extraordinaria."
        );

        registrarLibroDemo(
                "El Hobbit",
                "DEMO-HOBBIT-001",
                "Fantasía",
                "J. R. R. Tolkien",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_DURA,
                new BigDecimal("459.00"),
                11,
                3,
                "Bilbo Bolsón abandona la tranquilidad "
                        + "de su hogar para acompañar a un grupo "
                        + "de enanos en una aventura hacia "
                        + "la Montaña Solitaria."
        );

        // =====================================================
        // CIENCIA Y TECNOLOGÍA
        // =====================================================

        registrarLibroDemo(
                "Cosmos",
                "DEMO-COSMOS-001",
                "Divulgación Científica",
                "Carl Sagan",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_DURA,
                new BigDecimal("449.00"),
                8,
                2,
                "Una exploración del universo, "
                        + "la historia de la ciencia "
                        + "y el lugar que ocupa la humanidad "
                        + "dentro del cosmos."
        );

        /*
         * Presentación exclusivamente digital.
         * No debe generar inventario físico.
         */
        registrarLibroDemo(
                "Clean Code",
                "DEMO-CLEANCODE-001",
                "Tecnología",
                "Robert C. Martin",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.DIGITAL,
                new BigDecimal("599.00"),
                0,
                0,
                "Obra sobre principios y prácticas "
                        + "para desarrollar código legible, "
                        + "mantenible y de mayor calidad. "
                        + "Presentación digital ficticia."
        );

        // =====================================================
        // LITERATURA CONTEMPORÁNEA
        // =====================================================

        registrarLibroDemo(
                "Como agua para chocolate",
                "DEMO-CHOCOLATE-001",
                "Literatura Contemporánea",
                "Laura Esquivel",
                "Ediciones Raudskinna Demo",
                "Editorial ficticia de demostración",
                Formato.PASTA_BLANDA,
                new BigDecimal("289.00"),
                10,
                2,
                "La historia de Tita combina amor, "
                        + "tradiciones familiares, cocina "
                        + "y elementos de realismo mágico "
                        + "en el México revolucionario."
        );

        // =====================================================
        // PORTADAS LOCALES DEL CATÁLOGO DE DEMOSTRACIÓN
        // =====================================================

        asignarPortadaDemo("DEMO-RAUD-001",
                "/img/portadas/raudskinna.png");

        asignarPortadaDemo("DEMO-MALLEUS-001",
                "/img/portadas/malleus-maleficarum.png");

        asignarPortadaDemo("DEMO-GALDRA-001",
                "/img/portadas/galdrabok.png");

        asignarPortadaDemo("DEMO-GIGAS-001",
                "/img/portadas/codex-gigas.png");

        asignarPortadaDemo("DEMO-DRAGON-001",
                "/img/portadas/el-gran-grimorio.png");

        asignarPortadaDemo("DEMO-SALOMON-001",
                "/img/portadas/la-llave-menor-de-salomon.png");

        asignarPortadaDemo("DEMO-CIPRIANO-001",
                "/img/portadas/el-libro-de-san-cipriano.png");

        asignarPortadaDemo("DEMO-OERA-001",
                "/img/portadas/oera-linda.png");

        asignarPortadaDemo("DEMO-MONTANAS-001",
                "/img/portadas/montanas-locura.png");

        asignarPortadaDemo("DEMO-COLOR-001",
                "/img/portadas/el-color-que-cayo-del-cielo.png");

        asignarPortadaDemo("DEMO-NYARLA-001",
                "/img/portadas/nyarlathotep.png");

        asignarPortadaDemo("DEMO-ACECHA-001",
                "/img/portadas/el-que-acecha-en-la-oscuridad.png");

        asignarPortadaDemo("DEMO-TINDALOS-001",
                "/img/portadas/los-perros-de-tindalos.png");

        asignarPortadaDemo("DEMO-NECRON-001",
                "/img/portadas/necronomicon.png");

        asignarPortadaDemo("DEMO-REMINA-001",
                "/img/portadas/hellstar-remina.png");

        // =====================================================
        // PORTADAS - LITERATURA GENERAL
        // =====================================================

        asignarPortadaDemo("DEMO-PRINCIPITO-001",
                "/img/portadas/el-principito.png");

        asignarPortadaDemo("DEMO-ORGULLO-001",
                "/img/portadas/orgullo-y-prejuicio.png");

        asignarPortadaDemo("DEMO-PARAMO-001",
                "/img/portadas/pedro-paramo.png");

        asignarPortadaDemo("DEMO-DUNE-001",
                "/img/portadas/dune.png");

        asignarPortadaDemo("DEMO-HOBBIT-001",
                "/img/portadas/el-hobbit.png");

        asignarPortadaDemo("DEMO-COSMOS-001",
                "/img/portadas/cosmos.png");

        asignarPortadaDemo("DEMO-CLEANCODE-001",
                "/img/portadas/clean-code.png");

        asignarPortadaDemo("DEMO-CHOCOLATE-001",
                "/img/portadas/como-agua-para-chocolate.png");
    }

    /**
     * Registra una edición demostrativa y sus relaciones.
     *
     * Reutiliza los registros identificados por:
     * - Nombre de categoría.
     * - Nombre de autor.
     * - Nombre de editorial.
     * - ISBN demostrativo.
     * - Combinación de libro y formato.
     *
     * El inventario inicial se crea una sola vez.
     */
    private void registrarLibroDemo(
            String titulo,
            String isbn,
            String nombreCategoria,
            String nombreAutor,
            String nombreEditorial,
            String direccionEditorial,
            Formato formato,
            BigDecimal precio,
            int cantidad,
            int stockMinimo,
            String sinopsis) {

        // Recupera o crea la categoría.
        Categoria categoria = obtenerOCrearCategoria(
                nombreCategoria
        );

        // Recupera o crea el autor.
        Autor autor = obtenerOCrearAutor(
                nombreAutor
        );

        // Recupera o crea la editorial.
        Editorial editorial = obtenerOCrearEditorial(
                nombreEditorial,
                direccionEditorial
        );

        // Recupera o crea el libro mediante su ISBN.
        Libro libro = libroRepository.findByIsbn(isbn)
                .orElseGet(() -> {

                    Libro nuevo = new Libro(
                            titulo,
                            isbn,
                            categoria,
                            editorial
                    );

                    nuevo.setAutores(Set.of(autor));
                    nuevo.setSinopsis(sinopsis);
                    nuevo.setActivo(true);

                    return libroRepository.save(nuevo);
                });

        // Recupera o crea la presentación comercial.
        FormatoLibro formatoLibro = formatoLibroRepository
                .findByLibroIdAndFormato(
                        libro.getId(),
                        formato
                )
                .orElseGet(() -> formatoLibroRepository.save(
                        new FormatoLibro(
                                libro,
                                formato,
                                precio
                        )
                ));

        /*
         * Los formatos digitales no tienen inventario físico.
         *
         * Para formatos físicos, solamente creamos ItemStock
         * cuando todavía no existe.
         *
         * Nunca reponemos automáticamente unidades vendidas.
         */
        if (formato != Formato.DIGITAL
                && itemStockRepository.findByFormatoLibroId(
                formatoLibro.getId()).isEmpty()) {

            itemStockRepository.save(
                    new ItemStock(
                            formatoLibro,
                            cantidad,
                            stockMinimo
                    )
            );
        }
    }

    /**
     * Recupera una categoría existente o crea una nueva.
     *
     * No reactiva categorías que fueron deshabilitadas
     * por decisiones administrativas.
     */
    private Categoria obtenerOCrearCategoria(String nombre) {

        Categoria categoria = categoriaRepository
                .findByNombreIgnoreCase(nombre)
                .orElseGet(() -> categoriaRepository.save(
                        new Categoria(nombre)
                ));

        if (!categoria.isActivo()) {
            throw new IllegalStateException(
                    "La categoría de demostración está inactiva: "
                            + nombre
            );
        }

        return categoria;
    }

    /**
     * Recupera un autor existente o crea uno nuevo.
     */
    private Autor obtenerOCrearAutor(String nombre) {

        return autorRepository
                .findByNombreIgnoreCase(nombre)
                .orElseGet(() -> autorRepository.save(
                        new Autor(nombre)
                ));
    }

    /**
     * Recupera una editorial existente o crea una nueva.
     *
     * No sobrescribe su dirección si ya existe.
     */
    private Editorial obtenerOCrearEditorial(
            String nombre,
            String direccion) {

        return editorialRepository
                .findByNombreIgnoreCase(nombre)
                .orElseGet(() -> editorialRepository.save(
                        new Editorial(nombre, direccion)
                ));
    }


    /**
     * Asigna una portada local únicamente cuando el libro
     * todavía no tiene una imagen registrada.
     *
     * Respeta las URLs existentes, incluidas aquellas
     * almacenadas posteriormente mediante Cloudinary.
     *
     * La entidad se encuentra administrada por JPA dentro
     * de la transacción de inicializarCatalogo(), por lo
     * que Hibernate persiste el cambio automáticamente.
     */
    private void asignarPortadaDemo(String isbn, String rutaLocal) {

        Libro libro = libroRepository.findByIsbn(isbn)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró el libro de demostración: " + isbn
                ));

        if (libro.getPortadaUrl() == null
                || libro.getPortadaUrl().isBlank()) {

            libro.setPortadaUrl(rutaLocal);
        }
    }

}

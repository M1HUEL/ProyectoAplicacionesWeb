
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.model.Autor;
import com.itson.ProyectoAplicacionesWeb.repository.AutorRepository;
import com.itson.ProyectoAplicacionesWeb.repository.LibroRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio encargado de gestionar los autores
 * registrados en Librería Raudskinna.
 *
 * Las operaciones de negocio se implementan en
 * esta capa y la persistencia se delega
 * a AutorRepository.
 *
 */
@Service
@PreAuthorize("hasRole('ADMIN')")
public class AutorService {

    /** Repositorio utilizado para consultar autores. */
    private final AutorRepository autorRepository;

    private final LibroRepository libroRepository;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param autorRepository repositorio de autores.
     */
    public AutorService(AutorRepository autorRepository,
                        LibroRepository libroRepository) {
        this.autorRepository = autorRepository;
        this.libroRepository = libroRepository;
    }

    /**
     * Recupera todos los autores registrados.
     *
     * La transacción es de solo lectura porque
     * este método no modifica la base de datos.
     *
     * @return lista de autores registrados.
     */
    @Transactional(readOnly = true)
    public List<Autor> listarTodos() {
        return autorRepository.findAll();
    }


    /**
     * Registra un nuevo autor en Librería Raudskinna.
     *
     * Comprueba que el nombre sea obligatorio y que no
     * exceda los 150 caracteres.
     *
     * Permite registrar diferentes autores con el mismo
     * nombre, identificándolos mediante su ID.
     *
     * @param nombre nombre del autor que se desea registrar.
     * @return autor registrado en la base de datos.
     * @throws IllegalArgumentException si los datos no son válidos.
     */
    @Transactional
    public Autor registrar(String nombre) {

        // Comprueba que el nombre no sea nulo ni esté vacío.
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del autor es obligatorio."
            );
        }

        // Elimina los espacios innecesarios al inicio y al final.
        String nombreLimpio = nombre.trim();

        // Respeta la longitud máxima definida en Autor.java.
        if (nombreLimpio.length() > 150) {
            throw new IllegalArgumentException(
                    "El nombre del autor no puede superar los 150 caracteres."
            );
        }

        // Crea la entidad sin incorporar lógica de negocio en ella.
        Autor autor = new Autor(nombreLimpio);

        // Delega la persistencia del nuevo autor al repositorio.
        return autorRepository.save(autor);
    }

    /**
     * Busca un autor mediante su identificador.
     *
     * @param id identificador del autor.
     * @return autor encontrado.
     * @throws IllegalArgumentException si el autor no existe.
     */
    @Transactional(readOnly = true)
    public Autor buscarPorId(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del autor no es válido."
            );
        }

        return autorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un autor con el identificador indicado."
                ));
    }

    /**
     * Actualiza el nombre de un autor existente.
     *
     * @param id identificador del autor.
     * @param nombre nuevo nombre del autor.
     * @return autor actualizado.
     */
    @Transactional
    public Autor actualizar(Long id, String nombre) {

        Autor autor = buscarPorId(id);

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del autor es obligatorio."
            );
        }

        String nombreLimpio = nombre.trim();

        if (nombreLimpio.length() > 150) {
            throw new IllegalArgumentException(
                    "El nombre del autor no puede superar los 150 caracteres."
            );
        }

        autor.setNombre(nombreLimpio);

        return autorRepository.save(autor);
    }

    /**
     * Elimina un autor siempre que no tenga libros asociados.
     *
     * @param id identificador del autor.
     * @throws IllegalArgumentException si el autor no existe
     *         o tiene libros asociados.
     */
    @Transactional
    public void eliminar(Long id) {

        // Verifica que el autor exista.
        Autor autor = buscarPorId(id);

        // Impide eliminar autores relacionados con libros.
        if (libroRepository.existsByAutores_Id(id)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar el autor porque tiene libros asociados."
            );
        }

        // Elimina el autor cuando no existen asociaciones.
        autorRepository.delete(autor);
    }
}

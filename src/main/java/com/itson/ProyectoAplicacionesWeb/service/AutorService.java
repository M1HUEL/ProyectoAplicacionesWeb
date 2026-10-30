
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.model.Autor;
import com.itson.ProyectoAplicacionesWeb.repository.AutorRepository;
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
public class AutorService {

    /** Repositorio utilizado para consultar autores. */
    private final AutorRepository autorRepository;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param autorRepository repositorio de autores.
     */
    public AutorService(AutorRepository autorRepository) {
        this.autorRepository = autorRepository;
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
     * Comprueba que el nombre sea obligatorio, que no
     * exceda los 150 caracteres y que no exista otro
     * autor con el mismo nombre.
     *
     * @param nombre nombre del autor que se desea registrar.
     * @return autor registrado en la base de datos.
     * @throws IllegalArgumentException si los datos no son válidos.
     *
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

        // Evita registrar dos autores con el mismo nombre.
        if (autorRepository.existsByNombreIgnoreCase(nombreLimpio)) {
            throw new IllegalArgumentException(
                    "Ya existe un autor registrado con ese nombre."
            );
        }

        // Crea la entidad sin incorporar lógica de negocio en ella.
        Autor autor = new Autor(nombreLimpio);

        // Delega la persistencia del nuevo autor al repositorio.
        return autorRepository.save(autor);
    }

}

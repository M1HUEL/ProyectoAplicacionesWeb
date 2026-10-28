
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.model.Editorial;
import com.itson.ProyectoAplicacionesWeb.repository.EditorialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio encargado de gestionar las editoriales
 * registradas en Librería Raudskinna.
 *
 * Contiene las operaciones relacionadas con las editoriales
 * y delega el acceso a MySQL mediante EditorialRepository.
 *
 * Las reglas de negocio y validaciones se implementarán
 * en esta capa.
 *
 */
@Service
public class EditorialService {

    /** Repositorio utilizado para consultar editoriales. */
    private final EditorialRepository editorialRepository;

    /**
     * Constructor que recibe el repositorio mediante
     * inyección de dependencias de Spring.
     *
     * @param editorialRepository repositorio de editoriales.
     */
    public EditorialService(EditorialRepository editorialRepository) {
        this.editorialRepository = editorialRepository;
    }

    /**
     * Recupera todas las editoriales registradas
     * en la base de datos.
     *
     * La transacción es de solo lectura porque
     * no se modifican registros.
     *
     * @return lista de editoriales registradas.
     */
    @Transactional(readOnly = true)
    public List<Editorial> listarTodas() {
        return editorialRepository.findAll();
    }


    /**
     * Registra una nueva editorial en Librería Raudskinna.
     *
     * Valida el nombre obligatorio, la longitud de los campos
     * y la inexistencia de otra editorial con el mismo nombre.
     *
     * La dirección es opcional.
     *
     * @param nombre nombre de la editorial.
     * @param direccion dirección de la editorial (opcional).
     * @return editorial registrada en la base de datos.
     * @throws IllegalArgumentException si los datos no son válidos.
     *
     * Lafardilla 2026.
     */
    @Transactional
    public Editorial registrar(String nombre, String direccion) {

        // Comprueba que el nombre sea obligatorio.
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de la editorial es obligatorio."
            );
        }

        // Elimina espacios exteriores del nombre.
        String nombreLimpio = nombre.trim();

        // Respeta la longitud máxima definida en Editorial.java.
        if (nombreLimpio.length() > 150) {
            throw new IllegalArgumentException(
                    "El nombre de la editorial no puede superar los 150 caracteres."
            );
        }

        // Normaliza la dirección, que es un campo opcional.
        String direccionLimpia = direccion == null
                ? null
                : direccion.trim();

        // Convierte una dirección vacía en null.
        if (direccionLimpia != null && direccionLimpia.isEmpty()) {
            direccionLimpia = null;
        }

        // Respeta la longitud máxima del campo direccion.
        if (direccionLimpia != null && direccionLimpia.length() > 255) {
            throw new IllegalArgumentException(
                    "La dirección no puede superar los 255 caracteres."
            );
        }

        // Evita registrar editoriales con nombres duplicados.
        if (editorialRepository.existsByNombreIgnoreCase(nombreLimpio)) {
            throw new IllegalArgumentException(
                    "Ya existe una editorial registrada con ese nombre."
            );
        }

        // Crea la entidad respetando el modelo anémico.
        Editorial editorial = new Editorial(
                nombreLimpio,
                direccionLimpia
        );

        // Persiste la editorial mediante Spring Data JPA.
        return editorialRepository.save(editorial);
    }

}

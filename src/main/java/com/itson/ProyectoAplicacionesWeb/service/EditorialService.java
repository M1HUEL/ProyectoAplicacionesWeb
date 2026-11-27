
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.model.Editorial;
import com.itson.ProyectoAplicacionesWeb.repository.EditorialRepository;
import com.itson.ProyectoAplicacionesWeb.repository.LibroRepository;
import org.springframework.security.access.prepost.PreAuthorize;
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
@PreAuthorize("hasRole('ADMIN')")
public class EditorialService {

    /** Repositorio utilizado para consultar editoriales. */
    private final EditorialRepository editorialRepository;
    private final LibroRepository libroRepository;

    /**
     * Constructor que recibe el repositorio mediante
     * inyección de dependencias de Spring.
     *
     * @param editorialRepository repositorio de editoriales.
     */
    public EditorialService(EditorialRepository editorialRepository,
                            LibroRepository libroRepository) {
        this.editorialRepository = editorialRepository;
        this.libroRepository = libroRepository;
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
     * Busca una editorial mediante su identificador.
     *
     * @param id identificador de la editorial.
     * @return editorial encontrada.
     * @throws IllegalArgumentException si el identificador
     *         es inválido o la editorial no existe.
     */
    @Transactional(readOnly = true)
    public Editorial buscarPorId(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "El identificador de la editorial no es válido."
            );
        }

        return editorialRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe una editorial con el identificador indicado."
                ));
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

    /**
     * Actualiza los datos de una editorial existente.
     *
     * Valida el nombre obligatorio, la longitud de los campos
     * y que no exista otra editorial con el mismo nombre.
     *
     * @param id identificador de la editorial.
     * @param nombre nuevo nombre de la editorial.
     * @param direccion nueva dirección, opcional.
     * @return editorial actualizada.
     */
    @Transactional
    public Editorial actualizar(Long id, String nombre, String direccion) {

        Editorial editorial = buscarPorId(id);

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de la editorial es obligatorio."
            );
        }

        String nombreLimpio = nombre.trim();

        if (nombreLimpio.length() > 150) {
            throw new IllegalArgumentException(
                    "El nombre de la editorial no puede superar los 150 caracteres."
            );
        }

        String direccionLimpia = direccion == null
                ? null
                : direccion.trim();

        if (direccionLimpia != null && direccionLimpia.isEmpty()) {
            direccionLimpia = null;
        }

        if (direccionLimpia != null && direccionLimpia.length() > 255) {
            throw new IllegalArgumentException(
                    "La dirección no puede superar los 255 caracteres."
            );
        }

        // Permite conservar el nombre de la propia editorial.
        if (!editorial.getNombre().equalsIgnoreCase(nombreLimpio)
                && editorialRepository.existsByNombreIgnoreCase(nombreLimpio)) {
            throw new IllegalArgumentException(
                    "Ya existe una editorial registrada con ese nombre."
            );
        }

        editorial.setNombre(nombreLimpio);
        editorial.setDireccion(direccionLimpia);

        return editorialRepository.save(editorial);
    }

    /**
     * Elimina una editorial que no tenga libros asociados.
     *
     * @param id identificador de la editorial.
     * @throws IllegalArgumentException si la editorial
     *         no existe o tiene libros asociados.
     */
    @Transactional
    public void eliminar(Long id) {

        Editorial editorial = buscarPorId(id);

        // Protege tanto libros activos como inactivos.
        if (libroRepository.existsByEditorial_Id(id)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar la editorial porque tiene libros asociados."
            );
        }

        editorialRepository.delete(editorial);
    }

}

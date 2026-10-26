
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.model.Categoria;
import com.itson.ProyectoAplicacionesWeb.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio encargado de administrar las categorías
 * del catálogo de Librería Raudskinna.
 *
 * Contiene las reglas de negocio y validaciones.
 * La persistencia se delega a CategoriaRepository.
 */
@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    /**
     * Inyección de dependencias mediante constructor.
     */
    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    /**
     * Obtiene todas las categorías registradas.
     */
    @Transactional(readOnly = true)
    public List<Categoria> listarTodas() {
        return categoriaRepository.findAll();
    }

    /**
     * Busca una categoría por su identificador.
     *
     * @throws IllegalArgumentException si no existe.
     */
    @Transactional(readOnly = true)
    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró la categoría solicitada."));
    }

    /**
     * Registra una nueva categoría.
     *
     * Valida que el nombre sea obligatorio, no exceda
     * la longitud permitida y no esté duplicado.
     */
    @Transactional
    public Categoria registrar(String nombre) {

        String nombreValidado = validarNombre(nombre);

        if (categoriaRepository.existsByNombreIgnoreCase(nombreValidado)) {
            throw new IllegalArgumentException(
                    "Ya existe una categoría con ese nombre.");
        }

        return categoriaRepository.save(new Categoria(nombreValidado));
    }

    /**
     * Actualiza el nombre de una categoría existente.
     */
    @Transactional
    public Categoria actualizar(Long id, String nombre) {

        Categoria categoria = buscarPorId(id);
        String nombreValidado = validarNombre(nombre);

        categoriaRepository.findByNombreIgnoreCase(nombreValidado)
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new IllegalArgumentException(
                            "Ya existe otra categoría con ese nombre.");
                });

        categoria.setNombre(nombreValidado);

        return categoriaRepository.save(categoria);
    }

    /**
     * Desactiva una categoría sin eliminarla físicamente.
     *
     * La categoría permanece almacenada en MySQL para
     * conservar sus relaciones con los libros existentes.
     *
     * @param id Identificador de la categoría.
     * @throws IllegalArgumentException si no existe
     *                                  o ya está desactivada.
     */
    @Transactional
    public void desactivar(Long id) {

        Categoria categoria = buscarPorId(id);

        if (!categoria.isActivo()) {
            throw new IllegalArgumentException(
                    "La categoría ya se encuentra desactivada."
            );
        }

        categoria.setActivo(false);

        categoriaRepository.save(categoria);
    }

    /**
     * Verifica el nombre de una categoría.
     *
     * Esta validación corresponde a Service y no a Model.
     */
    private String validarNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de la categoría es obligatorio.");
        }

        String nombreNormalizado = nombre.trim();

        if (nombreNormalizado.length() > 100) {
            throw new IllegalArgumentException(
                    "El nombre no puede superar los 100 caracteres.");
        }

        return nombreNormalizado;
    }

}

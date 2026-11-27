
package com.itson.ProyectoAplicacionesWeb.service;

import com.itson.ProyectoAplicacionesWeb.dto.DireccionRequestDTO;
import com.itson.ProyectoAplicacionesWeb.model.Direccion;
import com.itson.ProyectoAplicacionesWeb.model.Usuario;
import com.itson.ProyectoAplicacionesWeb.repository.DireccionRepository;
import com.itson.ProyectoAplicacionesWeb.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestiona las direcciones de envío de los clientes.
 *
 * Las reglas de negocio y las validaciones
 * se implementan exclusivamente en esta capa.
 */
@Service
public class DireccionService {

    private final DireccionRepository direccionRepository;
    private final UsuarioRepository usuarioRepository;

    public DireccionService(
            DireccionRepository direccionRepository,
            UsuarioRepository usuarioRepository) {

        this.direccionRepository = direccionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Busca al cliente mediante su correo autenticado.
     */
    private Usuario obtenerUsuario(String correo) {

        if (correo == null || correo.isBlank()) {
            throw new IllegalArgumentException(
                    "El correo del cliente no es válido."
            );
        }

        return usuarioRepository.findByCorreo(correo.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "El usuario autenticado no existe."
                ));
    }

    /**
     * Recupera las direcciones del cliente.
     */
    @Transactional(readOnly = true)
    public List<Direccion> listarPorUsuario(String correo) {

        Usuario usuario = obtenerUsuario(correo);

        return direccionRepository.findByUsuario_Id(
                usuario.getId()
        );
    }

    /**
     * Busca una dirección que pertenezca al cliente.
     */
    @Transactional(readOnly = true)
    public Direccion buscarPropia(
            String correo,
            Long direccionId) {

        Usuario usuario = obtenerUsuario(correo);

        return obtenerDireccionPropia(
                direccionId,
                usuario.getId()
        );
    }

    private Direccion obtenerDireccionPropia(
            Long direccionId,
            Long usuarioId) {

        if (direccionId == null || direccionId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador de la dirección no es válido."
            );
        }

        return direccionRepository
                .findByIdAndUsuario_Id(direccionId, usuarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "La dirección no existe o no pertenece al cliente."
                ));
    }

    /**
     * Registra una nueva dirección para el cliente.
     */
    @Transactional
    public Direccion registrar(
            String correo,
            DireccionRequestDTO datos) {

        Usuario usuario = obtenerUsuario(correo);

        validarDatos(datos);

        Direccion direccion = new Direccion(usuario);

        asignarDatos(direccion, datos);

        return direccionRepository.save(direccion);
    }

    /**
     * Actualiza exclusivamente una dirección propia.
     */
    @Transactional
    public Direccion actualizar(
            String correo,
            Long direccionId,
            DireccionRequestDTO datos) {

        Usuario usuario = obtenerUsuario(correo);

        Direccion direccion = obtenerDireccionPropia(
                direccionId,
                usuario.getId()
        );

        validarDatos(datos);
        asignarDatos(direccion, datos);

        return direccionRepository.save(direccion);
    }

    /**
     * Elimina una dirección del cliente.
     */
    @Transactional
    public void eliminar(String correo, Long direccionId) {

        Usuario usuario = obtenerUsuario(correo);

        Direccion direccion = obtenerDireccionPropia(
                direccionId,
                usuario.getId()
        );

        direccionRepository.delete(direccion);
    }

    /**
     * Comprueba los datos obligatorios y sus longitudes.
     */
    private void validarDatos(DireccionRequestDTO datos) {

        if (datos == null) {
            throw new IllegalArgumentException(
                    "Los datos de la dirección son obligatorios."
            );
        }

        validarObligatorio(datos.nombreDestinatario(),
                "El nombre del destinatario", 150);

        validarObligatorio(datos.calle(),
                "La calle", 150);

        validarObligatorio(datos.numeroExterior(),
                "El número exterior", 20);

        validarOpcional(datos.numeroInterior(),
                "El número interior", 20);

        validarObligatorio(datos.colonia(),
                "La colonia", 100);

        validarObligatorio(datos.ciudad(),
                "La ciudad", 100);

        validarObligatorio(datos.estado(),
                "El estado", 100);

        validarObligatorio(datos.pais(),
                "El país", 100);

        validarOpcional(datos.referencias(),
                "Las referencias", 500);

        if (datos.codigoPostal() == null
                || !datos.codigoPostal().trim().matches("[0-9]{5}")) {

            throw new IllegalArgumentException(
                    "El código postal debe contener 5 dígitos."
            );
        }
    }

    private void validarObligatorio(
            String valor,
            String campo,
            int longitudMaxima) {

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    campo + " es obligatorio."
            );
        }

        if (valor.trim().length() > longitudMaxima) {
            throw new IllegalArgumentException(
                    campo + " supera los "
                            + longitudMaxima + " caracteres."
            );
        }
    }

    private void validarOpcional(
            String valor,
            String campo,
            int longitudMaxima) {

        if (valor != null
                && valor.trim().length() > longitudMaxima) {

            throw new IllegalArgumentException(
                    campo + " supera los "
                            + longitudMaxima + " caracteres."
            );
        }
    }

    /**
     * Asigna los datos previamente validados.
     * Los valores opcionales vacíos se guardan como null.
     */
    private void asignarDatos(
            Direccion direccion,
            DireccionRequestDTO datos) {

        direccion.setNombreDestinatario(
                datos.nombreDestinatario().trim());

        direccion.setCalle(datos.calle().trim());

        direccion.setNumeroExterior(
                datos.numeroExterior().trim());

        direccion.setNumeroInterior(
                normalizarOpcional(datos.numeroInterior()));

        direccion.setColonia(datos.colonia().trim());

        direccion.setCodigoPostal(
                datos.codigoPostal().trim());

        direccion.setCiudad(datos.ciudad().trim());
        direccion.setEstado(datos.estado().trim());
        direccion.setPais(datos.pais().trim());

        direccion.setReferencias(
                normalizarOpcional(datos.referencias()));
    }

    private String normalizarOpcional(String valor) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }
}

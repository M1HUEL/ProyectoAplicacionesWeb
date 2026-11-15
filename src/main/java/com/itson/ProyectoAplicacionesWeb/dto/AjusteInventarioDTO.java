package com.itson.ProyectoAplicacionesWeb.dto;

import com.itson.ProyectoAplicacionesWeb.model.MotivoAjuste;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AjusteInventarioDTO {

    /** Unidades a sumar (positivo) o restar (negativo). */
    private Integer diferencia;

    /** Motivo del ajuste. */
    private MotivoAjuste motivo;

    /** Comentario opcional. */
    private String nota;
}

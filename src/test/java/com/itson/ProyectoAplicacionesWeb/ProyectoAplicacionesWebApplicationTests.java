
package com.itson.ProyectoAplicacionesWeb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Prueba de inicialización del contexto de Spring Boot
 * para Librería Raudskinna.
 *
 * Utiliza el perfil integrationtest con H2 en memoria,
 * evitando depender de las credenciales MySQL locales
 * durante la ejecución automatizada de pruebas.
 */
@SpringBootTest
@ActiveProfiles("integrationtest")
class ProyectoAplicacionesWebApplicationTests {

	/**
	 * Comprueba que el contexto de Spring Boot pueda
	 * inicializarse correctamente con sus dependencias.
	 */
	@Test
	void contextLoads() {
		// El arranque exitoso del contexto constituye la prueba.
	}
}

# Librería Raudskinna 📚

Proyecto académico de **Aplicaciones Web**: plataforma de comercio electrónico de libros desarrollada con Spring Boot, Thymeleaf y MySQL.

> **Estado al 25 de octubre de 2026:** desarrollo en curso. Este documento distingue funcionalidades probadas de las pendientes. La rama de trabajo es `feat-administracion`.

## Tecnologías

- Java 21 y Maven Wrapper (`bash ./mvnw ...`).
- Spring Boot 4.1.2, Spring MVC, Spring Security 7, Spring Data JPA / Hibernate 7.
- Thymeleaf para las vistas; Lombok para reducir código repetitivo.
- MySQL 8.4 para persistencia.
- JUnit y Mockito para pruebas unitarias; Postman para peticiones HTTP.

## Arquitectura

Se mantiene separación de responsabilidades:

```text
Navegador / Postman
        |
     Controller          rutas HTTP y respuestas / vistas
        |
      Service            reglas de negocio y validaciones
        |
     Repository          acceso a datos con Spring Data JPA
        |
       Model             entidades JPA, relaciones y atributos
        |
       MySQL
```

**Criterio del curso:** las entidades `Model` no deben contener lógica de negocio. Las reglas y validaciones corresponden a `Service`. No usar `@Data` indiscriminadamente en entidades JPA.

## Preparación local

1. Tener Java 21 y MySQL disponibles.
2. Crear previamente la base de datos de desarrollo `libreria_raudskinna` y un usuario MySQL con permisos apropiados; **no usar `root` como usuario habitual de la aplicación**.
3. Configurar variables de entorno en IntelliJ (Run Configuration) o en la terminal:

   - `DB_USERNAME`: usuario MySQL.
   - `DB_PASSWORD`: contraseña MySQL.
   - `RAUDSKINNA_ADMIN_PASSWORD`: contraseña inicial del administrador de desarrollo.

   **Nunca subir credenciales reales al repositorio.** `application.properties` utiliza `${DB_USERNAME}` y `${DB_PASSWORD}`.

4. En Bash, para solicitar contraseñas sin escribirlas en el historial:

   ```bash
   export DB_USERNAME="raudskinna_app"  # Ajustar al usuario local
   read -rsp "Clave MySQL: " DB_PASSWORD; echo; export DB_PASSWORD
   read -rsp "Clave admin inicial: " RAUDSKINNA_ADMIN_PASSWORD; echo; export RAUDSKINNA_ADMIN_PASSWORD
   ```

5. Ejecutar:

   ```bash
   bash ./mvnw clean test
   bash ./mvnw spring-boot:run
   ```

6. Abrir `http://localhost:8080/` o `http://localhost:8080/admin`.

**Nota:** las variables definidas en IntelliJ no se exportan automáticamente a Bash. La prueba `contextLoads()` necesita poder iniciar el contexto y conectar con MySQL. Con las variables configuradas se comprobaron **8 pruebas aprobadas, 0 errores**.

**Datos:** `spring.jpa.hibernate.ddl-auto=update` se usa en desarrollo, no como sistema de migración para producción. Conviene respaldar la base antes de cambios estructurales.

## Modelo JPA disponible

| Entidad / tipo | Propósito |
|---|---|
| `Usuario`, `Rol` | Acceso y roles de usuario |
| `Categoria` | Clasificación de libros |
| `Autor` | Autoría |
| `Editorial` | Datos editoriales |
| `Libro` | Datos bibliográficos y relaciones |
| `Formato` | Enum: `PASTA_DURA`, `PASTA_BLANDA`, `DIGITAL` |
| `FormatoLibro` | Formato y precio por libro |
| `ItemStock` | Cantidad y stock mínimo por formato |

Relaciones principales: `Libro` → `Categoria` y `Editorial` (`ManyToOne`); `Libro` ↔ `Autor` (`ManyToMany`); `FormatoLibro` → `Libro`; `ItemStock` → `FormatoLibro` (`OneToOne`). **La existencia de la entidad no significa que su administración web ya esté terminada.**

## Estado funcional verificado

- Inicio y panel administrativo con plantillas Thymeleaf.
- Seguridad: acceso a `/admin/**` restringido a `ROLE_ADMIN`, login y logout; protección CSRF habilitada.
- Módulo de categorías: listar, registrar y editar desde formularios administrativos; validaciones de duplicados.
- Endpoint HTTP PATCH para modificar el nombre de una categoría. Se obtuvo **200 OK con respuesta JSON** mediante Postman usando sesión ADMIN y token CSRF.
- Siete pruebas unitarias de `CategoriaService` aprobadas; prueba de inicio de contexto aprobada cuando MySQL y variables están disponibles.

### Rutas del módulo Categorías

| Método | Ruta | Descripción | Estado |
|---|---|---|---|
| GET | `/admin/categorias` | Lista categorías | Probado en navegador |
| GET | `/admin/categorias/nueva` | Formulario de registro | Probado |
| POST | `/admin/categorias` | Registra categoría | Probado |
| GET | `/admin/categorias/{id}/editar` | Formulario de edición | Probado |
| POST | `/admin/categorias/{id}/editar` | Actualiza desde formulario | Probado |
| PATCH | `/admin/categorias/{id}` | Actualiza el nombre mediante JSON | Probado con Postman |
| DELETE / baja lógica | Por definir | Desactivación de categorías | **Pendiente** |

Ejemplo de petición PATCH autenticada:

```http
PATCH /admin/categorias/1 HTTP/1.1
Content-Type: application/json
X-CSRF-TOKEN: <token de la sesión>
Cookie: JSESSIONID=<cookie de la sesión>

{"nombre":"Fantasía y aventura"}
```

**No incluir tokens o cookies reales en documentación ni capturas compartidas.** Un `200 OK` con HTML puede ser resultado de una redirección seguida automáticamente por Postman: validar que el resultado esperado sea JSON y, ante dudas, desactivar redirecciones automáticas.

## Pendientes prioritarios

1. Terminar módulo `Categoria`: pruebas negativas de PATCH (nombre duplicado, identificador inexistente, JSON inválido y campos no permitidos); revisar códigos HTTP (`400` vs `404`).
2. Definir e implementar **soft delete** para categorías, su consulta/visibilidad y la protección de referencias desde libros. **Todavía no está implementado.**
3. Definir cómo atender PUT y DELETE conforme a las indicaciones del tercer avance; no confundir métodos HTTP con operaciones de baja lógica.
4. Completar repositorios, servicios, controladores y vistas de libros, formatos, autores, editoriales e inventario.
5. Implementar carrito, checkout, órdenes y estados de compra según alcance del proyecto.
6. Reforzar pruebas automatizadas y mensajes/estilos de validación en vistas.

**Importante:** existen algunas plantillas administrativas de libros y órdenes como prototipos visuales; no asumir que sus acciones ya están conectadas al backend.

## Git y colaboración

- Rama de desarrollo usada: `feat-administracion`.
- Punto de recuperación confirmado: `4952a40` — `feat: integrar dominio JPA y administracion de categorias`.
- **PATCH se probó después de ese commit**; verificar que exista el commit posterior y que se haya subido antes de depender del endpoint en otra computadora.

Comandos habituales:

```bash
git branch --show-current
git status
git log -3 --oneline
bash ./mvnw clean test
git diff --check
```

Para publicar una rama propia por primera vez (si hay permiso de escritura al repositorio remoto):

```bash
git push -u origin feat-administracion
```

**No mezclar directamente a `main` sin revisión y coordinación del equipo.** Compartir con compañeros el enlace de la rama y, si procede, abrir un Pull Request.

## Registro breve de avance

**25/oct/2026:** se integraron modelo JPA, autenticación, panel administrativo y operaciones de registro/consulta/edición de categorías; se comprobaron ocho pruebas. Se añadió y verificó con Postman el endpoint PATCH para categorías. Próxima sesión: validaciones de PATCH, eliminación lógica y siguientes módulos.

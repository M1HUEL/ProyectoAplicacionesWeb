# Librería Raudskinna 📚

Proyecto académico de **Aplicaciones Web**: plataforma de comercio electrónico de libros desarrollada con Spring Boot, Thymeleaf y MySQL.

> **Estado al 29 de octubre de 2026:** tercer avance en desarrollo y revisión. La implementación administrativa se encuentra publicada en la rama `feat-administracion`. Este documento distingue funcionalidades verificadas, limitaciones y tareas pendientes.

## 1. Tecnologías

- Java 21 y Maven Wrapper.
- Spring Boot 4.1.2, Spring MVC y Spring Security 7.
- Spring Data JPA / Hibernate 7.
- Thymeleaf para las vistas y Lombok en las entidades.
- MySQL 8.4 para persistencia.
- JUnit 5 y Mockito para pruebas automatizadas.
- Postman para pruebas HTTP.

## 2. Arquitectura

El proyecto mantiene una separación de responsabilidades:

```text
Navegador / Cliente HTTP
          |
      Controller
          |
        Service
          |
      Repository
          |
         MySQL

Model: entidades JPA utilizadas por las capas.
```

**Controller:** recibe solicitudes HTTP, prepara los datos de las vistas y controla las redirecciones.

**Service:** contiene las reglas de negocio y validaciones.

**Repository:** utiliza Spring Data JPA para consultar y almacenar información.

**Model:** representa las entidades, sus atributos y relaciones. No contiene lógica de negocio, conforme al criterio del curso.

Los formularios administrativos siguen el patrón **POST/Redirect/GET**.

## 3. Preparación local

1. Instalar y configurar Java 21 y MySQL.
2. Crear la base de datos `libreria_raudskinna`.
3. Crear un usuario MySQL con los permisos necesarios. Evitar utilizar `root` como usuario habitual de la aplicación.
4. Configurar las variables de entorno requeridas por `application.properties`, incluyendo `DB_USERNAME`, `DB_PASSWORD` y `RAUDSKINNA_ADMIN_PASSWORD`. Revisar también la URL de conexión configurada.
5. Ejecutar el proyecto desde IntelliJ IDEA o mediante Maven Wrapper.

Para ejecutar las pruebas desde Bash:

```bash
bash ./mvnw test
```

Para iniciar la aplicación:

```bash
bash ./mvnw spring-boot:run
```

**Importante:** las variables configuradas en IntelliJ no se transfieren automáticamente a Git Bash. La prueba de carga del contexto necesita una configuración válida de MySQL.

No subir credenciales reales, tokens de sesión ni contraseñas al repositorio.

Se utiliza `spring.jpa.hibernate.ddl-auto=update` en desarrollo. No debe considerarse un mecanismo de migraciones para producción.

## 4. Seguridad y autenticación

La aplicación utiliza Spring Security para:

- Validar credenciales contra los usuarios registrados.
- Restringir `/admin` y `/admin/**` a usuarios con `ROLE_ADMIN`.
- Permitir el acceso público a `/admin/login`.
- Almacenar contraseñas utilizando BCrypt.
- Mantener CSRF habilitado.
- Invalidar la sesión HTTP al cerrar sesión.

El acceso administrativo y el cierre de sesión fueron comprobados desde el navegador. También se verificó la inclusión del campo `_csrf` en formularios POST administrativos.

## Acceso administrativo para pruebas

El proyecto incluye la clase `DataInitializer`, encargada de crear automáticamente una cuenta administrativa inicial cuando arranca Spring Boot.

**No es necesario insertar manualmente un administrador desde MySQL.**

### Configuración

1. Crear la base de datos `libreria_raudskinna` y configurar la conexión MySQL.
2. Establecer la variable de entorno `RAUDSKINNA_ADMIN_PASSWORD` con una contraseña de prueba.
3. Ejecutar Spring Boot.
4. Si no existe el usuario `admin@raudskinna.com`, el sistema lo registra automáticamente con rol `ADMIN`.
5. Acceder a `http://localhost:8080/admin/login` e iniciar sesión utilizando el correo administrativo y la contraseña configurada.

**Credenciales:**

- **Correo:** `admin@raudskinna.com`
- **Contraseña:** valor de `RAUDSKINNA_ADMIN_PASSWORD`

La contraseña se codifica mediante `PasswordEncoder` antes de almacenarse en MySQL. La contraseña original no se guarda en la tabla `usuarios`.

**Consideraciones importantes:**

- La variable de entorno debe estar configurada antes de iniciar la aplicación.
- Si la cuenta administrativa ya existe, `DataInitializer` no modifica sus credenciales.
- Cambiar `RAUDSKINNA_ADMIN_PASSWORD` después de crear la cuenta no actualiza automáticamente la contraseña existente.
- No publicar contraseñas reales en GitHub.
- Cada integrante puede utilizar una contraseña de prueba diferente en su propio entorno local.

## 5. Funcionalidades administrativas

### 5.1. Panel principal

Ruta: `/admin`

Muestra accesos a los módulos administrativos e indicadores consultados desde MySQL:

- Cantidad de libros registrados.
- Cantidad de órdenes pendientes.

### 5.2. Libros e inventario

Ruta principal: `/admin/libros`

Funcionalidades implementadas:

- Consultar libros y sus presentaciones.
- Registrar libros.
- Editar la información de libros y formatos.
- Seleccionar autores, editorial y categoría.
- Definir precio de la presentación.
- Gestionar cantidad disponible y stock mínimo de formatos físicos.
- Identificar estados de inventario.

Los formatos comerciales disponibles son `PASTA_DURA`, `PASTA_BLANDA` y `DIGITAL`.

Los formatos digitales no utilizan inventario físico.

### 5.3. Catálogos auxiliares

| Módulo | Listado | Registro |
|---|---|---|
| Autores | `/admin/autores` | `/admin/autores/nuevo` |
| Editoriales | `/admin/editoriales` | `/admin/editoriales/nueva` |
| Categorías | `/admin/categorias` | `/admin/categorias/nueva` |

Desde el formulario de libros existen enlaces para registrar autores, editoriales y categorías.

Los enlaces abren otra pestaña. Después del registro, es necesario **actualizar el formulario original** para visualizar las nuevas opciones.

**Precaución:** actualizar el formulario puede eliminar información del libro que todavía no se haya guardado.

Las categorías cuentan también con edición y desactivación lógica. Las categorías inactivas no aparecen entre las opciones disponibles al registrar libros.

### 5.4. Órdenes de compra

Ruta: `/admin/ordenes`

El módulo permite consultar órdenes almacenadas en MySQL y cambiar su estado conforme a reglas de negocio definidas en `OrdenCompraService`.

Transiciones permitidas:

| Estado actual | Siguientes estados permitidos |
|---|---|
| PENDIENTE | PROCESANDO, CANCELADO |
| PROCESANDO | ENVIADO, CANCELADO |
| ENVIADO | ENTREGADO |
| ENTREGADO | Ninguno |
| CANCELADO | Ninguno |

Se utilizan formularios POST, redirecciones y mensajes de confirmación o error.

**Limitación importante:** las órdenes empleadas para comprobar el módulo administrativo se insertaron como datos de prueba en MySQL. La administración de órdenes no debe confundirse con un proceso de checkout completo ya implementado.

## 6. Modelo de datos y relaciones

La estructura relacional actual contiene diez tablas principales:

`usuarios`, `ordenes_compra`, `detalles_orden`, `libros`, `formatos_libro`, `items_stock`, `categorias`, `editoriales`, `autores` y `libros_autores`.

### Multiplicidades principales

| Relación | Multiplicidad |
|---|---|
| Usuario → OrdenCompra | 1:N |
| OrdenCompra → DetalleOrden | 1:N |
| FormatoLibro → DetalleOrden | 1:N |
| Libro → FormatoLibro | 1:N |
| FormatoLibro → ItemStock | 1:0..1 |
| Categoria → Libro | 1:N |
| Editorial → Libro | 1:N |
| Libro ↔ Autor | N:M |

La relación muchos a muchos entre libros y autores se implementa mediante la tabla intermedia `libros_autores`.

La columna `items_stock.formato_libro_id` tiene una restricción `UNIQUE`, por lo que una presentación comercial puede tener como máximo un registro de inventario.

Para el diagrama entidad–relación se recomienda revisar tanto las entidades JPA como las restricciones reales de MySQL: claves primarias, claves foráneas, nulabilidad e índices.

El esquema EER fue generado mediante ingeniería inversa desde MySQL Workbench y exportado a PDF.

## 7. Pruebas realizadas

Última ejecución completa verificada: **29 de octubre de 2026**.

| Clase | Pruebas |
|---|---:|
| ProyectoAplicacionesWebApplicationTests | 1 |
| AutorServiceTest | 4 |
| CategoriaServiceTest | 12 |
| EditorialServiceTest | 5 |
| LibroServiceTest | 23 |
| OrdenCompraServiceTest | 9 |
| **Total** | **54** |

Resultado:

```text
Tests run: 54, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Posteriormente se realizaron ajustes en las plantillas HTML de los módulos administrativos. Se recomienda ejecutar nuevamente la batería completa antes de la entrega.

También se verificaron visualmente las páginas administrativas en una resolución móvil de 375 × 825 píxeles.

## 8. Limitaciones y puntos para revisión

- El proceso completo de compra y checkout debe distinguirse de la administración de órdenes ya implementada.
- Los selectores de autores, editoriales y categorías no se actualizan automáticamente al registrar datos desde otra pestaña.
- La desactivación de categorías se realiza mediante POST sin una pantalla adicional de confirmación.
- Las pruebas unitarias con Mockito no sustituyen las pruebas integrales contra una base de datos real.
- Conviene revisar el comportamiento de las pantallas en dispositivos y resoluciones adicionales.

Si algún integrante detecta errores, problemas de validación, inconsistencias entre el código y los diagramas o aspectos que deban corregirse, comunicarlo al equipo antes de integrar cambios.

## 9. Git y colaboración

Repositorio:

https://github.com/M1HUEL/ProyectoAplicacionesWeb

Rama del avance:

`feat-administracion`

Último commit confirmado:

```text
7bab5c9 style: unificar vistas administrativas de autores editoriales y categorias
```

Para consultar el avance:

```bash
git branch --show-current
git status
git log -5 --oneline
```

Se recomienda realizar nuevas modificaciones en ramas propias, revisarlas en equipo y no fusionarlas directamente a `main` sin coordinación.

## 10. Preparación del video del tercer avance

El entregable requiere un video de **5 a 10 minutos** que explique el avance y demuestre las funcionalidades desarrolladas.

Temas sugeridos:

1. Arquitectura y organización del proyecto.
2. Inicio de sesión y protección del panel administrativo.
3. Panel principal e indicadores reales.
4. Administración de libros, formatos e inventario.
5. Registro de autores, editoriales y categorías.
6. Administración de órdenes y transiciones de estado.
7. Modelo de datos y sus relaciones.
8. Cierre de sesión y conclusiones.

Cada integrante debe revisar previamente los controladores, servicios, repositorios y entidades relacionados con la parte que vaya a explicar.

**Objetivo del equipo:** llegar a la grabación con el código revisado, el modelo de datos comprendido y las funcionalidades que se mostrarán previamente comprobadas.

---

**Registro del avance — 29/oct/2026:** módulos administrativos implementados y revisados, indicadores reales, pruebas automatizadas satisfactorias, diagrama EER generado y mejoras de navegación y responsividad publicadas en `feat-administracion`.## 11. Scripts SQL para insertar registros (MySQL)

Asegurate de tener la base de datos `libreria_raudskinna` creada. Ejecuta en orden, respetando claves foraneas.

### 11.1. Catalogos base

```sql
-- Categorias
INSERT INTO categorias (nombre, activo) VALUES
('Novela', 1),
('Ficcion', 1),
('Misterio', 1);

-- Editoriales
INSERT INTO editoriales (nombre) VALUES
('Editorial Planeta'),
('Alfaguara'),
('Anagrama');

-- Autores
INSERT INTO autores (nombre) VALUES
('Gabriel Garcia Marquez'),
('Isabel Allende'),
('Julio Cortazar');
```
### 11.2. Libros

Reemplaza `categoria_id` y `editorial_id` con los IDs generados.

```sql
-- Libro
INSERT INTO libros (titulo, isbn, sinopsis, portada_url, numero_paginas, fecha_publicacion, categoria_id, editorial_id)
VALUES
('Cien anos de soledad', '9788437604947', 'Realismo magico clasico.', NULL, 471, '1967-05-30', 1, 1),
('La casa de los espiritus', '9788401352836', 'Saga familiar chilena.', NULL, 448, '1982-01-01', 1, 2),
('Rayuela', '9788432216060', 'Novela experimental.', NULL, 635, '1963-06-28', 1, 3);
```

### 11.3. Relacion libros_autores

```sql
-- libros_autores (libro_id, autor_id)
INSERT INTO libros_autores (libro_id, autor_id) VALUES
(1, 1),
(2, 2),
(3, 3);
```

### 11.4. Formatos y stock

Los formatos permitidos: `PASTA_DURA`, `PASTA_BLANDA`, `DIGITAL`. Los formatos digitales no llevan stock fisico.

```sql
-- Formatos
INSERT INTO formatos_libro (libro_id, formato, precio) VALUES
(1, 'PASTA_DURA', 899.00),
(1, 'PASTA_BLANDA', 599.00),
(1, 'DIGITAL', 299.00),
(2, 'PASTA_BLANDA', 549.00),
(2, 'DIGITAL', 279.00),
(3, 'PASTA_DURA', 749.00);

-- Stock para formatos fisicos (items_stock con formato_libro_id unico)
INSERT INTO items_stock (formato_libro_id, cantidad, stock_minimo) VALUES
(1, 50, 10),
(2, 80, 15),
(4, 40, 10),
(6, 25, 5);
```

### 11.5. Usuario administrador (opcional si ya existe via DataInitializer)

`DataInitializer` crea `admin@raudskinna.com` con `RAUDSKINNA_ADMIN_PASSWORD`. Si prefieres crearlo manualmente, guarda el hash BCrypt.

```sql
-- Ejemplo (ajusta la contrasena hasheada)
INSERT INTO usuarios (nombre, correo, contrasena, rol)
VALUES ('Administrador', 'admin@raudskinna.com', '$2a$10$REEMPLAZAR_POR_HASH_BCRYPT', 'ADMIN');
```

### 11.6. Orden de prueba

```sql
-- Orden de compra (requiere usuario_id)
INSERT INTO ordenes_compra (usuario_id, fecha, estado, total)
VALUES (1, NOW(), 'PENDIENTE', 899.00);

-- Detalle de orden
INSERT INTO detalles_orden (orden_compra_id, formato_libro_id, cantidad, precio_unitario)
VALUES (1, 1, 1, 899.00);
```

## 12. Endpoints expuestos

Esta sección documenta los endpoints del lado del cliente. La aplicación tiene dos mecanismos de seguridad:

- **API REST (`/api/**`):** autenticación con **JWT Bearer**. CSRF deshabilitado.
- **Vistas y sesión de cliente (resto de rutas):** autenticación con **sesión HTTP**. CSRF habilitado en formularios y en `PATCH`/`DELETE`.

Las rutas administrativas (`/admin/**`) y las páginas que solo muestran formularios se omiten de esta documentación.

### 12.1. API REST pública

| Método | Ruta | Autenticación | Body / parámetros | Descripción | Respuestas |
|---|---|---|---|---|---|
| `POST` | `/api/auth/registro` | Pública | JSON: `nombre`, `correo`, `contrasena` | Registra un cliente con rol `USER`. | `201`, `400`, `409` |
| `POST` | `/api/auth/login` | Pública | JSON: `correo`, `contrasena` | Autentica y devuelve un JWT. | `200`, `401` |
| `GET` | `/api/libros` | Pública | — | Lista los libros activos del catálogo. | `200` |
| `GET` | `/api/libros/{id}` | Pública | `id` en la ruta | Consulta el detalle de un libro activo. | `200`, `404` |

### 12.2. API REST autenticada (JWT)

Todas requieren el encabezado `Authorization: Bearer <token>`.

| Método | Ruta | Body / parámetros | Descripción | Respuestas |
|---|---|---|---|---|
| `GET` | `/api/carrito` | — | Consulta el carrito del cliente autenticado. | `200`, `401` |
| `POST` | `/api/carrito/items` | JSON: `formatoLibroId`, `cantidad` | Agrega una presentación al carrito. | `200`, `400`, `401` |
| `PUT` | `/api/carrito/items/{formatoLibroId}` | JSON: `cantidad` | Modifica la cantidad de un ítem. | `200`, `400`, `401` |
| `DELETE` | `/api/carrito/items/{formatoLibroId}` | — | Elimina un ítem del carrito. | `200`, `401` |
| `POST` | `/api/checkout` | — | Confirma la compra del carrito y genera la orden. | `201`, `400`, `401` |
| `GET` | `/api/ordenes` | — | Consulta el historial de órdenes del cliente autenticado. | `200`, `401` |

### 12.3. Vistas públicas y sesión de cliente

| Método | Ruta | Autenticación | Body / parámetros | Descripción | Respuestas |
|---|---|---|---|---|---|
| `GET` | `/` | Pública | — | Página principal de la librería. | `200` |
| `POST` | `/registro` | Pública | Form: `nombre`, `correo`, `contrasena`, `_csrf` | Registra un cliente y redirige al login. | `302` |
| `POST` | `/iniciar-sesion` | Pública | Form: `correo`, `contrasena`, `_csrf` | Autentica la sesión; redirige al panel si es ADMIN o al inicio si es USER. | `302` |
| `POST` | `/logout` | Autenticado | Form: `_csrf` | Cierra la sesión HTTP. | `302` |

### 12.4. Códigos de respuesta

| Código | Significado |
|---|---|
| `200` | Operación exitosa. |
| `201` | Recurso creado correctamente. |
| `302` | Redirección (POST/Redirect/GET o falta de sesión). |
| `400` | Datos inválidos o regla de negocio incumplida. |
| `401` | Falta el JWT o es inválido. |
| `403` | Autenticado sin el rol requerido, o CSRF inválido. |
| `404` | Recurso no encontrado. |
| `409` | Conflicto, por ejemplo un correo ya registrado. |

### 12.5. Ejemplo rápido de uso de la API

```bash
# 1. Obtener el token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"correo":"cliente@ejemplo.com","contrasena":"Password123!"}'

# 2. Usar el token en un endpoint protegido
curl http://localhost:8080/api/carrito \
  -H "Authorization: Bearer <token>"
```

### 12.6. Colección de Postman

La carpeta `Postman/` contiene un archivo:

| Archivo | Contenido |
|---|---|
| `Raudskinna_API_Clientes.postman_collection.json` | 32 peticiones con pruebas automáticas para los endpoints de cliente y de API. **Basta con importar este archivo.** |

La colección no incluye los endpoints del panel administrativo (`/admin/**`) ni las páginas que solo muestran formularios. Se centra en los endpoints de cliente y de API: catálogo, autenticación JWT, carrito, checkout, órdenes y consulta de usuarios. Esas vistas siguen disponibles en la aplicación.

Cada petición lleva sus valores escritos directamente (URL, IDs, correos y contraseñas), así que no hace falta environment ni configurar variables. Las únicas variables son `jwtToken` y `csrfToken`, que Postman rellena solo al iniciar sesión.

**Importación:**

1. En Postman, usa **Import** y selecciona la colección.
2. La colección apunta a `http://localhost:8080`. Si usas otro puerto o host, edita la URL de cada petición.
3. En la petición **Iniciar sesión como administrador**, reemplaza `CAMBIAR_ESTA_CLAVE` por la contraseña del administrador (`RAUDSKINNA_ADMIN_PASSWORD`).
4. Mantén habilitado el almacenamiento de cookies de Postman.

**Carpetas de la colección:**

| Carpeta | Endpoints |
|---|---|
| `00 - Preparación` | Comprobación de la aplicación y token CSRF público. |
| `01 - Catálogo público (API)` | `/api/libros` y errores `400`/`404`. |
| `02 - Autenticación API (JWT)` | Registro, login y captura de `jwtToken`. |
| `03 - Carrito (JWT)` | `GET`, `POST`, `PUT` y `DELETE` del carrito. |
| `04 - Checkout y órdenes (JWT)` | Confirmación de compra e historial. |
| `05 - Vistas públicas y sesión` | Formularios HTML, login, autorización por rol y token CSRF. |
| `06 - Usuarios (JSON, ADMIN)` | `/usuarios` y `/usuarios/{id}`. |

**Orden de ejecución:** ejecuta las carpetas de `00` a `06` o usa el Collection Runner. Las peticiones que necesitan CSRF obtienen el token automáticamente mediante las peticiones `Obtener token CSRF`.

**Notas:**

- Las peticiones de login guardan el JWT en `jwtToken` y los formularios guardan el token CSRF en `csrfToken`.
- Las peticiones que esperan un `302` tienen deshabilitado el seguimiento automático de redirecciones para poder comprobar la cabecera `Location`.
- Las pruebas de registro crean clientes nuevos con un correo único basado en la variable dinámica `{{$timestamp}}` de Postman, para que la colección se pueda reejecutar sin errores `409`.
- La colección no prueba el logout ni el panel administrativo; para eso usa las peticiones de administración desde la aplicación o agrégalas manualmente en Postman.


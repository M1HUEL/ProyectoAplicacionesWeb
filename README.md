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

**Registro del avance — 29/oct/2026:** módulos administrativos implementados y revisados, indicadores reales, pruebas automatizadas satisfactorias, diagrama EER generado y mejoras de navegación y responsividad publicadas en `feat-administracion`.
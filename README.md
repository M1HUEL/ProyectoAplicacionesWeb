# Librería Raudskinna 📚

Proyecto académico de **Aplicaciones Web**: plataforma de comercio electrónico de libros desarrollada con Spring Boot, Thymeleaf y MySQL.

> **Estado al 15 de noviembre de 2026 — Avance 4.1:** API REST de clientes integrada en la rama `main`, con autenticación JWT, registro de usuarios, consulta del catálogo, carrito de compras persistente, checkout transaccional e historial de órdenes. La aplicación conserva los módulos administrativos desarrollados en avances anteriores. Se ejecutaron satisfactoriamente 91 pruebas automatizadas, sin fallos ni errores.

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

## 3. Preparación y ejecución local

### 3.1. Requisitos

Para ejecutar Librería Raudskinna se requiere:

- Java JDK 21.
- MySQL 8.4 o una versión compatible.
- Maven Wrapper (incluido en el repositorio).
- IntelliJ IDEA, NetBeans o una terminal compatible.
- Postman para probar los endpoints REST.

### 3.2. Preparación de MySQL

Iniciar el servicio de MySQL y crear la base de datos:

```sql
CREATE DATABASE IF NOT EXISTS libreria_raudskinna;
```

Configurar un usuario MySQL con permisos sobre esta base de datos.

La aplicación utiliza Spring Data JPA y Hibernate para actualizar el esquema durante el desarrollo mediante `spring.jpa.hibernate.ddl-auto=update`.

### 3.3. Variables de entorno

| Variable | Descripción | Requerimiento |
|---|---|---|
| `DB_USERNAME` | Usuario de conexión a MySQL | Obligatoria |
| `DB_PASSWORD` | Contraseña de conexión a MySQL | Obligatoria para la configuración habitual |
| `RAUDSKINNA_ADMIN_PASSWORD` | Contraseña de la cuenta administrativa inicial | Necesaria si el administrador todavía no existe |
| `JWT_SECRET` | Clave Base64 utilizada para firmar y validar JWT | Opcional en la configuración actual de `main`, que dispone de un valor predeterminado |
| `ARCHIVOS_PROVEEDOR` | Proveedor de archivos: `local` o `cloudinary` | Opcional; el valor predeterminado es `local` |
| `CLOUDINARY_CLOUD_NAME` | Cloud name de la cuenta de Cloudinary | Obligatoria si `ARCHIVOS_PROVEEDOR=cloudinary` |
| `CLOUDINARY_API_KEY` | API Key de Cloudinary | Obligatoria si `ARCHIVOS_PROVEEDOR=cloudinary` |
| `CLOUDINARY_API_SECRET` | API Secret de Cloudinary | Obligatoria si `ARCHIVOS_PROVEEDOR=cloudinary` |
| `CLOUDINARY_SECURE_DISTRIBUTION` | Dominio de entrega propio, sin `https://` ni barra final | Opcional; por defecto se usa `res.cloudinary.com/<cloud_name>` |

La contraseña administrativa se utiliza para crear automáticamente `admin@raudskinna.com` mediante `DataInitializer`.

Si la cuenta ya existe, cambiar `RAUDSKINNA_ADMIN_PASSWORD` no actualizará su contraseña almacenada.

### 3.4. Ejecución desde Konsole o Bash

Abrir una terminal en la raíz del proyecto y configurar las variables:

```bash
read -r -p "Usuario MySQL: " DB_USERNAME
export DB_USERNAME

read -r -s -p "Contraseña MySQL: " DB_PASSWORD
echo
export DB_PASSWORD

read -r -s -p "Contraseña inicial del administrador: " RAUDSKINNA_ADMIN_PASSWORD
echo
export RAUDSKINNA_ADMIN_PASSWORD
```

Si se desea utilizar una clave JWT propia:

```bash
read -r -s -p "JWT_SECRET (Base64): " JWT_SECRET
echo
export JWT_SECRET
```

La clave JWT debe representar al menos 256 bits (32 bytes) después de decodificarse desde Base64.

Para ejecutar la aplicación:

```bash
bash ./mvnw spring-boot:run
```

Para ejecutar las pruebas automatizadas:

```bash
bash ./mvnw clean test
```

Las variables exportadas estarán disponibles para los procesos ejecutados desde esa terminal. No se conservarán automáticamente en otras sesiones.

### 3.5. Ejecución desde Windows (PowerShell, CMD y Git Bash)

Librería Raudskinna también puede ejecutarse en Windows 10/11 utilizando PowerShell, Símbolo del sistema (CMD) o Git Bash.

**Requisitos previos:**
- JDK 21 instalado y configurado.
- MySQL en ejecución.
- Base de datos `libreria_raudskinna` creada.
- Terminal abierta en el directorio raíz del proyecto, donde se encuentra `mvnw.cmd`.

Las variables de entorno necesarias son `DB_USERNAME`, `DB_PASSWORD` y, para la creación inicial del administrador, `RAUDSKINNA_ADMIN_PASSWORD`.

La variable `JWT_SECRET` es opcional en la versión actual de `main`, que dispone de un valor predeterminado para desarrollo. Si se utiliza una clave propia, debe estar codificada en Base64 y representar al menos 256 bits.

#### A. PowerShell (recomendado en Windows)

Configurar las variables de entorno:

```powershell
$env:DB_USERNAME = Read-Host "Usuario de MySQL"

$claveDB = Read-Host "Contraseña de MySQL" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $claveDB).Password

$claveAdmin = Read-Host "Contraseña inicial del administrador" -AsSecureString
$env:RAUDSKINNA_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new("", $claveAdmin).Password
```

El uso de `-AsSecureString` evita mostrar las contraseñas mientras se capturan.

Opcionalmente, configurar una clave JWT propia:

```powershell
$claveJWT = Read-Host "JWT_SECRET (Base64)" -AsSecureString
$env:JWT_SECRET = [System.Net.NetworkCredential]::new("", $claveJWT).Password
```

Para iniciar Spring Boot:

```powershell
.\mvnw.cmd spring-boot:run
```

Para ejecutar las pruebas automatizadas:

```powershell
.\mvnw.cmd clean test
```

#### B. Símbolo del sistema (CMD)

En CMD, las variables se configuran mediante `set`:

```bat
set "DB_USERNAME=usuario_mysql"
set "DB_PASSWORD=contrasena_mysql"
set "RAUDSKINNA_ADMIN_PASSWORD=contrasena_admin"
```

Sustituir los valores de ejemplo por las credenciales correspondientes al entorno local.

Opcionalmente, para configurar una clave JWT propia:

```bat
set "JWT_SECRET=clave_base64"
```

Para iniciar la aplicación:

```bat
mvnw.cmd spring-boot:run
```

Para ejecutar las pruebas:

```bat
mvnw.cmd clean test
```

**Advertencia:** escribir contraseñas directamente en comandos CMD puede exponerlas en la pantalla o en registros de la sesión. Para introducir credenciales de manera más segura se recomienda PowerShell o Git Bash.

#### C. Git Bash en Windows

Si se tiene instalado Git for Windows, también puede utilizarse Git Bash.

Los comandos `read`, `export` y las instrucciones Bash de la sección 3.4 pueden utilizarse en este entorno.

Para ejecutar el proyecto:

```bash
bash ./mvnw spring-boot:run
```

Para ejecutar las pruebas:

```bash
bash ./mvnw clean test
```

#### D. Consideraciones sobre las variables de entorno

- Las variables configuradas mediante `set`, `$env:` o `export` están disponibles para los procesos iniciados desde la terminal correspondiente.
- Normalmente dejan de estar disponibles al cerrar esa sesión.
- No se transfieren automáticamente a IntelliJ IDEA o NetBeans si estos ya estaban ejecutándose.
- Si se utilizan configuraciones de ejecución propias del IDE, deben establecerse allí las variables necesarias.
- No deben subirse al repositorio contraseñas reales, secretos JWT ni archivos personales que contengan credenciales.

Una vez iniciada la aplicación, normalmente estará disponible en:

`http://localhost:8080`

El acceso administrativo se realiza desde:

`http://localhost:8080/admin/login`

### 3.6. Ejecución desde IntelliJ IDEA

1. Abrir el proyecto Maven en IntelliJ IDEA.
2. Configurar el JDK 21.
3. Abrir **Run → Edit Configurations**.
4. Seleccionar la configuración de ejecución de Spring Boot.
5. En **Environment variables**, definir `DB_USERNAME`, `DB_PASSWORD` y `RAUDSKINNA_ADMIN_PASSWORD`. Se puede añadir `JWT_SECRET` si se desea utilizar una clave propia.
6. Guardar la configuración.
7. Ejecutar la clase principal `ProyectoAplicacionesWebApplication`.

Las variables definidas en IntelliJ corresponden a esa configuración de ejecución y no se exportan automáticamente a otras terminales.

### 3.7. Ejecución desde NetBeans

1. Abrir el proyecto Maven en Apache NetBeans.
2. Verificar que se utilice el JDK 21.
3. Asegurarse de que MySQL esté funcionando y la base de datos exista.
4. Configurar las variables de entorno para el proceso Java que ejecutará la aplicación.
5. Ejecutar `ProyectoAplicacionesWebApplication`.

Como alternativa reproducible, abrir una terminal en el directorio del proyecto y utilizar los comandos de la sección 3.4. Esto evita depender de diferencias entre versiones de NetBeans y sus configuraciones de ejecución.

### 3.8. Acceso inicial

Una vez iniciada la aplicación, el servidor estará disponible normalmente en:

`http://localhost:8080`

Para acceder al panel administrativo:

`http://localhost:8080/admin/login`

Credenciales de prueba:

- Correo: `admin@raudskinna.com`
- Contraseña: valor configurado mediante `RAUDSKINNA_ADMIN_PASSWORD` durante la creación inicial de la cuenta.

### 3.9. Pruebas REST con Postman

Importar la colección:

`Postman/Raudskinna_API_Clientes.postman_collection.json`

Configurar `baseUrl` con el valor:

`http://localhost:8080`

Ejecutar el inicio de sesión para obtener un JWT y utilizarlo en las solicitudes protegidas mediante el encabezado:

`Authorization: Bearer <token>`

La colección incluye solicitudes organizadas por módulo para comprobar autenticación, catálogo, carrito, checkout e historial de órdenes.

**Nota:** esta configuración está destinada al entorno académico de desarrollo. No debe considerarse una configuración preparada para producción.

### 3.10. Almacenamiento de archivos: local o Cloudinary

Las portadas y los PDF de descarga digital se gestionan mediante la interfaz `AlmacenamientoArchivos`, con dos implementaciones seleccionadas por `ARCHIVOS_PROVEEDOR`:

- `local` (predeterminado): los archivos se guardan en `app.uploads.dir` y se publican en `/uploads/**`. El PDF de demostración se lee del classpath cuando no existe en disco.
- `cloudinary`: los archivos se guardan con el SDK oficial de Cloudinary (`cloudinary-http5`). Las imágenes se suben como `image` y los PDF como `raw`; las URL de entrega se generan con el mismo SDK (`https://res.cloudinary.com/<cloud_name>/...` o el dominio configurado en `CLOUDINARY_SECURE_DISTRIBUTION`).

La base de datos guarda solo la clave lógica (por ejemplo, `portadas/uuid.png`). `ResolvedorUrlsArchivos` genera la URL correspondiente al proveedor activo, por lo que las referencias históricas `/uploads/...` se resuelven con el proveedor configurado sin migrar la tabla `libros`. Las URL externas y las imágenes estáticas `/img/portadas/...` se respetan tal cual.

Para activar Cloudinary en PowerShell:

```powershell
$env:ARCHIVOS_PROVEEDOR = "cloudinary"
$env:CLOUDINARY_CLOUD_NAME = "nombre-de-la-nube"
$env:CLOUDINARY_API_KEY = "api-key"
$env:CLOUDINARY_API_SECRET = "api-secret"

# Solo si se usa un dominio de entrega propio:
$env:CLOUDINARY_SECURE_DISTRIBUTION = "cdn.ejemplo.com"
```

Las credenciales se obtienen en *Dashboard → API Keys* de Cloudinary. El `API Secret` no se vuelve a mostrar después de crearlo. Para que las nuevas credenciales surtan efecto hay que reiniciar la aplicación, porque el cliente se construye al arrancar.

Antes de cambiar de proveedor, copiar los objetos existentes al otro almacenamiento manteniendo las mismas claves (`public_id` sin la extensión). Las portadas requieren que la entrega pública esté habilitada; el PDF se sigue entregando por el backend para conservar la autorización por orden.

## 4. Seguridad y autenticación

La aplicación utiliza Spring Security para:

- Validar credenciales contra los usuarios registrados.
- Restringir `/admin` y `/admin/**` a usuarios con `ROLE_ADMIN`.
- Permitir el acceso público a `/admin/login`.
- Almacenar contraseñas utilizando BCrypt.
- Mantener CSRF habilitado.
- Invalidar la sesión HTTP al cerrar sesión.

El acceso administrativo y el cierre de sesión fueron comprobados desde el navegador. También se verificó la inclusión del campo `_csrf` en formularios POST administrativos.

### 4.1. Acceso administrativo para pruebas

El proyecto incluye la clase `DataInitializer`, encargada de crear automáticamente una cuenta administrativa inicial cuando arranca Spring Boot.

**No es necesario insertar manualmente un administrador desde MySQL.**

#### 4.1.1. Configuración
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

La estructura relacional del proyecto incluye las tablas correspondientes a usuarios, catálogo, inventario, carritos de compra y órdenes.

Entre las tablas principales se encuentran:

`usuarios`, `ordenes_compra`, `detalles_orden`, `libros`, `formatos_libro`, `items_stock`, `categorias`, `editoriales`, `autores` y `libros_autores`.

El Avance 4.1 incorpora además persistencia para los carritos de compra, sus artículos y los movimientos de inventario asociados a las operaciones comerciales.

Para consultar la estructura definitiva, revisar las entidades JPA y las relaciones existentes en MySQL.

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

**Última ejecución completa verificada:** 15 de noviembre de 2026.

Se ejecutó la batería de pruebas automatizadas sobre la versión integrada del proyecto, correspondiente al Avance 4.1.

**Comando utilizado:**

```bash
bash ./mvnw clean test
```

**Resultado obtenido:**

```text
Tests run: 91, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Las 91 pruebas incluyen comprobaciones de servicios, repositorios, controladores y carga del contexto de Spring Boot.

Para ejecutar correctamente las pruebas es necesario configurar las credenciales de MySQL mediante las variables de entorno correspondientes y disponer de una base de datos accesible.

**Pruebas funcionales realizadas:**

- Registro de clientes mediante la API REST.
- Autenticación JWT y acceso a endpoints protegidos.
- Consulta pública del catálogo de libros.
- Operaciones de consulta, registro, modificación y eliminación de artículos del carrito.
- Confirmación exitosa de una compra.
- Descuento de inventario y registro del movimiento correspondiente.
- Rechazo del checkout por existencias insuficientes, verificando que no se generan órdenes incompletas y que el carrito conserva sus artículos.

Las pruebas automatizadas y funcionales aportan evidencia del funcionamiento de los módulos evaluados, aunque no sustituyen una revisión integral de todos los escenarios posibles.

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

**Rama principal del proyecto:** `main`

**Último commit confirmado en `origin/main` (15/nov/2026):**

```text
96ccf2a Merge pull request #2 from M1HUEL/feat-api-clientes
```

**Estado del Avance 4.1:** API REST de clientes integrada, incluyendo autenticación JWT, registro de usuarios, carrito de compras, checkout transaccional e historial de órdenes.

Las actualizaciones de documentación y de la colección Postman se desarrollan en una rama independiente para su posterior revisión e integración mediante Pull Request.

Para consultar el avance:

```bash
git branch --show-current
git status
git log -5 --oneline
```

Se recomienda realizar nuevas modificaciones en ramas propias, revisarlas en equipo y no fusionarlas directamente a `main` sin coordinación.

## 10. Verificación funcional del Avance 4.1

El Avance 4.1 incorpora una API REST para las operaciones principales del cliente, protegida mediante autenticación JWT cuando corresponde.

### 10.1. Funcionalidades disponibles

Consulta pública del catálogo de libros.

Registro de clientes e inicio de sesión mediante JWT.

Consulta y administración del carrito de compras.

Confirmación de compras con validación de existencias.

Consulta del historial de órdenes del usuario autenticado.

Manejo de errores HTTP mediante respuestas JSON estructuradas.

### 10.2. Pruebas realizadas

Se ejecutaron satisfactoriamente 91 pruebas automatizadas mediante Maven.

Adicionalmente, se realizaron comprobaciones funcionales con Postman sobre autenticación, historial de órdenes y checkout, incluyendo escenarios de carrito vacío y existencias insuficientes.

En la prueba de inventario insuficiente se verificó mediante consultas SQL que la solicitud rechazada no generó una orden adicional, no modificó las existencias y conservó los artículos del carrito.

### 10.3. Colección Postman

La colección se encuentra en:

Postman/Raudskinna_API_Clientes.postman_collection.json

Contiene solicitudes organizadas por funcionalidad, validaciones de respuestas HTTP y documentación de las precondiciones necesarias para ejecutar determinados escenarios.

La aplicación debe estar en ejecución y contar con una base de datos preparada para realizar las pruebas correspondientes.

## 11. Scripts SQL para insertar registros (MySQL)

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

Esta sección documenta los endpoints REST del lado del cliente.

- **API REST (`/api/**`):** autenticación con **JWT Bearer**. CSRF deshabilitado.

Las rutas administrativas (`/admin/**`), las vistas y las páginas que solo muestran formularios se omiten de esta documentación.

### 12.1. API REST pública

| Método | Ruta | Autenticación | Body / parámetros | Descripción | Respuestas |
|---|---|---|---|---|---|
| `POST` | `/api/auth/registro` | Pública | JSON: `nombre`, `correo`, `contrasena` | Registra un cliente con rol `USER`. | `201`, `400`, `409` |
| `POST` | `/api/auth/login` | Pública | JSON: `correo`, `contrasena` | Autentica y devuelve un JWT. | `200`, `401` |
| `GET` | `/api/libros` | Pública | Opcionales: `q` (título o autor), `categoria`, `page` (default `0`), `size` (default `12`, máximo `50`) | Devuelve una página del catálogo con `precioDesde` y `disponible` por libro. | `200`, `400` |
| `GET` | `/api/libros/{id}` | Pública | `id` en la ruta | Consulta el detalle de un libro activo, incluyendo `formatos` con `id`, `precio` y `disponible`. | `200`, `404` |
| `GET` | `/api/libros/{id}/formatos` | Pública | `id` en la ruta | Consulta únicamente las presentaciones comerciales de un libro activo. | `200`, `404` |

La respuesta paginada contiene `contenido`, `pagina`, `tamano`, `totalElementos`, `totalPaginas`, `primera` y `ultima`. Cada elemento de `contenido` incluye el libro, su `precioDesde` y su disponibilidad. Los filtros se aplican en la base de datos, por lo que la respuesta no depende del tamaño total del catálogo.

### 12.2. API REST autenticada (JWT)

Todas requieren el encabezado `Authorization: Bearer <token>`.

| Método | Ruta | Body / parámetros | Descripción | Respuestas |
|---|---|---|---|---|
| `GET` | `/api/carrito` | — | Consulta el carrito del cliente autenticado. | `200`, `401` |
| `POST` | `/api/carrito/items` | JSON: `formatoLibroId`, `cantidad` | Agrega una presentación al carrito. | `200`, `400`, `401`, `404` |
| `PUT` | `/api/carrito/items/{formatoLibroId}` | JSON: `cantidad` | Modifica la cantidad de un ítem. | `200`, `400`, `401` |
| `DELETE` | `/api/carrito/items/{formatoLibroId}` | — | Elimina un ítem del carrito. | `200`, `401` |
| `POST` | `/api/checkout` | — | Confirma la compra del carrito y genera la orden. | `201`, `400`, `401` |
| `GET` | `/api/ordenes` | — | Consulta el historial de órdenes del cliente autenticado. | `200`, `401` |
| `GET` | `/api/direcciones` | — | Lista las direcciones de envío del cliente autenticado. | `200`, `401` |
| `GET` | `/api/direcciones/{id}` | `id` en la ruta | Consulta una dirección propia. | `200`, `400`, `401` |
| `POST` | `/api/direcciones` | JSON: `nombreDestinatario`, `calle`, `numeroExterior`, `colonia`, `codigoPostal`, `ciudad`, `estado`, `pais`; opcionales: `numeroInterior`, `referencias` | Registra una dirección y devuelve `Location`. | `201`, `400`, `401` |
| `PUT` | `/api/direcciones/{id}` | Los mismos campos que `POST` | Actualiza una dirección propia. | `200`, `400`, `401` |
| `DELETE` | `/api/direcciones/{id}` | `id` en la ruta | Elimina una dirección propia. | `204`, `400`, `401` |

Todas las operaciones de direcciones reutilizan `DireccionService`, por lo que el propietario se toma del JWT y nunca se acepta un `usuarioId` en la petición. Las respuestas usan `DireccionResponseDTO` y no exponen la entidad JPA ni la relación con el usuario.

El campo `id` de cada elemento de `formatos` es el `formatoLibroId` que debe enviarse a `POST /api/carrito/items`. La disponibilidad informada es informativa; el inventario se valida de nuevo al confirmar la compra.

### 12.3. Códigos de respuesta

| Código | Significado |
|---|---|
| `200` | Operación exitosa. |
| `201` | Recurso creado correctamente. |
| `204` | Operación exitosa sin contenido de respuesta, por ejemplo al eliminar una dirección. |
| `302` | Redirección (POST/Redirect/GET o falta de sesión). |
| `400` | Datos inválidos o regla de negocio incumplida. |
| `401` | Falta el JWT o es inválido. |
| `403` | Autenticado sin el rol requerido, o CSRF inválido. |
| `404` | Recurso no encontrado. |
| `409` | Conflicto, por ejemplo un correo ya registrado. |
| `500` | Error inesperado; responde con el mismo formato JSON de error de la API. |


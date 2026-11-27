# Librería Raudskinna 📚

Proyecto académico de **Aplicaciones Web**: plataforma de comercio electrónico de libros desarrollada con Spring Boot, Thymeleaf y MySQL. Incluye las vistas de cliente, el panel administrativo y la API REST documentada en `PROYECTO.md`.

## Requisitos previos para la instalación

- **JDK 21** instalado (`java -version` debe mostrar la versión 21).
- **MySQL 8.4** o una versión compatible, con el servicio en ejecución.
- **Maven Wrapper** incluido en el repositorio (`mvnw` / `mvnw.cmd`); no es necesario instalar Maven.
- Terminal compatible: PowerShell o Símbolo del sistema en Windows; Bash en Linux, macOS o Git Bash.
- IDE opcional: IntelliJ IDEA o Apache NetBeans.
- Postman opcional para probar los endpoints REST.

## Pasos para configurar el entorno de desarrollo

1. Crear la base de datos desde MySQL:

   ```sql
   CREATE DATABASE IF NOT EXISTS libreria_raudskinna;
   ```

2. Configurar las variables de entorno en la terminal desde la que se ejecutará la aplicación:

   | Variable | Descripción | Requerimiento |
   |---|---|---|
   | `DB_USERNAME` | Usuario de conexión a MySQL | Obligatoria |
   | `DB_PASSWORD` | Contraseña de conexión a MySQL | Obligatoria |
   | `RAUDSKINNA_ADMIN_PASSWORD` | Contraseña inicial de `admin@raudskinna.com` | Obligatoria la primera vez |
   | `JWT_SECRET` | Clave Base64 para firmar los JWT | Obligatoria; al menos 32 bytes después de decodificar |
   | `ARCHIVOS_PROVEEDOR` | `local` o `cloudinary` | Opcional; por defecto `local` |
   | `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | Credenciales de Cloudinary | Obligatorias solo si `ARCHIVOS_PROVEEDOR=cloudinary` |
   | `CLOUDINARY_SECURE_DISTRIBUTION` | Dominio de entrega propio, sin `https://` ni barra final | Opcional |

   Hibernate usa `spring.jpa.hibernate.ddl-auto=update`, por lo que crea y actualiza las tablas automáticamente. Los scripts SQL opcionales para datos de prueba están en `PROYECTO.md`.

   **PowerShell (Windows):**

   ```powershell
   $env:DB_USERNAME = Read-Host "Usuario de MySQL"

   $claveDB = Read-Host "Contraseña de MySQL" -AsSecureString
   $env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $claveDB).Password

   $claveAdmin = Read-Host "Contraseña inicial del administrador" -AsSecureString
   $env:RAUDSKINNA_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new("", $claveAdmin).Password

   $claveJWT = Read-Host "JWT_SECRET (Base64)" -AsSecureString
   $env:JWT_SECRET = [System.Net.NetworkCredential]::new("", $claveJWT).Password
   ```

   **Bash (Linux, macOS o Git Bash):**

   ```bash
   read -r -p "Usuario MySQL: " DB_USERNAME; export DB_USERNAME
   read -r -s -p "Contraseña MySQL: " DB_PASSWORD; echo; export DB_PASSWORD
   read -r -s -p "Contraseña inicial del administrador: " RAUDSKINNA_ADMIN_PASSWORD; echo; export RAUDSKINNA_ADMIN_PASSWORD
   read -r -s -p "JWT_SECRET (Base64): " JWT_SECRET; echo; export JWT_SECRET
   ```

   En CMD se pueden definir con `set "VARIABLE=valor"`, aunque PowerShell o Git Bash evitan mostrar las contraseñas en pantalla. Las variables solo están disponibles en la terminal donde se definieron; en un IDE deben configurarse en la configuración de ejecución. No subir credenciales ni secretos reales al repositorio.

3. Opcional: para cargar el catálogo de demostración, activar el perfil `demo` antes de iniciar:

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "demo"
   ```

## Instrucciones para ejecutar el backend y el frontend

No existe un proyecto frontend separado: las pantallas son plantillas **Thymeleaf** servidas por el mismo backend Spring Boot. No se requiere Node.js ni `npm install`.

Desde la raíz del repositorio:

```powershell
# Windows (PowerShell)
.\mvnw.cmd spring-boot:run
```

```bash
# Linux, macOS o Git Bash
bash ./mvnw spring-boot:run
```

En CMD: `mvnw.cmd spring-boot:run`. También se puede ejecutar la clase principal `ProyectoAplicacionesWebApplication` desde el IDE con las variables del paso anterior.

Una vez iniciado:

- Tienda: `http://localhost:8080`
- Panel administrativo: `http://localhost:8080/admin/login`
- API REST: `http://localhost:8080/api/**`

Para ejecutar las pruebas automatizadas:

```powershell
.\mvnw.cmd clean test
```

```bash
bash ./mvnw clean test
```

## Credenciales de prueba (cuando sean necesarias)

- **MySQL:** las credenciales son las que cada integrante define en `DB_USERNAME` y `DB_PASSWORD`; no hay usuario ni contraseña fijos en el repositorio.
- **Administrador:** correo `admin@raudskinna.com`; contraseña igual al valor de `RAUDSKINNA_ADMIN_PASSWORD`. `DataInitializer` crea la cuenta si todavía no existe. Cambiar la variable después no actualiza una contraseña ya guardada.
- **Clientes:** registrar un usuario desde el formulario de registro o desde `POST /api/auth/registro`. Para los endpoints protegidos, obtener el JWT con `POST /api/auth/login` y enviarlo como `Authorization: Bearer <token>`.
- **Postman:** importar `Postman/Raudskinna_API_Clientes.postman_collection.json` y el entorno `Postman/Raudskinna_local.postman_environment.json`, que define `baseUrl=http://localhost:8080`.

## Tecnologías utilizadas

- Java 21 y Maven Wrapper.
- Spring Boot 4.1.2: Spring MVC, Spring Security, OAuth2 Resource Server, Data JPA, Validation.
- Hibernate y MySQL 8.4.
- Thymeleaf con `thymeleaf-extras-springsecurity6` para las vistas.
- Lombok, JUnit 5, Mockito y H2 para las pruebas.
- Postman para pruebas HTTP.
- Cloudinary SDK 2.6.0 para el almacenamiento opcional de archivos.

## Estructura general del proyecto

```text
.
├── pom.xml, mvnw, mvnw.cmd          dependencias y Maven Wrapper
├── Postman/                         colección de la API y entorno local
└── src/
    ├── main/
    │   ├── java/com/itson/ProyectoAplicacionesWeb/
    │   │   ├── config/              seguridad e inicializadores de datos
    │   │   ├── controller/          vistas web, /admin/** y API /api/**
    │   │   ├── dto/                 contratos de entrada y salida de la API
    │   │   ├── exception/           manejo centralizado de errores
    │   │   ├── model/               entidades JPA
    │   │   ├── repository/          consultas y persistencia
    │   │   ├── service/             reglas de negocio
    │   │   └── storage/             almacenamiento local y Cloudinary
    │   └── resources/
    │       ├── templates/           vistas Thymeleaf (admin, usuarios, fragments)
    │       ├── static/              estilos, scripts e imágenes
    │       ├── descargas/           PDF de demostración
    │       └── application.properties
    └── test/java/...                pruebas unitarias, de repositorio e integración
```

La arquitectura por capas, los endpoints, el modelo de datos, las funcionalidades administrativas y las evidencias de pruebas se documentan en [`PROYECTO.md`](PROYECTO.md).

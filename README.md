# Sistema de Pedidos — Servlet + JSP + Maven

Proyecto académico para demostrar una aplicación empresarial básica con Jakarta EE.

## Tecnologías

- Java 21
- Maven
- Jakarta EE 11
- Jakarta Servlet
- JSP / Jakarta Server Pages
- Jakarta Tags (JSTL)
- Jakarta Enterprise Beans (EJB)
- Jakarta Persistence (JPA)
- Hibernate ORM provisto por WildFly
- H2
- WildFly 41
- WildFly Maven Plugin

## Arquitectura

```text
Navegador
    |
    | HTTP
    v
PedidoServlet
    |
    | @EJB
    v
PedidoService
    |
    | JPA / EntityManager
    v
ExampleDS
    |
    v
H2
```

La JSP actúa como vista:

```text
PedidoServlet
    |
    | request.setAttribute(...)
    v
/WEB-INF/views/pedidos.jsp
```

## Requisitos

- JDK 21
- Maven 3.9 o superior
- Acceso a Internet la primera vez para descargar dependencias
- Puerto 8081 disponible

Verificar:

```bash
java -version
mvn -version
```

## Ejecutar

Desde la raíz del proyecto:

```bash
mvn clean wildfly:run
```

El plugin provisionará WildFly dentro de `target/server`.

Abrir:

```text
http://localhost:8081/sistema-pedidos/
```

o directamente:

```text
http://localhost:8081/sistema-pedidos/pedidos
```

Para detener WildFly: `Ctrl + C`.

## Flujo de la aplicación

1. `PedidoServlet#doGet()` consulta productos y pedidos.
2. El Servlet coloca los datos en el `request`.
3. Se ejecuta un `forward` hacia `pedidos.jsp`.
4. La JSP presenta los datos utilizando EL y Jakarta Tags.
5. El formulario realiza `POST /pedidos`.
6. `PedidoServlet#doPost()` recibe los parámetros.
7. `PedidoService` valida las reglas de negocio.
8. JPA persiste el pedido y actualiza el stock.
9. Se utiliza el patrón PRG: Post / Redirect / Get.

## Base de datos

Se utiliza el DataSource de ejemplo de WildFly:

```text
java:jboss/datasources/ExampleDS
```

con H2 en memoria.

`persistence.xml` usa `drop-and-create`, por lo que tablas y datos se reinician al desplegar una nueva instancia.

## Productos iniciales

- Laptop — S/ 2500.00 — Stock 5
- Monitor — S/ 850.00 — Stock 8
- Teclado — S/ 120.00 — Stock 15

## Archivos principales

```text
src/main/java/pe/edu/isil/pedidos/
├── domain/
│   ├── Pedido.java
│   └── Producto.java
├── service/
│   ├── PedidoException.java
│   └── PedidoService.java
└── web/
    └── PedidoServlet.java

src/main/resources/META-INF/
└── persistence.xml

src/main/webapp/
├── index.jsp
├── assets/css/app.css
└── WEB-INF/views/pedidos.jsp
```

## Nota
- Las JSP se ubican dentro de `WEB-INF/views` para evitar su acceso directo. Se renderizan mediante `forward` desde el Servlet.
- Esta es la versión base Servlet + JSP + Maven. No incluye todavía la tarea de edición y eliminación de pedidos.

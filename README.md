# Sistema de Pedidos — Servlet + JSP + Maven

Proyecto académico desarrollado con Jakarta EE para gestionar pedidos y controlar el stock de productos.

## Funcionalidades

La aplicación permite:

- Registrar pedidos.
- Listar los pedidos registrados.
- Editar el cliente de un pedido.
- Modificar la cantidad solicitada.
- Cambiar el producto de un pedido.
- Eliminar pedidos.
- Actualizar el stock de acuerdo con las operaciones realizadas.
- Validar cantidades y disponibilidad de stock.

## Tecnologías utilizadas

- Java 21
- Jakarta EE
- JSP
- Servlets
- EJB
- JPA
- H2
- Maven
- WildFly

## Flujo de la aplicación

La aplicación mantiene el siguiente flujo:

Navegador → PedidoServlet → PedidoService → JPA / EntityManager → H2

La interfaz se desarrolla con JSP y las operaciones de edición y eliminación se realizan desde JavaScript utilizando `fetch()`.

Para actualizar un pedido se utiliza una petición HTTP `PUT`.

Para eliminar un pedido se utiliza una petición HTTP `DELETE`.

## Manejo de stock

Al modificar un pedido se actualiza el stock según el cambio realizado.

Si cambia solamente el cliente, el stock no se modifica.

Si cambia la cantidad, se considera la diferencia entre la cantidad anterior y la nueva.

Si cambia el producto, se devuelve el stock correspondiente al producto anterior y se descuenta la cantidad del nuevo producto.

Al eliminar un pedido, la cantidad asociada al pedido se devuelve al stock antes de eliminar el registro.


## Requisitos

Para ejecutar el proyecto se requiere:

- Java 21.
- Maven.
- WildFly.
- Un navegador web.

## Pruebas realizadas

Las operaciones de edición y eliminación fueron probadas desde la aplicación en el navegador.

Para la actualización de pedidos se utilizó JavaScript con `fetch()` enviando una petición HTTP `PUT` hacia `PedidoServlet`.

Para la eliminación se utilizó `fetch()` enviando una petición HTTP `DELETE`.

Ambas operaciones fueron verificadas desde DevTools, en la pestaña Network, comprobando que las solicitudes se enviaron realmente mediante los métodos `PUT` y `DELETE` y que las operaciones se realizaron correctamente.

## Ejecución

La aplicación se ejecuta en WildFly mediante Maven.

Una vez desplegado el proyecto, se puede acceder desde:

http://localhost:8081/sistema-pedidos/pedidos
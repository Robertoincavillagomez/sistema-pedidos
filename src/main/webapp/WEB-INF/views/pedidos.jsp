<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>

<!doctype html>
<html lang="es">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Sistema de Pedidos - ISIL</title>

    <c:url var="cssUrl" value="/assets/css/app.css"/>
    <link rel="stylesheet" href="${cssUrl}">
</head>

<body>

<h1>Sistema de Pedidos</h1>

<p class="nota">
    Flujo: Navegador → PedidoServlet → PedidoService (EJB) → JPA → H2
</p>

<c:if test="${not empty error}">
    <div class="error">
        <c:out value="${error}"/>
    </div>
</c:if>

<c:if test="${not empty param.creado}">
    <div class="mensaje">
        Pedido #
        <c:out value="${param.creado}"/>
        registrado correctamente.
    </div>
</c:if>

<h2>Registrar pedido</h2>

<c:url var="pedidosUrl" value="/pedidos"/>

<form method="post"
      action="${pedidosUrl}"
      class="form-grid">

    <label>
        Cliente

        <input
            name="cliente"
            required
            maxlength="120"
            placeholder="Ej. Ana Torres"
            value="${clienteIngresado}">
    </label>

    <label>
        Producto

        <select name="productoId" required>

            <c:forEach var="producto" items="${productos}">

                <option value="${producto.id}">
                    <c:out value="${producto.nombre}"/>
                    - S/
                    <fmt:formatNumber
                            value="${producto.precio}"
                            minFractionDigits="2"
                            maxFractionDigits="2"/>
                    - stock:
                    <c:out value="${producto.stock}"/>
                </option>

            </c:forEach>

        </select>
    </label>

    <label>
        Cantidad

        <input
            name="cantidad"
            type="number"
            min="1"
            value="${empty cantidadIngresada ? 1 : cantidadIngresada}"
            required>
    </label>

    <button type="submit">
        Registrar
    </button>

</form>

<h2>Pedidos registrados</h2>

<table>

    <thead>

    <tr>
        <th>ID</th>
        <th>Cliente</th>
        <th>Producto</th>
        <th>Cantidad</th>
        <th>Total</th>
        <th>Fecha</th>
        <th>Acciones</th>
    </tr>

    </thead>

    <tbody>

    <c:forEach var="pedido" items="${pedidos}">

        <tr data-pedido-id="${pedido.id}">

            <td>
                <c:out value="${pedido.id}"/>
            </td>

            <td>

                <input
                    class="campo-cliente"
                    type="text"
                    value="<c:out value='${pedido.cliente}'/>"
                    maxlength="120"
                    disabled>

            </td>

            <td>

                <select
                    class="campo-producto"
                    disabled>

                    <c:forEach var="producto" items="${productos}">

                        <option
                            value="${producto.id}"
                            <c:if test="${producto.id == pedido.producto.id}">
                                selected
                            </c:if>>

                            <c:out value="${producto.nombre}"/>
                            - S/
                            <fmt:formatNumber
                                    value="${producto.precio}"
                                    minFractionDigits="2"
                                    maxFractionDigits="2"/>

                            - stock:
                            <c:out value="${producto.stock}"/>

                        </option>

                    </c:forEach>

                </select>

            </td>

            <td>

                <input
                    class="campo-cantidad"
                    type="number"
                    min="1"
                    value="${pedido.cantidad}"
                    disabled>

            </td>

            <td>

                <span class="valor-total">

                    S/
                    <fmt:formatNumber
                            value="${pedido.total}"
                            minFractionDigits="2"
                            maxFractionDigits="2"/>

                </span>

            </td>

            <td>

                <c:out value="${pedido.fecha}"/>

            </td>

            <td>

                <button
                    type="button"
                    class="btn-editar"
                    onclick="editarPedido(this)">
                    Editar
                </button>

                <button
                    type="button"
                    class="btn-guardar"
                    onclick="guardarPedido(this)"
                    style="display:none;">
                    Guardar
                </button>

                <button
                    type="button"
                    class="btn-cancelar"
                    onclick="cancelarEdicion(this)"
                    style="display:none;">
                    Cancelar
                </button>

                <button
                    type="button"
                    class="btn-eliminar"
                    onclick="eliminarPedido(this)">
                    Eliminar
                </button>

            </td>

        </tr>

    </c:forEach>

    <c:if test="${empty pedidos}">

        <tr>

            <td colspan="7">
                Aún no hay pedidos.
            </td>

        </tr>

    </c:if>

    </tbody>

</table>

<script>

    function editarPedido(boton) {

        const fila = boton.closest("tr");

        const cliente =
            fila.querySelector(".campo-cliente");

        const producto =
            fila.querySelector(".campo-producto");

        const cantidad =
            fila.querySelector(".campo-cantidad");

        cliente.disabled = false;
        producto.disabled = false;
        cantidad.disabled = false;

        fila.querySelector(".btn-editar")
            .style.display = "none";

        fila.querySelector(".btn-guardar")
            .style.display = "inline-block";

        fila.querySelector(".btn-cancelar")
            .style.display = "inline-block";

        fila.querySelector(".btn-eliminar")
            .style.display = "none";
    }


    function cancelarEdicion(boton) {

        window.location.reload();
    }


    async function guardarPedido(boton) {

        const fila =
            boton.closest("tr");

        const pedidoId =
            fila.dataset.pedidoId;

        const cliente =
            fila.querySelector(".campo-cliente")
                .value
                .trim();

        const productoId =
            fila.querySelector(".campo-producto")
                .value;

        const cantidad =
            fila.querySelector(".campo-cantidad")
                .value;

        if (!cliente) {

            alert("El cliente es obligatorio.");

            return;
        }

        if (!cantidad || Number(cantidad) <= 0) {

            alert(
                "La cantidad debe ser mayor que cero."
            );

            return;
        }

        const parametros =
            new URLSearchParams();

        parametros.append(
            "pedidoId",
            pedidoId
        );

        parametros.append(
            "cliente",
            cliente
        );

        parametros.append(
            "productoId",
            productoId
        );

        parametros.append(
            "cantidad",
            cantidad
        );

        try {

            const response =
                await fetch(
                    "${pedidosUrl}",
                    {
                        method: "PUT",

                        headers: {
                            "Content-Type":
                                "application/x-www-form-urlencoded;charset=UTF-8"
                        },

                        body:
                            parametros.toString()
                    }
                );

            const resultado =
                await response.json();

            if (!response.ok) {

                alert(
                    resultado.mensaje
                    || "No se pudo actualizar el pedido."
                );

                return;
            }

            alert(
                resultado.mensaje
                || "Pedido actualizado correctamente."
            );

            window.location.reload();

        } catch (error) {

            console.error(error);

            alert(
                "No fue posible comunicarse con el servidor."
            );
        }
    }


    async function eliminarPedido(boton) {

        const fila =
            boton.closest("tr");

        const pedidoId =
            fila.dataset.pedidoId;

        const confirmado =
            confirm(
                "¿Está seguro de eliminar el pedido #"
                + pedidoId
                + "?"
            );

        if (!confirmado) {
            return;
        }

        try {

            const response =
                await fetch(
                    "${pedidosUrl}?pedidoId="
                    + encodeURIComponent(pedidoId),
                    {
                        method: "DELETE"
                    }
                );

            const resultado =
                await response.json();

            if (!response.ok) {

                alert(
                    resultado.mensaje
                    || "No se pudo eliminar el pedido."
                );

                return;
            }

            alert(
                resultado.mensaje
                || "Pedido eliminado correctamente."
            );

            window.location.reload();

        } catch (error) {

            console.error(error);

            alert(
                "No fue posible comunicarse con el servidor."
            );
        }
    }

</script>

</body>
</html>
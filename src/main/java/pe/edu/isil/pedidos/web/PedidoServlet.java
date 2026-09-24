package pe.edu.isil.pedidos.web;

import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import pe.edu.isil.pedidos.domain.Pedido;
import pe.edu.isil.pedidos.service.PedidoException;
import pe.edu.isil.pedidos.service.PedidoService;

/**
 * Servlet que maneja las solicitudes relacionadas con los pedidos.
 */
@WebServlet("/pedidos")
public class PedidoServlet extends HttpServlet {

  @EJB
  private PedidoService pedidoService;

  @Override
  protected void doGet(
          HttpServletRequest request,
          HttpServletResponse response)
          throws ServletException, IOException {

    cargarDatosVista(request);

    request.getRequestDispatcher(
            "/WEB-INF/views/pedidos.jsp"
    ).forward(request, response);
  }

  @Override
  protected void doPost(
          HttpServletRequest request,
          HttpServletResponse response)
          throws ServletException, IOException {

    request.setCharacterEncoding(
            StandardCharsets.UTF_8.name()
    );

    try {
      String cliente = request.getParameter("cliente");

      Long productoId =
              Long.valueOf(
                      request.getParameter("productoId")
              );

      int cantidad =
              Integer.parseInt(
                      request.getParameter("cantidad")
              );

      Pedido pedido =
              pedidoService.registrarPedido(
                      cliente,
                      productoId,
                      cantidad
              );

      response.sendRedirect(
              request.getContextPath()
                      + "/pedidos?creado="
                      + pedido.getId()
      );

    } catch (NumberFormatException e) {

      mostrarErrorNegocio(
              request,
              response,
              "Producto o cantidad inválidos."
      );

    } catch (PedidoException e) {

      mostrarErrorNegocio(
              request,
              response,
              e.getMessage()
      );

    } catch (RuntimeException e) {

      mostrarErrorGeneral(
              request,
              response
      );
    }
  }

  /**
   * Actualiza un pedido mediante una solicitud HTTP PUT.
   */
  @Override
  protected void doPut(
          HttpServletRequest request,
          HttpServletResponse response)
          throws IOException {

    try {
      request.setCharacterEncoding(
              StandardCharsets.UTF_8.name()
      );

      String body =
              new String(
                      request.getInputStream().readAllBytes(),
                      StandardCharsets.UTF_8
              );

      String pedidoIdParam =
              obtenerParametro(body, "pedidoId");

      String cliente =
              obtenerParametro(body, "cliente");

      String productoIdParam =
              obtenerParametro(body, "productoId");

      String cantidadParam =
              obtenerParametro(body, "cantidad");

      if (pedidoIdParam == null
              || productoIdParam == null
              || cantidadParam == null
              || cliente == null) {

        enviarJsonError(
                response,
                HttpServletResponse.SC_BAD_REQUEST,
                "Faltan datos para actualizar el pedido."
        );

        return;
      }

      Long pedidoId =
              Long.valueOf(pedidoIdParam);

      Long productoId =
              Long.valueOf(productoIdParam);

      int cantidad =
              Integer.parseInt(cantidadParam);

      Pedido pedido =
              pedidoService.actualizarPedido(
                      pedidoId,
                      cliente,
                      productoId,
                      cantidad
              );

      enviarJsonExito(
              response,
              "Pedido #" + pedido.getId()
                      + " actualizado correctamente."
      );

    } catch (NumberFormatException e) {

      enviarJsonError(
              response,
              HttpServletResponse.SC_BAD_REQUEST,
              "El ID del pedido, producto o la cantidad no son válidos."
      );

    } catch (PedidoException e) {

      enviarJsonError(
              response,
              HttpServletResponse.SC_BAD_REQUEST,
              e.getMessage()
      );

    } catch (RuntimeException e) {

      enviarJsonError(
              response,
              HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              "Ocurrió un error interno al actualizar el pedido."
      );
    }
  }

  /**
   * Elimina un pedido mediante una solicitud HTTP DELETE.
   */
  @Override
  protected void doDelete(
          HttpServletRequest request,
          HttpServletResponse response)
          throws IOException {

    try {
      String pedidoIdParam =
              request.getParameter("pedidoId");

      if (pedidoIdParam == null
              || pedidoIdParam.isBlank()) {

        enviarJsonError(
                response,
                HttpServletResponse.SC_BAD_REQUEST,
                "Debe indicar el ID del pedido."
        );

        return;
      }

      Long pedidoId =
              Long.valueOf(pedidoIdParam);

      pedidoService.eliminarPedido(pedidoId);

      enviarJsonExito(
              response,
              "Pedido eliminado correctamente."
      );

    } catch (NumberFormatException e) {

      enviarJsonError(
              response,
              HttpServletResponse.SC_BAD_REQUEST,
              "El ID del pedido no es válido."
      );

    } catch (PedidoException e) {

      int estado =
              e.getMessage() != null
                      && e.getMessage().contains(
                      "no existe"
              )
                      ? HttpServletResponse.SC_NOT_FOUND
                      : HttpServletResponse.SC_BAD_REQUEST;

      enviarJsonError(
              response,
              estado,
              e.getMessage()
      );

    } catch (RuntimeException e) {

      enviarJsonError(
              response,
              HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              "Ocurrió un error interno al eliminar el pedido."
      );
    }
  }

  /**
   * Obtiene un parámetro enviado en formato
   * application/x-www-form-urlencoded.
   */
  private String obtenerParametro(
          String body,
          String nombre) {

    if (body == null || body.isBlank()) {
      return null;
    }

    String[] parametros =
            body.split("&");

    for (String parametro : parametros) {

      String[] partes =
              parametro.split("=", 2);

      if (partes.length != 2) {
        continue;
      }

      String clave =
              URLDecoder.decode(
                      partes[0],
                      StandardCharsets.UTF_8
              );

      if (nombre.equals(clave)) {

        return URLDecoder.decode(
                partes[1],
                StandardCharsets.UTF_8
        );
      }
    }

    return null;
  }

  /**
   * Carga productos y pedidos para la vista principal.
   */
  private void cargarDatosVista(
          HttpServletRequest request) {

    request.setAttribute(
            "productos",
            pedidoService.listarProductos()
    );

    request.setAttribute(
            "pedidos",
            pedidoService.listarPedidos()
    );
  }

  /**
   * Muestra un error de negocio en la vista.
   */
  private void mostrarErrorNegocio(
          HttpServletRequest request,
          HttpServletResponse response,
          String mensaje)
          throws ServletException, IOException {

    response.setStatus(
            HttpServletResponse.SC_BAD_REQUEST
    );

    request.setAttribute(
            "clienteIngresado",
            request.getParameter("cliente")
    );

    request.setAttribute(
            "cantidadIngresada",
            request.getParameter("cantidad")
    );

    request.setAttribute(
            "error",
            mensaje
    );

    cargarDatosVista(request);

    request.getRequestDispatcher(
            "/WEB-INF/views/pedidos.jsp"
    ).forward(request, response);
  }

  /**
   * Muestra un error general.
   */
  private void mostrarErrorGeneral(
          HttpServletRequest request,
          HttpServletResponse response)
          throws ServletException, IOException {

    response.setStatus(
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR
    );

    request.setAttribute(
            "error",
            "Ocurrió un error interno al procesar la solicitud."
    );

    request.getRequestDispatcher(
            "/WEB-INF/views/error.jsp"
    ).forward(request, response);
  }

  /**
   * Envía una respuesta JSON indicando éxito.
   */
  private void enviarJsonExito(
          HttpServletResponse response,
          String mensaje)
          throws IOException {

    response.setStatus(
            HttpServletResponse.SC_OK
    );

    response.setContentType(
            "application/json"
    );

    response.setCharacterEncoding(
            StandardCharsets.UTF_8.name()
    );

    response.getWriter().write(
            "{\"ok\":true,\"mensaje\":\""
                    + escaparJson(mensaje)
                    + "\"}"
    );
  }

  /**
   * Envía una respuesta JSON indicando error.
   */
  private void enviarJsonError(
          HttpServletResponse response,
          int estado,
          String mensaje)
          throws IOException {

    response.setStatus(estado);

    response.setContentType(
            "application/json"
    );

    response.setCharacterEncoding(
            StandardCharsets.UTF_8.name()
    );

    response.getWriter().write(
            "{\"ok\":false,\"mensaje\":\""
                    + escaparJson(mensaje)
                    + "\"}"
    );
  }

  /**
   * Escapa caracteres especiales para una respuesta JSON.
   */
  private String escaparJson(String texto) {

    if (texto == null) {
      return "";
    }

    return texto
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
  }
}
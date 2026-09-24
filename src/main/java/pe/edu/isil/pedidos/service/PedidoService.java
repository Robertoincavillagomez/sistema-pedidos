package pe.edu.isil.pedidos.service;

import pe.edu.isil.pedidos.domain.Pedido;
import pe.edu.isil.pedidos.domain.Producto;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio EJB que maneja la lógica de negocio relacionada con los pedidos.
 */
@Stateless
public class PedidoService {

  @PersistenceContext(unitName = "PedidosPU")
  private EntityManager entityManager;

  /**
   * Registra un nuevo pedido en el sistema.
   *
   * @param cliente    Nombre del cliente que realiza el pedido.
   * @param productoId ID del producto que se desea comprar.
   * @param cantidad   Cantidad de productos a comprar.
   * @return El pedido registrado.
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public Pedido registrarPedido(
          String cliente,
          Long productoId,
          int cantidad) {

    validarDatos(cliente, productoId, cantidad);

    Producto producto = entityManager.find(Producto.class, productoId);

    if (producto == null) {
      throw new PedidoException("El producto no existe.");
    }

    try {
      producto.descontarStock(cantidad);
    } catch (IllegalArgumentException | IllegalStateException e) {
      throw new PedidoException(e.getMessage());
    }

    BigDecimal total =
            producto.getPrecio()
                    .multiply(BigDecimal.valueOf(cantidad));

    Pedido pedido =
            new Pedido(
                    cliente.trim(),
                    producto,
                    cantidad,
                    total
            );

    entityManager.persist(pedido);

    return pedido;
  }

  /**
   * Lista todos los productos disponibles en el sistema.
   *
   * @return Lista de productos.
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public List<Producto> listarProductos() {

    inicializarProductosSiEsNecesario();

    return entityManager
            .createQuery(
                    """
                    select p
                    from Producto p
                    order by p.id
                    """,
                    Producto.class
            )
            .getResultList();
  }

  /**
   * Lista todos los pedidos realizados en el sistema.
   *
   * @return Lista de pedidos.
   */
  @TransactionAttribute(TransactionAttributeType.SUPPORTS)
  public List<Pedido> listarPedidos() {

    return entityManager
            .createQuery(
                    """
                    select p
                    from Pedido p
                    join fetch p.producto
                    order by p.id desc
                    """,
                    Pedido.class
            )
            .getResultList();
  }

  /**
   * Busca un pedido por su identificador.
   *
   * @param pedidoId Identificador del pedido.
   * @return El pedido encontrado.
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public Pedido buscarPedido(Long pedidoId) {

    if (pedidoId == null || pedidoId <= 0) {
      throw new PedidoException("El ID del pedido no es válido.");
    }

    Pedido pedido = entityManager.find(Pedido.class, pedidoId);

    if (pedido == null) {
      throw new PedidoException("El pedido no existe.");
    }

    return pedido;
  }

  /**
   * Actualiza los datos modificables de un pedido.
   *
   * Si cambia únicamente el cliente, no se modifica el stock.
   *
   * Si cambia la cantidad del mismo producto, se modifica el stock
   * solamente por la diferencia entre la cantidad anterior y la nueva.
   *
   * Si cambia el producto, se devuelve al stock la cantidad del
   * producto anterior y se descuenta la cantidad solicitada del
   * nuevo producto.
   *
   * @param pedidoId   ID del pedido.
   * @param cliente    Nuevo cliente.
   * @param productoId Nuevo producto.
   * @param cantidad   Nueva cantidad.
   * @return El pedido actualizado.
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public Pedido actualizarPedido(
          Long pedidoId,
          String cliente,
          Long productoId,
          int cantidad) {

    if (pedidoId == null || pedidoId <= 0) {
      throw new PedidoException("El ID del pedido no es válido.");
    }

    if (cliente == null || cliente.isBlank()) {
      throw new PedidoException("El cliente es obligatorio.");
    }

    if (productoId == null) {
      throw new PedidoException("Debe seleccionar un producto.");
    }

    if (cantidad <= 0) {
      throw new PedidoException(
              "La cantidad debe ser mayor que cero."
      );
    }

    Pedido pedido = buscarPedido(pedidoId);

    Producto productoAnterior = pedido.getProducto();

    Producto productoNuevo =
            entityManager.find(Producto.class, productoId);

    if (productoNuevo == null) {
      throw new PedidoException("El producto no existe.");
    }

    int cantidadAnterior = pedido.getCantidad();

    /*
     * CASO 1:
     * El producto no cambia.
     */
    if (productoAnterior.getId().equals(productoNuevo.getId())) {

      int diferencia = cantidad - cantidadAnterior;

      /*
       * Si la nueva cantidad es mayor, se descuenta solamente
       * la diferencia del stock.
       */
      if (diferencia > 0) {
        try {
          productoAnterior.descontarStock(diferencia);
        } catch (IllegalArgumentException | IllegalStateException e) {
          throw new PedidoException(e.getMessage());
        }
      }

      /*
       * Si la nueva cantidad es menor, se devuelve al stock
       * solamente la diferencia.
       */
      if (diferencia < 0) {
        productoAnterior.reponerStock(-diferencia);
      }

      BigDecimal nuevoTotal =
              productoNuevo.getPrecio()
                      .multiply(BigDecimal.valueOf(cantidad));

      pedido.actualizar(
              cliente.trim(),
              productoNuevo,
              cantidad,
              nuevoTotal
      );

      return pedido;
    }

    /*
     * CASO 2:
     * El producto cambia.
     *
     * Primero validamos que el nuevo producto tenga stock
     * suficiente. De esta forma evitamos modificar el producto
     * anterior si la operación no puede completarse.
     */
    if (cantidad > productoNuevo.getStock()) {
      throw new PedidoException(
              "Stock insuficiente. Disponible: "
                      + productoNuevo.getStock()
      );
    }

    /*
     * Devolvemos al stock del producto anterior la cantidad
     * que pertenecía al pedido.
     */
    productoAnterior.reponerStock(cantidadAnterior);

    /*
     * Descontamos del nuevo producto la cantidad solicitada.
     */
    try {
      productoNuevo.descontarStock(cantidad);
    } catch (IllegalArgumentException | IllegalStateException e) {
      throw new PedidoException(e.getMessage());
    }

    BigDecimal nuevoTotal =
            productoNuevo.getPrecio()
                    .multiply(BigDecimal.valueOf(cantidad));

    pedido.actualizar(
            cliente.trim(),
            productoNuevo,
            cantidad,
            nuevoTotal
    );

    return pedido;
  }

  /**
   * Elimina un pedido y devuelve al stock la cantidad que había
   * sido reservada para dicho pedido.
   *
   * @param pedidoId ID del pedido que se desea eliminar.
   */
  @TransactionAttribute(TransactionAttributeType.REQUIRED)
  public void eliminarPedido(Long pedidoId) {

    Pedido pedido = buscarPedido(pedidoId);

    Producto producto = pedido.getProducto();

    producto.reponerStock(pedido.getCantidad());

    entityManager.remove(pedido);
  }

  /**
   * Valida los datos de entrada para registrar un pedido.
   *
   * @param cliente    Nombre del cliente.
   * @param productoId ID del producto.
   * @param cantidad   Cantidad de productos.
   */
  private void validarDatos(
          String cliente,
          Long productoId,
          int cantidad) {

    if (cliente == null || cliente.isBlank()) {
      throw new PedidoException("El cliente es obligatorio.");
    }

    if (productoId == null) {
      throw new PedidoException("Debe seleccionar un producto.");
    }

    if (cantidad <= 0) {
      throw new PedidoException(
              "La cantidad debe ser mayor que cero."
      );
    }
  }

  /**
   * Inicializa algunos productos de ejemplo si no existen
   * en la base de datos.
   */
  private void inicializarProductosSiEsNecesario() {

    Long cantidad =
            entityManager
                    .createQuery(
                            """
                            select count(p)
                            from Producto p
                            """,
                            Long.class
                    )
                    .getSingleResult();

    if (cantidad == 0) {

      entityManager.persist(
              new Producto(
                      "Laptop",
                      new BigDecimal("2500.00"),
                      5
              )
      );

      entityManager.persist(
              new Producto(
                      "Monitor",
                      new BigDecimal("850.00"),
                      8
              )
      );

      entityManager.persist(
              new Producto(
                      "Teclado",
                      new BigDecimal("120.00"),
                      15
              )
      );
    }
  }
}
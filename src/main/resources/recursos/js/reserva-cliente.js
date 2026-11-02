import { api, conCarga, mostrarError, pedirMotivo, ruta, toast, vacio } from './nucleo.js';
import { contenidoDetalle } from './detalle-reserva.js';

const detalle = document.querySelector('[data-detalle]');
const codigo = document.body.dataset.codigo;

async function cargar() {
  detalle.innerHTML = '<div class="esqueleto esqueleto--titulo"></div><div class="esqueleto esqueleto--bloque"></div>';
  try {
    const reserva = (await api(`/api/cliente/reservas/${encodeURIComponent(codigo)}`)).datos;
    const pendiente = reserva.estado === 'PENDIENTE';
    detalle.innerHTML = `${contenidoDetalle(reserva)}
      ${pendiente ? `<div class="resultado__acciones">
        <a class="boton boton--acento" href="${ruta(`/carrito?reserva=${encodeURIComponent(codigo)}&paso=4`)}">Pagar reserva</a>
        <button class="boton boton--peligro" type="button" data-cancelar>Cancelar reserva</button></div>` : ''}`;
    const cancelar = detalle.querySelector('[data-cancelar]');
    cancelar?.addEventListener('click', () => conCarga(cancelar, async () => {
      const motivo = await pedirMotivo({ titulo: 'Cancelar reserva', mensaje: 'La habitación quedará libre para otros huéspedes.', textoConfirmar: 'Cancelar reserva' });
      if (!motivo) {
        return;
      }
      try {
        const respuesta = await api(`/api/cliente/reservas/${encodeURIComponent(codigo)}/cancelar`, { metodo: 'POST', cuerpo: { motivo } });
        toast(respuesta.mensaje);
        cargar();
      } catch (error) {
        mostrarError(error);
      }
    }));
  } catch (error) {
    detalle.innerHTML = vacio({ nombreIcono: 'alerta', titulo: 'No encontramos esta reserva', texto: error.message });
  }
}

cargar();

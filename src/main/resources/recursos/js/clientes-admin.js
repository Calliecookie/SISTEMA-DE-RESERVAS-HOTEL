import { abrirModal, api, conCarga, consulta, datosFormulario, debounce, enviarFormulario, esc, errorCarga, esqueletoTabla, icono, moneda, mostrarError, paginar, toast, vacio } from './nucleo.js';

const filtros = document.querySelector('[data-filtros]');
const tabla = document.querySelector('[data-tabla]');
const paginacion = document.querySelector('[data-paginacion]');
let clientes = [];
let paginaActual = 0;

async function cargar(pagina = 0) {
  paginaActual = pagina;
  tabla.innerHTML = esqueletoTabla(8, 6);
  try {
    const { datos } = await api(`/api/administracion/clientes${consulta({ busqueda: filtros.busqueda.value, pagina, tamano: 10 })}`);
    clientes = datos.elementos;
    tabla.innerHTML = clientes.length ? `<table class="tabla">
      <thead><tr><th>Cliente</th><th>DNI</th><th>Celular</th><th class="derecha">Reservas</th><th class="derecha">Total pagado</th><th>Estado</th><th><span class="oculto-visual">Acciones</span></th></tr></thead>
      <tbody>${clientes.map((item) => `<tr>
        <td><strong>${esc(item.nombres)} ${esc(item.apellidos)}</strong><br><span class="texto-suave">${esc(item.correo)}</span></td>
        <td>${esc(item.dni)} ${item.dniVerificado ? '<span class="insignia insignia--exito">Verificado</span>' : ''}</td>
        <td>${esc(item.telefono || 'Sin registrar')}</td><td class="derecha">${item.cantidadReservas}</td><td class="derecha">${moneda(item.totalPagado)}</td>
        <td><span class="insignia insignia--${item.activo ? 'exito' : 'neutra'}">${item.activo ? 'Activo' : 'Desactivado'}</span></td>
        <td><div class="tabla__acciones">
          <button class="boton boton--fantasma boton--icono" type="button" data-editar="${item.id}" aria-label="Editar ${esc(item.nombres)}">${icono('lapiz')}</button>
          <button class="boton boton--fantasma boton--icono" type="button" data-activar="${item.id}" aria-label="${item.activo ? 'Desactivar' : 'Activar'} ${esc(item.nombres)}">${icono('encender')}</button>
        </div></td></tr>`).join('')}</tbody></table>`
      : vacio({ nombreIcono: 'usuarios', titulo: 'No hay clientes con esa búsqueda' });
    paginar(paginacion, datos, cargar);
  } catch {
    tabla.innerHTML = errorCarga();
  }
}

function formulario(item) {
  const campo = (nombre, etiqueta, valor) => `<div class="campo" data-campo="${nombre}"><label class="campo__etiqueta" for="c-${nombre}">${etiqueta}</label><input class="entrada" id="c-${nombre}" name="${nombre}" value="${esc(valor)}"><span class="campo__error"></span></div>`;
  const { modal, cerrar } = abrirModal({
    titulo: 'Editar cliente',
    contenido: `<form class="formulario" data-formulario novalidate>
      <div class="formulario__fila formulario__fila--2">${campo('nombres', 'Nombres', item.nombres)}${campo('apellidos', 'Apellidos', item.apellidos)}</div>
      ${campo('telefono', 'Celular', item.telefono)}${campo('direccion', 'Dirección', item.direccion)}
    </form>`,
    pie: '<button class="boton boton--fantasma" type="button" data-cerrar>Cancelar</button><button class="boton boton--primario" type="button" data-guardar>Guardar cambios</button>',
  });
  const form = modal.querySelector('[data-formulario]');
  const guardar = modal.querySelector('[data-guardar]');
  guardar.addEventListener('click', async () => {
    const respuesta = await enviarFormulario(form, guardar, () =>
      api(`/api/administracion/clientes/${item.id}`, { metodo: 'PUT', cuerpo: datosFormulario(form) }));
    if (respuesta) {
      toast(respuesta.mensaje);
      cerrar();
      cargar(paginaActual);
    }
  });
}

tabla.addEventListener('click', (evento) => {
  if (evento.target.closest('[data-reintentar]')) {
    cargar(paginaActual);
    return;
  }
  const editar = evento.target.closest('[data-editar]');
  if (editar) {
    formulario(clientes.find((item) => item.id === Number(editar.dataset.editar)));
  }
  const activar = evento.target.closest('[data-activar]');
  if (activar) {
    const item = clientes.find((elemento) => elemento.id === Number(activar.dataset.activar));
    conCarga(activar, async () => {
      try {
        await api(`/api/administracion/clientes/${item.id}/activo`, { metodo: 'PATCH', cuerpo: { activo: !item.activo } });
        toast(item.activo ? 'Cliente desactivado.' : 'Cliente activado.');
        cargar(paginaActual);
      } catch (error) {
        mostrarError(error);
      }
    });
  }
});

filtros.addEventListener('input', debounce(() => cargar(0), 300));
filtros.addEventListener('submit', (evento) => evento.preventDefault());
filtros.busqueda.value = new URLSearchParams(location.search).get('busqueda') || '';
cargar();

import { api, consulta, debounce, errorCarga, esc, esqueletoTabla, fechaHora, paginar, vacio, textoAccion } from './nucleo.js';

const filtros = document.querySelector('[data-filtros]');
const tabla = document.querySelector('[data-tabla]');
const paginacion = document.querySelector('[data-paginacion]');

async function cargar(pagina = 0) {
  tabla.innerHTML = esqueletoTabla(10, 5);
  try {
    const { datos } = await api(`/api/administracion/auditoria${consulta({ busqueda: filtros.busqueda.value, pagina })}`);
    tabla.innerHTML = datos.elementos.length ? `<table class="tabla">
      <thead><tr><th>Fecha</th><th>Usuario</th><th>Acción</th><th>Registro</th><th>Detalle</th><th>Dirección IP</th></tr></thead>
      <tbody>${datos.elementos.map((registro) => `<tr><td>${fechaHora(registro.creadoEn)}</td><td>${esc(registro.usuario)}</td>
        <td>${esc(textoAccion(registro.accion))}</td><td>${esc(registro.entidad)} ${esc(registro.entidadId)}</td>
        <td>${esc(registro.detalle)}</td><td>${esc(registro.direccionIp)}</td></tr>`).join('')}</tbody></table>`
      : vacio({ nombreIcono: 'historial', titulo: 'Sin registros', texto: 'Las operaciones administrativas aparecerán aquí.' });
    paginar(paginacion, datos, cargar);
  } catch {
    tabla.innerHTML = errorCarga();
  }
}

tabla.addEventListener('click', (evento) => {
  if (evento.target.closest('[data-reintentar]')) {
    cargar();
  }
});
filtros.addEventListener('input', debounce(() => cargar(0), 300));
filtros.addEventListener('submit', (evento) => evento.preventDefault());
cargar();

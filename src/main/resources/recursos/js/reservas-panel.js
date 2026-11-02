import { api } from './nucleo.js';
import { iniciarListaReservas } from './lista-reservas.js';

iniciarListaReservas({
  obtener: async (parametros, senal) => ({
    pagina: (await api(`/api/operaciones/reservas${parametros}`, { senal })).datos,
  }),
});

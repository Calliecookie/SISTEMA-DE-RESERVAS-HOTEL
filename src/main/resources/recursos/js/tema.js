(function () {
  var guardado = null;
  try {
    guardado = localStorage.getItem('tema');
  } catch (error) {
    guardado = null;
  }
  var oscuro = guardado ? guardado === 'oscuro' : window.matchMedia('(prefers-color-scheme: dark)').matches;
  document.documentElement.dataset.tema = oscuro ? 'oscuro' : 'claro';
})();

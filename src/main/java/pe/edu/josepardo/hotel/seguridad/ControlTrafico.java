package pe.edu.josepardo.hotel.seguridad;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public class ControlTrafico {

    public enum Decision {
        PERMITIR, LIMITAR, BLOQUEAR
    }

    private static final long MINUTO = 60_000L;

    private record Contador(long fin, int conteo, int excesos, boolean excesoContado) {
    }

    private final int excesosParaBloquear;
    private final long duracionBloqueo;
    private final int maximoIps;
    private final LongSupplier reloj;
    private final Map<String, Contador> contadores = new ConcurrentHashMap<>();
    private final Map<String, Long> bloqueos = new ConcurrentHashMap<>();

    public ControlTrafico(int excesosParaBloquear, int minutosBloqueo, int maximoIps, LongSupplier reloj) {
        this.excesosParaBloquear = excesosParaBloquear;
        this.duracionBloqueo = minutosBloqueo * MINUTO;
        this.maximoIps = maximoIps;
        this.reloj = reloj;
    }

    public Decision evaluar(String ip, int limitePorMinuto) {
        long ahora = reloj.getAsLong();
        Long bloqueadoHasta = bloqueos.get(ip);
        if (bloqueadoHasta != null) {
            if (bloqueadoHasta > ahora) {
                return Decision.BLOQUEAR;
            }
            bloqueos.remove(ip);
        }
        if (contadores.size() >= maximoIps && !contadores.containsKey(ip)) {
            limpiar();
            if (contadores.size() >= maximoIps) {
                return Decision.LIMITAR;
            }
        }
        Contador contador = contadores.compute(ip, (clave, actual) -> {
            if (actual == null) {
                return new Contador(ahora + MINUTO, 1, 0, false);
            }
            if (actual.fin() <= ahora) {
                return new Contador(ahora + MINUTO, 1, actual.excesos(), false);
            }
            int conteo = actual.conteo() + 1;
            boolean nuevoExceso = conteo > limitePorMinuto && !actual.excesoContado();
            return new Contador(
                    actual.fin(),
                    conteo,
                    nuevoExceso ? actual.excesos() + 1 : actual.excesos(),
                    actual.excesoContado() || nuevoExceso);
        });
        if (contador.excesos() >= excesosParaBloquear) {
            bloqueos.put(ip, ahora + duracionBloqueo);
            contadores.remove(ip);
            return Decision.BLOQUEAR;
        }
        return contador.conteo() > limitePorMinuto ? Decision.LIMITAR : Decision.PERMITIR;
    }

    public long segundosRestantes(String ip) {
        Long hasta = bloqueos.get(ip);
        if (hasta == null) {
            return 60;
        }
        return Math.max(1, (hasta - reloj.getAsLong()) / 1000);
    }

    public int ipsBloqueadas() {
        return bloqueos.size();
    }

    public void limpiar() {
        long ahora = reloj.getAsLong();
        contadores.entrySet().removeIf(entrada -> entrada.getValue().fin() + 10 * MINUTO <= ahora);
        bloqueos.values().removeIf(hasta -> hasta <= ahora);
    }

    public static String normalizarIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "desconocida";
        }
        if (!ip.contains(":")) {
            return ip;
        }
        try {
            byte[] bytes = InetAddress.getByName(ip).getAddress();
            if (bytes.length != 16) {
                return ip;
            }
            StringBuilder prefijo = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                prefijo.append(String.format("%02x", bytes[i]));
            }
            return prefijo + "::/64";
        } catch (UnknownHostException excepcion) {
            return ip;
        }
    }
}

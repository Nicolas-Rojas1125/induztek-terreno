package cl.duocuc.induztek.msusuarios.service;

import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.ActualizarPerfilRequest;
import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.CambiarContrasenaRequest;
import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.CrearUsuarioRequest;
import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.RecuperarResponse;
import cl.duocuc.induztek.msusuarios.dto.UsuarioDtos.RestablecerRequest;
import cl.duocuc.induztek.msusuarios.exception.ApiException;
import cl.duocuc.induztek.msusuarios.model.Rol;
import cl.duocuc.induztek.msusuarios.model.Usuario;
import cl.duocuc.induztek.msusuarios.repository.UsuarioRepository;
import cl.duocuc.induztek.msusuarios.util.Contrasenas;
import cl.duocuc.induztek.msusuarios.util.ValidadorRut;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    static final long VIGENCIA_CODIGO_MS = 15 * 60 * 1000L;
    private static final Pattern CORREO = Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");
    private static final int TAMANO_MAXIMO = 100;

    private final UsuarioRepository repositorio;
    private final boolean mostrarCodigo;

    public UsuarioService(UsuarioRepository repositorio,
                          @Value("${app.recuperacion.mostrar-codigo:true}") boolean mostrarCodigo) {
        this.repositorio = repositorio;
        this.mostrarCodigo = mostrarCodigo;
    }

    @Transactional(readOnly = true)
    public Usuario login(String correo, String contrasena) {
        Usuario u = repositorio.findByCorreoIgnoreCase(correo.trim())
                .orElseThrow(() -> ApiException.noAutorizado("Correo o contraseña incorrectos"));
        if (!Contrasenas.verificar(contrasena, u.getSal(), u.getPasswordHash())) {
            throw ApiException.noAutorizado("Correo o contraseña incorrectos");
        }
        if (!u.isActivo()) {
            throw ApiException.prohibido("Usuario desactivado. Contacte al administrador");
        }
        return u;
    }

    @Transactional(readOnly = true)
    public Page<Usuario> listar(String buscar, String rol, Boolean activo, Long idCliente, int pagina, int tamano) {
        Rol filtroRol = Rol.desdeTexto(rol);
        String texto = buscar == null ? "" : buscar.trim().toLowerCase();
        Specification<Usuario> spec = (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (!texto.isEmpty()) {
                String patron = "%" + texto + "%";
                condiciones.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(root.get("correo")), patron),
                        cb.like(cb.lower(root.get("rut")), patron)));
            }
            if (filtroRol != null) {
                condiciones.add(cb.equal(root.get("rol"), filtroRol));
            }
            if (activo != null) {
                condiciones.add(cb.equal(root.get("activo"), activo));
            }
            if (idCliente != null) {
                condiciones.add(cb.equal(root.get("idCliente"), idCliente));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
        PageRequest pageable = PageRequest.of(Math.max(pagina, 0), limitar(tamano),
                Sort.by(Sort.Order.desc("activo"), Sort.Order.asc("nombre")));
        return repositorio.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<Usuario> directorio() {
        return repositorio.findAll(Sort.by("nombre"));
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> ApiException.noEncontrado("No existe el usuario " + id));
    }

    @Transactional
    public Usuario crear(Long idActor, CrearUsuarioRequest req) {
        Usuario actor = actorActivo(idActor);
        Rol nuevoRol = Rol.desdeTexto(req.rol());
        if (nuevoRol == null) {
            throw ApiException.invalido("Rol inválido: use ADMINISTRADOR, INGENIERO, TECNICO o CLIENTE");
        }
        if (!actor.getRol().puedeCrear(nuevoRol)) {
            throw ApiException.prohibido(mensajeNoPuedeCrear(actor.getRol(), nuevoRol));
        }
        if (!ValidadorRut.esValido(req.rut())) {
            throw ApiException.invalido("RUT inválido: revise el dígito verificador");
        }
        String rut = ValidadorRut.formatear(req.rut());
        String correo = req.correo().trim().toLowerCase();
        validarCorreo(correo);
        String errorClave = Contrasenas.errorFormato(req.contrasena());
        if (errorClave != null) {
            throw ApiException.invalido(errorClave);
        }
        if (nuevoRol == Rol.CLIENTE && req.idCliente() == null) {
            throw ApiException.invalido("Un usuario cliente debe pertenecer a una empresa");
        }
        if (repositorio.existsByRut(rut)) {
            throw ApiException.conflicto("Ya existe un usuario con ese RUT");
        }
        if (repositorio.existsByCorreoIgnoreCase(correo)) {
            throw ApiException.conflicto("Ya existe un usuario con ese correo");
        }
        Usuario u = new Usuario();
        u.setRut(rut);
        u.setNombre(req.nombre().trim());
        u.setCorreo(correo);
        u.setTelefono(textoSeguro(req.telefono()));
        u.setCargo(textoSeguro(req.cargo()));
        u.setRol(nuevoRol);
        if (nuevoRol == Rol.CLIENTE) {
            u.setIdCliente(req.idCliente());
            u.setNombreCliente(textoSeguro(req.nombreCliente()));
        }
        u.setActivo(true);
        String sal = Contrasenas.nuevaSal();
        u.setSal(sal);
        u.setPasswordHash(Contrasenas.hash(req.contrasena(), sal));
        u.setCreadoPor(actor.getId());
        return repositorio.save(u);
    }

    @Transactional
    public Usuario actualizarPerfil(Long idActor, Long id, ActualizarPerfilRequest req) {
        Usuario actor = actorActivo(idActor);
        Usuario u = obtener(id);
        boolean esElMismo = actor.getId().equals(u.getId());
        if (!esElMismo && !actor.getRol().puedeCrear(u.getRol())) {
            throw ApiException.prohibido("Solo puede editar su propio perfil o cuentas que usted administra");
        }
        String correo = req.correo().trim().toLowerCase();
        validarCorreo(correo);
        if (repositorio.existsByCorreoIgnoreCaseAndIdNot(correo, id)) {
            throw ApiException.conflicto("Ese correo ya está en uso por otra cuenta");
        }
        u.setNombre(req.nombre().trim());
        u.setCorreo(correo);
        u.setTelefono(textoSeguro(req.telefono()));
        u.setCargo(textoSeguro(req.cargo()));
        return repositorio.save(u);
    }

    @Transactional
    public void cambiarContrasena(Long idActor, Long id, CambiarContrasenaRequest req) {
        if (idActor == null || !idActor.equals(id)) {
            throw ApiException.prohibido("Solo el titular puede cambiar su contraseña");
        }
        Usuario u = obtener(id);
        if (!Contrasenas.verificar(req.actual(), u.getSal(), u.getPasswordHash())) {
            throw ApiException.invalido("La contraseña actual no es correcta");
        }
        String error = Contrasenas.errorFormato(req.nueva());
        if (error != null) {
            throw ApiException.invalido(error);
        }
        if (req.nueva().equals(req.actual())) {
            throw ApiException.invalido("La nueva contraseña debe ser distinta de la actual");
        }
        asignarContrasena(u, req.nueva());
        repositorio.save(u);
    }

    @Transactional
    public Usuario cambiarEstado(Long idActor, Long id, boolean activo) {
        Usuario actor = actorActivo(idActor);
        Usuario u = obtener(id);
        if (actor.getId().equals(u.getId())) {
            throw ApiException.invalido("No puede activar ni desactivar su propia cuenta");
        }
        if (!actor.getRol().puedeCrear(u.getRol())) {
            throw ApiException.prohibido("No tiene permiso para administrar cuentas de " + u.getRol().getEtiqueta());
        }
        if (!activo && u.getRol() == Rol.ADMINISTRADOR && repositorio.countByRolAndActivoTrue(Rol.ADMINISTRADOR) <= 1) {
            throw ApiException.conflicto("Debe quedar al menos un administrador activo");
        }
        u.setActivo(activo);
        return repositorio.save(u);
    }

    @Transactional
    public void eliminar(Long idActor, Long id) {
        Usuario actor = actorActivo(idActor);
        if (actor.getRol() != Rol.ADMINISTRADOR) {
            throw ApiException.prohibido("Solo un administrador puede eliminar cuentas");
        }
        Usuario u = obtener(id);
        if (actor.getId().equals(u.getId())) {
            throw ApiException.invalido("No puede eliminar su propia cuenta");
        }
        if (u.getRol() == Rol.ADMINISTRADOR && u.isActivo() && repositorio.countByRolAndActivoTrue(Rol.ADMINISTRADOR) <= 1) {
            throw ApiException.conflicto("Debe quedar al menos un administrador activo");
        }
        repositorio.delete(u);
    }

    @Transactional
    public RecuperarResponse recuperar(String correo) {
        String mensaje = "Si el correo está registrado, se generó un código de recuperación válido por 15 minutos";
        Usuario u = repositorio.findByCorreoIgnoreCase(correo.trim()).orElse(null);
        if (u == null || !u.isActivo()) {
            return new RecuperarResponse(mensaje, null, null);
        }
        String codigo = Contrasenas.codigoRecuperacion();
        long expira = System.currentTimeMillis() + VIGENCIA_CODIGO_MS;
        u.setCodigoRecuperacion(codigo);
        u.setCodigoExpira(expira);
        repositorio.save(u);
        return new RecuperarResponse(mensaje, mostrarCodigo ? codigo : null, expira);
    }

    @Transactional
    public void restablecer(RestablecerRequest req) {
        Usuario u = repositorio.findByCorreoIgnoreCase(req.correo().trim())
                .orElseThrow(() -> ApiException.invalido("El código no es válido"));
        if (u.getCodigoRecuperacion() == null || !u.getCodigoRecuperacion().equals(req.codigo().trim())) {
            throw ApiException.invalido("El código no es válido");
        }
        if (u.getCodigoExpira() == null || u.getCodigoExpira() < System.currentTimeMillis()) {
            throw ApiException.invalido("El código expiró: solicite uno nuevo");
        }
        String error = Contrasenas.errorFormato(req.nueva());
        if (error != null) {
            throw ApiException.invalido(error);
        }
        asignarContrasena(u, req.nueva());
        u.setCodigoRecuperacion(null);
        u.setCodigoExpira(null);
        repositorio.save(u);
    }

    public List<Rol> rolesCreables(Long idActor) {
        Usuario actor = actorActivo(idActor);
        return actor.getRol().rolesQuePuedeCrear().stream().toList();
    }

    Usuario actorActivo(Long idActor) {
        if (idActor == null) {
            throw ApiException.noAutorizado("Falta el encabezado X-Usuario-Id");
        }
        Usuario actor = repositorio.findById(idActor)
                .orElseThrow(() -> ApiException.noAutorizado("El usuario que realiza la acción no existe"));
        if (!actor.isActivo()) {
            throw ApiException.prohibido("Usuario desactivado");
        }
        return actor;
    }

    static String mensajeNoPuedeCrear(Rol actor, Rol nuevo) {
        return switch (actor) {
            case INGENIERO -> "Un Ingeniero solo puede crear cuentas de Técnico y de Cliente";
            case TECNICO, CLIENTE -> "Su rol no permite crear cuentas";
            case ADMINISTRADOR -> "No puede crear cuentas de " + nuevo.getEtiqueta();
        };
    }

    private void asignarContrasena(Usuario u, String nueva) {
        String sal = Contrasenas.nuevaSal();
        u.setSal(sal);
        u.setPasswordHash(Contrasenas.hash(nueva, sal));
    }

    private void validarCorreo(String correo) {
        if (!CORREO.matcher(correo).matches()) {
            throw ApiException.invalido("El correo no es válido");
        }
    }

    private static String textoSeguro(String texto) {
        return texto == null ? "" : texto.trim();
    }

    private static int limitar(int tamano) {
        if (tamano <= 0) {
            return 10;
        }
        return Math.min(tamano, TAMANO_MAXIMO);
    }
}

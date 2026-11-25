package cl.duocuc.induztek.msusuarios.service;

import cl.duocuc.induztek.msusuarios.model.Rol;
import cl.duocuc.induztek.msusuarios.model.Usuario;
import cl.duocuc.induztek.msusuarios.repository.UsuarioRepository;
import cl.duocuc.induztek.msusuarios.util.Contrasenas;
import cl.duocuc.induztek.msusuarios.util.ValidadorRut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class InicializadorDatos implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(InicializadorDatos.class);

    private final UsuarioRepository repositorio;
    private final String correo;
    private final String contrasena;
    private final String rut;

    public InicializadorDatos(UsuarioRepository repositorio,
                              @Value("${app.admin.correo:admin@induztek.cl}") String correo,
                              @Value("${app.admin.contrasena:Induztek2026}") String contrasena,
                              @Value("${app.admin.rut:11.111.111-1}") String rut) {
        this.repositorio = repositorio;
        this.correo = correo;
        this.contrasena = contrasena;
        this.rut = rut;
    }

    @Override
    public void run(String... args) {
        if (repositorio.countByRolAndActivoTrue(Rol.ADMINISTRADOR) > 0) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setRut(ValidadorRut.formatear(rut));
        admin.setNombre("Administrador Induztek");
        admin.setCorreo(correo.toLowerCase());
        admin.setCargo("Jefe técnico");
        admin.setRol(Rol.ADMINISTRADOR);
        admin.setActivo(true);
        String sal = Contrasenas.nuevaSal();
        admin.setSal(sal);
        admin.setPasswordHash(Contrasenas.hash(contrasena, sal));
        repositorio.save(admin);
        LOG.info("Cuenta administrador inicial creada: {}", correo);
    }
}

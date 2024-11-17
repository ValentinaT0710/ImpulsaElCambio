package com.impulsaElCambio.Servicios;
import com.impulsaElCambio.Model.DesafioModel;
import com.impulsaElCambio.Model.UsuarioModel;
import com.impulsaElCambio.Repository.UsuarioRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;



import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.nio.file.Path;
import org.springframework.web.multipart.MultipartFile;



/**
 * Servicio que maneja las operaciones relacionadas con los usuarios.
 * Esta clase proporciona métodos para crear, actualizar, eliminar y recuperar usuarios,
 * así como para realizar operaciones de conteo y análisis de datos.
 */
@Service
public class UsuarioService {

   
    @Autowired
    private UsuarioRepository usuarioRepo;
    @Autowired
    private ForoService foroService;
    @Autowired
    private ProyectoService proyectoService;
    @Autowired
    private EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(UsuarioService.class);
    /**
     * Guarda un nuevo usuario o actualiza uno existente.
     *
     * @param usuario El usuario a guardar o actualizar.
     * @return El usuario guardado o actualizado.
     */
    public UsuarioModel saveUsuario(UsuarioModel usuario) {
        return usuarioRepo.save(usuario);
    }

    /**
     * Busca un usuario por su correo electrónico.
     *
     * @param correo El correo electrónico del usuario a buscar.
     * @return Un Optional que contiene el usuario si se encuentra, o vacío si no.
     */
    public Optional<UsuarioModel> findByCorreo(String correo) {
        return usuarioRepo.findByCorreo(correo);
    }

    /**
     * Busca un usuario por su ID.
     *
     * @param id El ID del usuario a buscar.
     * @return Un Optional que contiene el usuario si se encuentra, o vacío si no.
     */
    public Optional<UsuarioModel> findById(Long id) {
        return usuarioRepo.findById(id);
    }

    /**
     * Recupera todos los usuarios.
     *
     * @return Una lista de todos los usuarios.
     */
    public List<UsuarioModel> findAll() {
        return usuarioRepo.findAll();
    }


    /**
     * Cuenta el número total de usuarios.
     *
     * @return El número total de usuarios.
     */
    public int countAllUsers() {
        return (int) usuarioRepo.count();
    }

    /**
     * Cuenta el número de voluntarios activos.
     *
     * @return El número de usuarios con rol de VOLUNTARIO.
     */
    public int countActiveVolunteers() {
        return usuarioRepo.countByRol(UsuarioModel.Rol.VOLUNTARIO);
    }

    /**
     * Cuenta el número total de usuarios (versión alternativa).
     *
     * @return El número total de usuarios.
     */
    public long contarUsuarios() {
        return usuarioRepo.count();
    }

    /**
     * Cuenta el número de voluntarios activos.
     *
     * @return El número de usuarios con rol de VOLUNTARIO.
     */
    public int contarVoluntariosActivos() {
        return usuarioRepo.countByRol(UsuarioModel.Rol.VOLUNTARIO);
    }

    /**
     * Método para actualizar el perfil del usuario, incluyendo la opción de cambiar la contraseña.
     * Este método permite cambiar la contraseña del usuario si se proporciona una nueva.
     */
    @Transactional
    public UsuarioModel actualizarPerfil(Long id, String nombre, String correo, String numero, String password, MultipartFile imagen) {
        UsuarioModel usuario = usuarioRepo.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (nombre != null && !nombre.isEmpty()) {
            usuario.setNombre(nombre);
        }
        if (correo != null && !correo.isEmpty()) {
            usuario.setCorreo(correo);
        }
        if (numero != null && !numero.isEmpty()) {
            usuario.setNumero(numero);
        }
        if (password != null && !password.isEmpty()) {
            usuario.setPassword(password); // Considera encriptar la contraseña aquí
        }

        if (imagen != null && !imagen.isEmpty()) {
            String nombreArchivo = guardarImagen(imagen);
            usuario.setImagenPerfil(nombreArchivo);
        }

        return usuarioRepo.save(usuario);
    }

    private String guardarImagen(MultipartFile imagen) {
        try {
            String nombreArchivo = System.currentTimeMillis() + "_" + imagen.getOriginalFilename();
            Path rutaCompleta = Paths.get(RUTA_IMAGENES_PERFIL, nombreArchivo);
            Files.createDirectories(rutaCompleta.getParent());
            Files.write(rutaCompleta, imagen.getBytes());
            return "/Imagenes/imagen_perfil/" + nombreArchivo; // Asegúrate de que esta ruta sea accesible desde el navegador
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar la imagen de perfil", e);
        }
    }

    public String iniciarRecuperacionPassword(String correo) {
        Optional<UsuarioModel> usuarioOpt = usuarioRepo.findByCorreo(correo);
        
        if (usuarioOpt.isEmpty()) {
            logger.warn("Intento de recuperación de contraseña para correo no existente: {}", correo);
            return "Correo no encontrado";
        }
        
        try {
            UsuarioModel usuario = usuarioOpt.get();
            String token = UUID.randomUUID().toString();
            
            usuario.setResetPasswordToken(token);
            usuario.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(24));
            usuarioRepo.save(usuario);
            
            logger.info("Token generado para usuario {}: {}", usuario.getId(), token);
            
            String resetLink = "http://localhost:8080/cambiarPassword?token=" + token;
            emailService.enviarCorreoRecuperacion(correo, resetLink);
            
            return "Se ha enviado un enlace a tu correo para cambiar la contraseña";
        } catch (Exception e) {
            logger.error("Error al procesar recuperación de contraseña para {}: {}", correo, e.getMessage(), e);
            throw new RuntimeException("Error al procesar la solicitud de recuperación de contraseña");
        }
    }

    @Transactional
    public String cambiarPassword(String token, String nuevaPassword) {
        if (token == null || token.trim().isEmpty()) {
            logger.warn("Intento de cambio de contraseña con token vacío");
            throw new RuntimeException("Token no válido");
        }
        
        UsuarioModel usuario = usuarioRepo.findByResetPasswordToken(token)
            .orElseThrow(() -> {
                logger.warn("Intento de cambio de contraseña con token no encontrado: {}", token);
                return new RuntimeException("Token inválido o expirado");
            });
        
        if (usuario.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            logger.warn("Intento de cambio de contraseña con token expirado para usuario: {}", usuario.getId());
            throw new RuntimeException("El enlace ha expirado");
        }
        
        usuario.setPassword(nuevaPassword);
        usuario.setResetPasswordToken(null);
        usuario.setResetPasswordTokenExpiry(null);
        usuarioRepo.save(usuario);
        
        logger.info("Contraseña actualizada exitosamente para usuario: {}", usuario.getId());
        return "Contraseña actualizada exitosamente";
    }

    public double getPromedioParticipacionPorEmpresa() {
        List<String> empresas = usuarioRepo.findDistinctEmpresas();
        long totalUsuarios = usuarioRepo.count();
        return (double) totalUsuarios / empresas.size();
    }

    public String getEmpresaConMasUsuarios() {
        try {
            List<Object[]> resultados = usuarioRepo.findEmpresaConMasUsuarios();
            if (resultados.isEmpty()) {
                return "N/A";
            }
            
            // Si hay empate, concatenar las empresas
            if (resultados.size() > 1) {
                return resultados.stream()
                    .map(r -> (String) r[0])
                    .collect(Collectors.joining(" / "));
            }
            
            return (String) resultados.get(0)[0];
        } catch (Exception e) {
            logger.error("Error al obtener empresa con más usuarios: ", e);
            return "N/A";
        }
    }


    public List<Map<String, Object>> getDesgloseParticipacionEmpresarial() {
        List<Object[]> resultados = usuarioRepo.getDesgloseParticipacionEmpresarial();
        List<Map<String, Object>> desglose = new ArrayList<>();
        for (Object[] resultado : resultados) {
            Map<String, Object> empresaInfo = new HashMap<>();
            empresaInfo.put("empresa", resultado[0]);
            empresaInfo.put("usuariosRegistrados", resultado[1]);
            empresaInfo.put("proyectosActivos", resultado[2]);
            empresaInfo.put("tasaParticipacion", resultado[3]);
            desglose.add(empresaInfo);
        }
        return desglose;
    }

    public List<Map<String, Object>> obtenerEmpresasConMasVoluntariosEnProyectos(int limite) {
        Pageable topN = PageRequest.of(0, limite);
        List<Object[]> resultados = usuarioRepo.contarVoluntariosEnProyectosPorEmpresa(topN);

        return resultados.stream()
                .limit(limite)
                .map(resultado -> {
                    Map<String, Object> empresaInfo = new HashMap<>();
                    empresaInfo.put("empresa", resultado[0]);
                    empresaInfo.put("cantidadVoluntariosEnProyectos", resultado[1]);
                    return empresaInfo;
                }).collect(Collectors.toList());
    }

    @Transactional
    public boolean eliminarCuenta(Long id) {
        try {
            Optional<UsuarioModel> usuarioOpt = usuarioRepo.findById(id);
            if (usuarioOpt.isPresent()) {
                UsuarioModel usuario = usuarioOpt.get();
                
                // 1. Eliminar "me gusta" de comentarios
                foroService.eliminarMeEncantaDeUsuario(usuario.getId());
                
                // 2. Eliminar posts del foro
                foroService.eliminarPostsDeUsuario(usuario);
                
                // 3. Eliminar participaciones en proyectos
                proyectoService.eliminarParticipacionesDeUsuario(id);
                
                // 4. Limpiar desafíos completados
                if (usuario.getDesafiosCompletados() != null) {
                    usuario.getDesafiosCompletados().forEach(desafio -> {
                        desafio.getUsuariosCompletados().remove(usuario);
                    });
                    usuario.getDesafiosCompletados().clear();
                }
                
                // 5. Guardar cambios en las relaciones
                usuarioRepo.save(usuario);
                
                // 6. Finalmente eliminar el usuario
                usuarioRepo.delete(usuario);
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Error al eliminar la cuenta del usuario: " + id, e);
            throw new RuntimeException("Error al eliminar la cuenta: " + e.getMessage());
        }
    }

    @Transactional
    public UsuarioModel actualizarPerfilUsuario(Long id, String nombre, String correo, String numero, String passwordActual, String nuevaPassword, MultipartFile imagen) {
        Optional<UsuarioModel> usuarioOpt = usuarioRepo.findById(id);
        if (usuarioOpt.isPresent()) {
            UsuarioModel usuario = usuarioOpt.get();
            if (usuario.getPassword().equals(passwordActual)) {
                usuario.setNombre(nombre);
                usuario.setNumero(numero);
                if (nuevaPassword != null && !nuevaPassword.isEmpty()) {
                    usuario.setPassword(nuevaPassword);
                }
                if (imagen != null && !imagen.isEmpty()) {
                    String rutaImagen = guardarImagenPerfil(imagen);
                    usuario.setImagenPerfil(rutaImagen);
                }
                return usuarioRepo.save(usuario);
            }
        }
        return null;
    }

    private String guardarImagenPerfil(MultipartFile imagen) {
        try {
            String nombreArchivo = System.currentTimeMillis() + "_" + imagen.getOriginalFilename();
            Path rutaCompleta = Paths.get(RUTA_IMAGENES_PERFIL).resolve(nombreArchivo);
            
            Files.createDirectories(rutaCompleta.getParent());
            Files.copy(imagen.getInputStream(), rutaCompleta, StandardCopyOption.REPLACE_EXISTING);
            
            return "/imagenes/perfiles/" + nombreArchivo;
        } catch (IOException e) {
            logger.error("Error al guardar la imagen de perfil: " + e.getMessage(), e);
            throw new RuntimeException("Error al guardar la imagen de perfil del usuario: " + e.getMessage(), e);
        }
    }

    // Constante para la ruta de imágenes de perfil
    private static final String RUTA_IMAGENES_PERFIL = "src/main/resources/static/imagenes/perfiles/";

    public void verificarYOtorgarMedalla(Long usuarioId) {
        UsuarioModel usuario = usuarioRepo.findById(usuarioId).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Set<Long> proyectosParticipados = proyectoService.obtenerProyectosParticipados(usuarioId);
        
        String nuevaMedalla = determinarMedalla(proyectosParticipados.size());
        if (nuevaMedalla != null && (usuario.getInsignias() == null || !usuario.getInsignias().contains(nuevaMedalla))) {
            usuario.agregarInsignia(nuevaMedalla);
            usuarioRepo.save(usuario);
            logger.info("Nueva insignia otorgada a usuario {}: {}", usuarioId, nuevaMedalla);
        }
    }

    private String determinarMedalla(int cantidadProyectos) {
        if (cantidadProyectos >= 10) return "medal-3";
        if (cantidadProyectos >= 6) return "medal-2";
        if (cantidadProyectos >= 3) return "medal-1";
        return null;
    }

    public void otorgarInsignia(Long usuarioId, String insignia) {
        UsuarioModel usuario = findById(usuarioId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        usuario.agregarInsignia(insignia);
        usuarioRepo.save(usuario);
    }

    public List<String> obtenerInsigniasUsuario(Long usuarioId) {
        UsuarioModel usuario = findById(usuarioId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        return usuario.getInsigniasList();
    }

    public void agregarPuntos(Long usuarioId, int puntosRequeridos) {
        UsuarioModel usuario = usuarioRepo.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.agregarPuntos(puntosRequeridos);
        usuarioRepo.save(usuario);
        logger.info("Se agregaron {} puntos al usuario {}", puntosRequeridos, usuarioId);
    }

    public void validarToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("Token no válido");
        }
        
        UsuarioModel usuario = usuarioRepo.findByResetPasswordToken(token)
            .orElseThrow(() -> {
                logger.warn("Token no encontrado: {}", token);
                return new RuntimeException("Token inválido o expirado");
            });
        
        if (usuario.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            logger.warn("Token expirado para usuario: {}", usuario.getId());
            throw new RuntimeException("El enlace ha expirado");
        }
    }

    public List<DesafioModel> obtenerDesafiosCompletados(Long usuarioId) {
        UsuarioModel usuario = findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return new ArrayList<>(usuario.getDesafiosCompletados());
    }
}

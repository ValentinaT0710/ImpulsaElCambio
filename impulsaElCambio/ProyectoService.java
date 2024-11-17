package com.impulsaElCambio.Servicios;

import com.impulsaElCambio.Model.proyectoModel;
import com.impulsaElCambio.Model.UsuarioModel;
import com.impulsaElCambio.Repository.Repository;
import com.impulsaElCambio.Repository.UsuarioRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Servicio que maneja las operaciones relacionadas con los proyectos.
 * Esta clase proporciona métodos para crear, actualizar, eliminar y recuperar proyectos,
 * así como para gestionar la participación de usuarios en los proyectos.
 */
@Service
public class ProyectoService {

    @Autowired
    private Repository proyectoRepository;  
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private Environment env; // Para obtener la ruta de las imágenes desde la configuración

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Crea un nuevo proyecto.
     * @param nombre El nombre del proyecto.
     * @param descripcion La descripción del proyecto.
     * @param fechaExpiracion La fecha de expiración del proyecto.
     * @param imagen La imagen del proyecto.
     * @return El proyecto creado y guardado en la base de datos.
     */
    @Transactional
    public proyectoModel crearProyecto(String nombre, String descripcion, LocalDateTime fechaExpiracion, MultipartFile imagen) throws IOException {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del proyecto no puede estar vacío");
        }
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción del proyecto no puede estar vacía");
        }
        
        proyectoModel nuevoProyecto = new proyectoModel();
        nuevoProyecto.setNombre(nombre);
        nuevoProyecto.setDescripcion(descripcion);
        nuevoProyecto.setFechaCreacion(LocalDateTime.now());
        nuevoProyecto.setFechaExpiracion(fechaExpiracion);
        nuevoProyecto.setEstado("activo");

        if (imagen != null && !imagen.isEmpty()) {
            String rutaImagenes = env.getProperty("app.upload.dir", "src/main/resources/static/imagenes/proyectos/");
            String nombreArchivo = System.currentTimeMillis() + "_" + imagen.getOriginalFilename();
            Path rutaCompleta = Paths.get(rutaImagenes + nombreArchivo);
            Files.write(rutaCompleta, imagen.getBytes());
            nuevoProyecto.setImagenUrl("/imagenes/proyectos/" + nombreArchivo);
        }

        proyectoModel proyectoGuardado = proyectoRepository.save(nuevoProyecto);
        
        // Crear objeto con la información necesaria para el WebSocket
        Map<String, Object> proyectoInfo = new HashMap<>();
        proyectoInfo.put("tipo", "NUEVO_PROYECTO");
        proyectoInfo.put("proyecto", convertirProyectoAMap(proyectoGuardado));
        
        // Enviar actualización por WebSocket
        messagingTemplate.convertAndSend("/topic/proyectos", proyectoInfo);

        return proyectoGuardado;
    }

    private Map<String, Object> convertirProyectoAMap(proyectoModel proyecto) {
        Map<String, Object> proyectoMap = new HashMap<>();
        proyectoMap.put("id", proyecto.getId());
        proyectoMap.put("nombre", proyecto.getNombre());
        proyectoMap.put("descripcion", proyecto.getDescripcion());
        proyectoMap.put("imagenUrl", proyecto.getImagenUrl());
        proyectoMap.put("fechaCreacion", proyecto.getFechaCreacion());
        proyectoMap.put("fechaExpiracion", proyecto.getFechaExpiracion());
        proyectoMap.put("numeroParticipantes", proyecto.getNumeroParticipantes());
        proyectoMap.put("estado", proyecto.getEstado());
        proyectoMap.put("isActivo", proyecto.isActivo(LocalDateTime.now()));
        return proyectoMap;
    }

    /**
     * Recupera todos los proyectos.
     * @return Una lista de todos los proyectos.
     */
    public List<proyectoModel> getAllProyectos() {
        return proyectoRepository.findAll();
    }

    /**
     * Recupera un proyecto por su ID.
     * @param id El ID del proyecto a recuperar.
     * @return El proyecto si se encuentra, null en caso contrario.
     */
    public proyectoModel getProyectoById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del proyecto no puede ser nulo");
        }
        return proyectoRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró el proyecto con ID: " + id));
    }

    /**
     * Elimina un proyecto por su ID.
     * @param id El ID del proyecto a eliminar.
     * @return true si el proyecto fue eliminado, false si no se encontró.
     */
    @Transactional
    public boolean deleteProyecto(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del proyecto no puede ser nulo");
        }
        if (!proyectoRepository.existsById(id)) {
            throw new IllegalArgumentException("No se encontró el proyecto con ID: " + id);
        }
        proyectoRepository.deleteById(id);
        return true;
    }

    /**
     * Permite a un usuario participar en un proyecto.
     * @param proyectoId El ID del proyecto.
     * @param usuarioId El ID del usuario.
     * @return true si el usuario se añadió como participante, false en caso contrario.
     */
    @Transactional
    public boolean participarEnProyecto(Long proyectoId, Long usuarioId) {
        if (proyectoId == null || usuarioId == null) {
            throw new IllegalArgumentException("El ID del proyecto y del usuario no pueden ser nulos");
        }
        
        proyectoModel proyecto = proyectoRepository.findById(proyectoId)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró el proyecto con ID: " + proyectoId));
        UsuarioModel usuario = usuarioService.findById(usuarioId)
            .orElseThrow(() -> new IllegalArgumentException("No se encontró el usuario con ID: " + usuarioId));

        if (proyecto.isExpirado()) {
            throw new IllegalStateException("No se puede participar en un proyecto expirado");
        }
        
        if (proyecto.getParticipantes().contains(usuario)) {
            throw new IllegalStateException("El usuario ya participa en este proyecto");
        }
        
        proyecto.addParticipante(usuario);
        proyectoModel proyectoActualizado = proyectoRepository.save(proyecto);
        
        // Notificar cambio
        notificarCambioParticipacion("ACTUALIZAR_PARTICIPANTES", proyectoActualizado);
        
        boolean participacionExitosa = true;
        
        if (participacionExitosa) {
            usuarioService.verificarYOtorgarMedalla(usuarioId);
        }

        return participacionExitosa;
    }

    /**
     * Cuenta el número de proyectos activos.
     * @return El número de proyectos con estado "Activo".
     */
    public long countActiveProjects() {
        return proyectoRepository.findActiveProjects().size();
    }

    /**
     * Cuenta el número total de proyectos.
     * @return El número total de proyectos en la base de datos.
     */
    public long contarProyectos() {
        return proyectoRepository.count();
    }

    /**
     * Cuenta el número de participaciones de un usuario en proyectos.
     * @param userId El ID del usuario.
     * @return El número de participaciones en proyectos.
     */
    public int countProjectParticipationByUser(Long userId) {
        return proyectoRepository.countProjectParticipationByUserId(userId);
    }

    /**
     * Obtiene detalles de todos los proyectos.
     * @return Una lista de mapas con detalles de cada proyecto.
     */
    public List<Map<String, Object>> getDetallesProyectos() {
        List<proyectoModel> proyectos = proyectoRepository.findAll();
        return proyectos.stream().map(proyecto -> {
            Map<String, Object> detalles = new HashMap<>();
            detalles.put("nombre", proyecto.getNombre());
            detalles.put("participantes", proyecto.getNumeroParticipantes());
            detalles.put("fechaInicio", proyecto.getFechaCreacion());
            detalles.put("estado", proyecto.getEstado());
            return detalles;
        }).collect(Collectors.toList());
    }



    public List<proyectoModel> getActiveProjects() {
        LocalDateTime now = LocalDateTime.now();
        return proyectoRepository.findAll().stream()
            .filter(p -> p.getFechaExpiracion() != null && p.getFechaExpiracion().isAfter(now))
            .filter(p -> "activo".equals(p.getEstado()))
            .collect(Collectors.toList());
    }

    public List<proyectoModel> getExpiredProjects() {
        LocalDateTime now = LocalDateTime.now();
        return proyectoRepository.findAll().stream()
            .filter(p -> p.getFechaExpiracion() != null && p.getFechaExpiracion().isBefore(now))
            .collect(Collectors.toList());
    }

    public proyectoModel updateProyecto(proyectoModel proyecto) {
        if (proyecto == null) {
            throw new IllegalArgumentException("El proyecto no puede ser nulo");
        }
        if (proyecto.getId() == null) {
            throw new IllegalArgumentException("El ID del proyecto no puede ser nulo");
        }
        if (proyecto.getNombre() == null || proyecto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del proyecto no puede estar vacío");
        }
        if (proyecto.getDescripcion() == null || proyecto.getDescripcion().trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción del proyecto no puede estar vacía");
        }
        if (!proyectoRepository.existsById(proyecto.getId())) {
            throw new IllegalArgumentException("No se encontró el proyecto con ID: " + proyecto.getId());
        }
        return proyectoRepository.save(proyecto);
    }

    public long countProyectosActivos() {
        return proyectoRepository.countByEstado("activo");
    }
    /**
     * Elimina la participación de un usuario en un proyecto específico.
     * @param proyectoId El ID del proyecto.
     * @param usuarioId El ID del usuario.
     * @return El número de registros afectados (debería ser 1 si la operación fue exitosa).
     */
    @Transactional
    public int eliminarParticipacionUsuario(Long proyectoId, Long usuarioId) {
        if (proyectoId == null || usuarioId == null) {
            throw new IllegalArgumentException("El ID del proyecto y del usuario no pueden ser nulos");
        }
        
        proyectoModel proyecto = proyectoRepository.findById(proyectoId)
            .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado con ID: " + proyectoId));
        UsuarioModel usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + usuarioId));
        
        if (!proyecto.getParticipantes().contains(usuario)) {
            throw new IllegalStateException("El usuario no participa en este proyecto");
        }
        
        proyecto.getParticipantes().remove(usuario);
        proyectoModel proyectoActualizado = proyectoRepository.save(proyecto);
        
        // Notificar cambio
        notificarCambioParticipacion("ACTUALIZAR_PARTICIPANTES", proyectoActualizado);
        
        return 1;
    }

    @SuppressWarnings("unused")
    private proyectoModel ensureValidState(proyectoModel proyecto) {
        if (proyecto.getEstado() == null || proyecto.getEstado().isEmpty()) {
            proyecto.setEstado("activo");
        }
        return proyecto;
    }


    @Transactional
    public void eliminarParticipacionesDeUsuario(Long usuarioId) {
        List<proyectoModel> proyectos = proyectoRepository.findAll();
        for (proyectoModel proyecto : proyectos) {
            proyecto.getParticipantes().removeIf(u -> u.getId().equals(usuarioId));
            proyectoRepository.save(proyecto);
        }
    }

    @Transactional
    public proyectoModel editarProyecto(Long id, String nombre, String descripcion, MultipartFile imagen) throws IOException {
        proyectoModel proyecto = getProyectoById(id);
        if (proyecto == null) {
            throw new IllegalArgumentException("No se encontró el proyecto con ID: " + id);
        }

        proyecto.setNombre(nombre);
        proyecto.setDescripcion(descripcion);

        if (imagen != null && !imagen.isEmpty()) {
            String rutaImagenes = env.getProperty("app.upload.dir", "src/main/resources/static/imagenes/proyectos/");
            String nombreArchivo = System.currentTimeMillis() + "_" + imagen.getOriginalFilename();
            Path rutaCompleta = Paths.get(rutaImagenes + nombreArchivo);
            Files.write(rutaCompleta, imagen.getBytes());
            proyecto.setImagenUrl("/imagenes/proyectos/" + nombreArchivo);
        }

        return updateProyecto(proyecto);
    }

    public Set<Long> obtenerProyectosParticipados(Long usuarioId) {
        return proyectoRepository.findProyectoIdsByParticipanteId(usuarioId);
    }

    public boolean haParticipadoEnProyectoDesdeFecha(Long usuarioId, LocalDateTime fecha) {
        return proyectoRepository.existsByParticipantesIdAndFechaCreacionAfter(usuarioId, fecha);
    }

    public int contarProyectosParticipados(Long usuarioId) {
        return proyectoRepository.countProjectParticipationByUserId(usuarioId);
    }

    /**
     * Verifica si un usuario está participando en un proyecto específico
     */
    public boolean isUserParticipatingInProject(Long usuarioId, Long proyectoId) {
        proyectoModel proyecto = proyectoRepository.findById(proyectoId)
            .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));
            
        return proyecto.getParticipantes().stream()
            .anyMatch(usuario -> usuario.getId().equals(usuarioId));
    }

    /**
     * Obtiene todos los proyectos activos para el selector de desafíos
     */
    public List<proyectoModel> getProyectosActivos() {
        return proyectoRepository.findAll().stream()
            .filter(p -> "activo".equals(p.getEstado()))
            .collect(Collectors.toList());
    }

    public boolean hasUserCompletedMinimumParticipation(Long usuarioId, Long proyectoId) {
        proyectoModel proyecto = proyectoRepository.findById(proyectoId)
            .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        // Just verify user exists in project
        if (!proyecto.getParticipantes().stream()
                .anyMatch(u -> u.getId().equals(usuarioId))) {
            throw new RuntimeException("Usuario no encontrado en el proyecto");
        }

        // Verificar tiempo mínimo de participación (por ejemplo, 7 días)
        LocalDateTime fechaUnion = proyecto.getFechaCreacion();
        LocalDateTime ahora = LocalDateTime.now();
        long diasParticipando = ChronoUnit.DAYS.between(fechaUnion, ahora);

        if (diasParticipando < 7) {
            return false;
        }

        // Verificar participación activa
        boolean hasCompletedChallenges = proyecto.getDesafios().stream()
            .flatMap(desafio -> desafio.getUsuariosCompletados().stream())
            .anyMatch(u -> u.getId().equals(usuarioId));

        return hasCompletedChallenges;
    }

    public List<proyectoModel> findMostParticipatedProjects(PageRequest pageRequest) {
        return proyectoRepository.findAllByOrderByParticipantesDesc(pageRequest);
    }

    private void notificarCambioParticipacion(String tipo, proyectoModel proyecto) {
        Map<String, Object> mensaje = new HashMap<>();
        mensaje.put("tipo", tipo);
        mensaje.put("proyecto", convertirProyectoAMap(proyecto));
        messagingTemplate.convertAndSend("/topic/proyectos", mensaje);
    }
}

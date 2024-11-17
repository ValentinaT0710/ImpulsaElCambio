package com.impulsaElCambio.Controller;

import com.impulsaElCambio.Model.proyectoModel;
import com.impulsaElCambio.Model.Foro_post;
import com.impulsaElCambio.Model.UsuarioModel;
import com.impulsaElCambio.Servicios.ProyectoService;
import com.impulsaElCambio.Servicios.UsuarioService;
import com.impulsaElCambio.Servicios.ForoService;



import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.format.annotation.DateTimeFormat;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Controlador para la gestión de funciones administrativas.
 * Maneja las operaciones relacionadas con proyectos, usuarios y foro desde la
 * perspectiva del administrador.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final ProyectoService proyectoService;
    private final UsuarioService usuarioService;
    private final ForoService foroService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public AdminController(ProyectoService proyectoService, UsuarioService usuarioService, ForoService foroService) {
        this.proyectoService = proyectoService;
        this.usuarioService = usuarioService;
        this.foroService = foroService;
    }

    // Método privado para verificar si el usuario es administrador
    private void verificarAdmin(HttpSession session) throws Exception {
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
        if (isAdmin == null || !isAdmin) {
            throw new Exception("Acceso denegado. Se requieren privilegios de administrador.");
        }
    }

    /**
     * Muestra la página de inicio del administrador con estadísticas básicas.
     * 
     * @param session Sesión HTTP para verificar la autenticación del administrador.
     * @param model   Modelo para pasar datos a la vista.
     * @return Vista de inicio del administrador o redirección al inicio de sesión.
     * @throws Exception 
     */
    @GetMapping("/inicio")
    public String adminInicio(HttpSession session, Model model) throws Exception {
        verificarAdmin(session);
        // Agregar estadísticas básicas para la página de inicio
        model.addAttribute("proyectosActivos", proyectoService.countActiveProjects());
        model.addAttribute("voluntariosActivos", usuarioService.countActiveVolunteers());
        model.addAttribute("nuevasPublicaciones", foroService.countRecentPosts(7)); // Publicaciones en los últimos 7
                                                                                    // días
        model.addAttribute("totalVoluntarios", usuarioService.countAllUsers());
        return "AdminInicio";
    }

    /**
     * Muestra la lista de proyectos para el administrador.
     * 
     * @param model Modelo para pasar datos a la vista.
     * @return Vista de proyectos para el administrador.
     * @throws Exception 
     */
    @GetMapping("/proyectos")
    public String vistaProyectosAdmin(HttpSession session, Model model) throws Exception {
        verificarAdmin(session);
        
        model.addAttribute("proyectosActivos", proyectoService.getActiveProjects());
        model.addAttribute("proyectosExpirados", proyectoService.getExpiredProjects());
        return "VistaProyectosAdmin";
    }

    @GetMapping("/proyectos/todos")
    @ResponseBody
    public Map<String, List<proyectoModel>> getAllProyectos(HttpSession session) throws Exception {
        verificarAdmin(session);
        Map<String, List<proyectoModel>> proyectos = new HashMap<>();
        proyectos.put("activos", proyectoService.getActiveProjects());
        proyectos.put("expirados", proyectoService.getExpiredProjects());
        return proyectos;
    }

    /**
     * Crea un nuevo proyecto.
     * 
     * @param nombre      Nombre del proyecto.
     * @param descripcion Descripción del proyecto.
     * @param imagen      Imagen del proyecto.
     * @return ResponseEntity con el proyecto creado o error.
     * @throws Exception 
     */
    @PostMapping("/proyectos/crear")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> crearProyecto(
            HttpSession session,
            @RequestParam("nombre") String nombre,
            @RequestParam("descripcion") String descripcion,
            @RequestParam("fechaExpiracion") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaExpiracion,
            @RequestParam(value = "imagen", required = false) MultipartFile imagen) throws Exception {
        
        verificarAdmin(session);
        Map<String, Object> response = new HashMap<>();
        
        try {
            proyectoModel proyectoCreado = proyectoService.crearProyecto(nombre, descripcion, fechaExpiracion, imagen);
            
            // Preparar respuesta
            response.put("success", true);
            response.put("message", "Proyecto creado con éxito");
            response.put("proyecto", proyectoCreado);
            
            // Enviar actualización WebSocket
            Map<String, Object> wsMessage = new HashMap<>();
            wsMessage.put("tipo", "NUEVO_PROYECTO");
            wsMessage.put("proyecto", proyectoCreado);
            messagingTemplate.convertAndSend("/topic/proyectos", wsMessage);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al crear el proyecto: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Obtiene un proyecto específico por su ID.
     * 
     * @param id ID del proyecto.
     * @return ResponseEntity con el proyecto o notFound si no existe.
     */
    @GetMapping("/proyectos/{id}")
    @ResponseBody
    public ResponseEntity<proyectoModel> getProyecto(@PathVariable Long id) {
        proyectoModel proyecto = proyectoService.getProyectoById(id);
        if (proyecto != null) {
            return ResponseEntity.ok(proyecto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Edita un proyecto existente.
     * 
     * @param id          ID del proyecto a editar.
     * @param nombre      Nuevo nombre del proyecto.
     * @param descripcion Nueva descripción del proyecto.
     * @param imagen      Nueva imagen del proyecto (opcional).
     * @return ResponseEntity con el proyecto actualizado o error.
     */
    @PutMapping("/proyectos/editar/{id}")
    @ResponseBody
    public ResponseEntity<proyectoModel> editarProyecto(
            @PathVariable Long id,
            @RequestParam("nombre") String nombre,
            @RequestParam("descripcion") String descripcion,
            @RequestParam(value = "imagen", required = false) MultipartFile imagen) {
        try {
            proyectoModel proyecto = proyectoService.getProyectoById(id);
            if (proyecto == null) {
                return ResponseEntity.notFound().build();
            }

            proyecto.setNombre(nombre);
            proyecto.setDescripcion(descripcion);

            if (imagen != null && !imagen.isEmpty()) {
                String rutaImagenes = "src/main/resources/static/imagenes/proyectos/";
                String nombreArchivo = System.currentTimeMillis() + "_" + imagen.getOriginalFilename();
                Path rutaCompleta = Paths.get(rutaImagenes + nombreArchivo);
                Files.write(rutaCompleta, imagen.getBytes());
                proyecto.setImagenUrl("/imagenes/proyectos/" + nombreArchivo);
            }

            proyectoModel proyectoActualizado = proyectoService.updateProyecto(proyecto);
            return ResponseEntity.ok(proyectoActualizado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Elimina un proyecto específico.
     * 
     * @param id ID del proyecto a eliminar.
     * @return ResponseEntity con mensaje de éxito o error.
     */
  
    @DeleteMapping("/proyectos/eliminar/{id}")
    @ResponseBody
    public ResponseEntity<String> eliminarProyecto(@PathVariable Long id) {
        try {
            proyectoService.deleteProyecto(id);
            
            // Enviar actualización a través del WebSocket
            Map<String, Object> mensaje = new HashMap<>();
            mensaje.put("accion", "ELIMINAR");
            mensaje.put("proyectoId", id);
            messagingTemplate.convertAndSend("/topic/proyectos", mensaje);
            
            return ResponseEntity.ok("Proyecto eliminado con éxito");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body("Error al eliminar el proyecto: " + e.getMessage());
        }
    }

    @PostMapping("/proyectos/{id}/participar")
    @ResponseBody
    public ResponseEntity<?> participarProyecto(@PathVariable Long id, HttpSession session) {
        try {
            Long usuarioId = (Long) session.getAttribute("usuarioId");
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
            }
            proyectoService.participarEnProyecto(id, usuarioId);
            return ResponseEntity.ok().body("Participación en el proyecto registrada exitosamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al registrar la participación en el proyecto: " + e.getMessage());
        }
    }

    /**
     * Muestra la vista del foro para el administrador.
     * 
     * @param model Modelo para pasar datos a la vista.
     * @return Vista del foro para el administrador.
     */
    @GetMapping("/foroAdmin")
    public String vistaForoAdmin(Model model) {
        List<Foro_post> comentarios = foroService.getAllComentarios();
        model.addAttribute("comentarios", comentarios);
        // Agregar estadísticas del foro
        model.addAttribute("totalComentarios", foroService.countAllPosts());
        model.addAttribute("comentariosRecientes", foroService.countRecentPosts(7));
        return "ForoAdmin";
    }

    /**
     * Borra un comentario del foro.
     * 
     * @param id      ID del comentario a borrar.
     * @param session Sesión HTTP para verificar la autenticación del administrador.
     * @return ResponseEntity con mensaje de éxito o error.
     */
    @DeleteMapping("/foroAdmin/borrarComentario/{id}")
    @ResponseBody
    public ResponseEntity<?> borrarComentario(@PathVariable Long id, HttpSession session) {
        // Consider moving session validation to a separate method or interceptor
        // to avoid repeating this code in multiple controller methods
        
        // Use more specific exception types instead of catching general Exception
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");

        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        if (isAdmin == null || !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acceso denegado");
        }

        try {
            String resultado = foroService.borrarComentario(id, usuarioId);
            if ("Comentario eliminado exitosamente".equals(resultado)) {
                return ResponseEntity.ok().body(resultado);
            } else {
                return ResponseEntity.badRequest().body(resultado);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Argumento inválido al borrar comentario: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al borrar comentario: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error inesperado al borrar el comentario");
        }
    }

    /**
     * Edita un comentario del foro.
     * 
     * @param id             ID del comentario a editar.
     * @param nuevoContenido Nuevo contenido del comentario.
     * @param session        Sesión HTTP para verificar la autenticación del
     *                       administrador.
     * @return ResponseEntity con el comentario editado o notFound si no existe.
     */
    @PutMapping("/editarComentario/{id}")
    @ResponseBody
    public ResponseEntity<?> editarComentario(@PathVariable Long id, @RequestBody String nuevoContenido,
            HttpSession session) {
        // Similar improvements as in borrarComentario method
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");

        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        if (isAdmin == null || !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acceso denegado");
        }

        try {
            nuevoContenido = nuevoContenido.trim();
            Foro_post comentarioEditado = foroService.editarComentario(id, nuevoContenido, usuarioId);
            if (comentarioEditado != null) {
                Map<String, Object> response = crearRespuestaComentario(comentarioEditado);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Argumento inválido al editar comentario: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado al editar comentario: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error inesperado al editar el comentario");
        }
    }

    // Consider moving this to a separate DTO or mapper class
    private Map<String, Object> crearRespuestaComentario(Foro_post comentario) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", comentario.getId());
        response.put("contenido", comentario.getContenido());
        response.put("fechaCreacion", comentario.getFechaCreacion());
        response.put("usuarioId", comentario.getUsuarioId());
        response.put("usuarioNombre", comentario.getUsuarioNombre());
        return response;
    }

    /**
     * Muestra las estadísticas generales para el administrador.
     * 
     * @param model Modelo para pasar datos a la vista.
     * @return Vista de estadísticas.
     */
    @GetMapping("/Estadisticas")
    public String mostrarEstadisticas(Model model) {
        agregarEstadisticasGenerales(model);
        agregarEstadisticasEmpresas(model);
        agregarEstadisticasProyectos(model);
        agregarEstadisticasForo(model);
        return "Estadisticas";
    }

    private void agregarEstadisticasGenerales(Model model) {
        model.addAttribute("usuariosRegistrados", usuarioService.contarUsuarios());
        model.addAttribute("voluntariosActivos", usuarioService.countActiveVolunteers());
        model.addAttribute("promedioParticipacionEmpresa", usuarioService.getPromedioParticipacionPorEmpresa());
        model.addAttribute("empresaConMasUsuarios", usuarioService.getEmpresaConMasUsuarios());
    }

    private void agregarEstadisticasEmpresas(Model model) {
        List<Map<String, Object>> empresasConMasVoluntarios = usuarioService.obtenerEmpresasConMasVoluntariosEnProyectos(5);
        logger.info("empresasConMasVoluntarios: {}", empresasConMasVoluntarios);
        model.addAttribute("empresasConMasVoluntarios", empresasConMasVoluntarios);
        model.addAttribute("desgloseEmpresas", usuarioService.getDesgloseParticipacionEmpresarial());
    }

    private static final String ESTADO_ACTIVO = "Activo";
    private static final String ESTADO_EXPIRADO = "Expirado";

    private void agregarEstadisticasProyectos(Model model) {
        List<proyectoModel> todosProyectos = proyectoService.getAllProyectos();
        LocalDateTime now = LocalDateTime.now();

        Map<Boolean, List<proyectoModel>> proyectosPorEstado = clasificarProyectosPorEstado(todosProyectos, now);
        List<proyectoModel> proyectosActivos = proyectosPorEstado.get(true);
        List<proyectoModel> proyectosExpirados = proyectosPorEstado.get(false);

        model.addAttribute("proyectosActivos", proyectosActivos.size());
        model.addAttribute("proyectosExpirados", proyectosExpirados.size());
        model.addAttribute("voluntariosAsignados", contarVoluntariosAsignados(proyectosActivos));

        List<Map<String, Object>> detallesProyectos = crearDetallesProyectos(todosProyectos, now);
        model.addAttribute("detallesProyectos", detallesProyectos);
    }

    private Map<Boolean, List<proyectoModel>> clasificarProyectosPorEstado(List<proyectoModel> proyectos, LocalDateTime now) {
        return proyectos.stream()
            .collect(Collectors.partitioningBy(proyecto -> proyecto.getFechaExpiracion() == null || proyecto.getFechaExpiracion().isAfter(now)));
    }

    private long contarVoluntariosAsignados(List<proyectoModel> proyectos) {
        return proyectos.stream()
            .mapToLong(proyecto -> proyecto.getParticipantes().size())
            .sum();
    }

    private List<Map<String, Object>> crearDetallesProyectos(List<proyectoModel> proyectos, LocalDateTime now) {
        return proyectos.stream()
            .map(proyecto -> {
                Map<String, Object> detalles = new HashMap<>();
                detalles.put("nombre", proyecto.getNombre());
                detalles.put("participantes", proyecto.getParticipantes().size());
                detalles.put("fechaInicio", proyecto.getFechaCreacion());
                detalles.put("estado", determinarEstadoProyecto(proyecto, now));
                return detalles;
            })
            .collect(Collectors.toList());
    }

    private String determinarEstadoProyecto(proyectoModel proyecto, LocalDateTime now) {
        return (proyecto.getFechaExpiracion() == null || proyecto.getFechaExpiracion().isAfter(now)) ? ESTADO_ACTIVO : ESTADO_EXPIRADO;
    }

    private void agregarEstadisticasForo(Model model) {
        model.addAttribute("participacionesForo", foroService.countAllPosts());
        model.addAttribute("ultimasPublicaciones", foroService.getUltimasCincoPublicaciones());
    }

    @GetMapping("/ManejoUser")
    public String listarUsuarios(Model model) {
        List<UsuarioModel> usuarios = usuarioService.findAll();
        Map<Long, Integer> actividadForo = new HashMap<>();
        Map<Long, Integer> participacionProyectos = new HashMap<>();

        for (UsuarioModel usuario : usuarios) {
            actividadForo.put(usuario.getId(), foroService.countPostsByUser(usuario.getId()));
            participacionProyectos.put(usuario.getId(), proyectoService.countProjectParticipationByUser(usuario.getId()));
        }

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("actividadForo", actividadForo);
        model.addAttribute("participacionProyectos", participacionProyectos);

        return "ManejoUser";
    }

    @PostMapping("/ManejoUser/eliminarCuenta/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> eliminarCuenta(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean eliminado = usuarioService.eliminarCuenta(id);
            if (eliminado) {
                response.put("success", true);
                response.put("message", "Cuenta eliminada con éxito");
            } else {
                response.put("success", false);
                response.put("message", "No se encontró la cuenta");
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al eliminar la cuenta: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    /**
     * Cierra la sesión del administrador.
     * 
     * @param session Sesión HTTP actual.
     * @return Redirección a la página de inicio.
     * @throws Exception 
     */
    @GetMapping("/cerrarSesion")
    public String cerrarSesion(HttpSession session) throws Exception {
        verificarAdmin(session);
        session.invalidate();
        return "redirect:/Inicio";
    }

    

}

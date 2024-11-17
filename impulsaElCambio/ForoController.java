package com.impulsaElCambio.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.impulsaElCambio.Model.Foro_post;
import com.impulsaElCambio.Model.UsuarioModel;
import com.impulsaElCambio.Servicios.ForoService;
import com.impulsaElCambio.Servicios.UsuarioService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Controlador para manejar las operaciones relacionadas con el foro.
 */
@Controller
@RequestMapping("/foro")
public class ForoController {
    private static final Logger logger = LoggerFactory.getLogger(ForoController.class);

    @Autowired
    private ForoService foroService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Muestra la página principal del foro.
     * @param model Modelo para pasar datos a la vista.
     * @param session Sesión HTTP para verificar la autenticación del usuario.
     * @return La vista del foro o redirección a inicio de sesión si no está autenticado.
     */
    @GetMapping
    public String mostrarForo(Model model, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/inicioSesion";
        }

        // Obtener el usuario actual
        UsuarioModel usuarioActual = usuarioService.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<Foro_post> comentarios = foroService.getAllComentarios();
        model.addAttribute("comentarios", comentarios);
        model.addAttribute("usuarioId", usuarioId);
        model.addAttribute("isAdmin", session.getAttribute("isAdmin") != null ? session.getAttribute("isAdmin") : false);
        model.addAttribute("usuarioActual", usuarioActual); // Añadimos el usuario actual al modelo

        return "Foro";
    }

    /**
     * Muestra la página del foro para administradores.
     * @param model Modelo para pasar datos a la vista.
     * @return La vista del foro para administradores.
     */
    @GetMapping("/admin")
    public String mostrarForoAdmin(Model model, HttpSession session) {
        try {
            List<Foro_post> comentarios = foroService.getAllComentarios();
            model.addAttribute("comentarios", comentarios);
            return "ForoAdmin";
        } catch (Exception e) {
            logger.error("Error al acceder al foro admin: {}", e.getMessage());
            return "redirect:/error";
        }
    }

    /**
     * Publica un nuevo comentario en el foro.
     * @param payload Contenido del comentario.
     * @param session Sesión HTTP para obtener el ID del usuario.
     * @return ResponseEntity con el nuevo comentario o un mensaje de error.
     */
    @PostMapping("/publicar")
    @ResponseBody
    public ResponseEntity<?> publicarComentario(@RequestBody Map<String, String> payload, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        
        try {
            String contenido = payload.get("contenido");
            if (contenido == null || contenido.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("El contenido no puede estar vacío");
            }
            
            Foro_post nuevoComentario = foroService.crearComentario(contenido, usuarioId);
            UsuarioModel usuario = nuevoComentario.getUsuario();
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", nuevoComentario.getId());
            response.put("contenido", nuevoComentario.getContenido());
            response.put("fechaCreacion", nuevoComentario.getFechaCreacion());
            response.put("comentarioPadreId", null);
            response.put("usuario", new HashMap<String, Object>() {{
                put("id", usuario.getId());
                put("nombre", usuario.getNombre());
                put("imagenPerfil", usuario.getImagenPerfil() != null ? usuario.getImagenPerfil() : "/Imagenes/ImagenPredeterminada.jpg");
            }});
            
            messagingTemplate.convertAndSend("/topic/comentarios", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al crear comentario: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear el comentario: " + e.getMessage()));
        }
    }

    /**
     * Borra un comentario del foro.
     * @param id ID del comentario a borrar.
     * @param session Sesión HTTP para verificar el usuario.
     * @return ResponseEntity indicando el éxito o fracaso de la operación.
     */
    @DeleteMapping("/borrarComentario/{id}")
    @ResponseBody
    public ResponseEntity<?> borrarComentario(@PathVariable Long id, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        try {
            String borrado = foroService.borrarComentario(id, usuarioId);
            if (borrado.equals("Comentario eliminado exitosamente")) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.badRequest().body(borrado);
            }
        } catch (Exception e) {
            logger.error("Error al borrar comentario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al borrar el comentario");
        }
    }

    /**
     * Edita un comentario existente en el foro.
     * @param id ID del comentario a editar.
     * @param nuevoContenido Nuevo contenido del comentario.
     * @param session Sesión HTTP para verificar el usuario.
     * @return ResponseEntity con el comentario editado o un error si falla.
     */
    @PutMapping("/editarComentario/{id}")
    @ResponseBody
    public ResponseEntity<?> editarComentario(@PathVariable Long id, @RequestBody String nuevoContenido, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }

        try {
            Foro_post comentarioEditado = foroService.editarComentario(id, nuevoContenido, usuarioId);
            if (comentarioEditado != null) {
                return ResponseEntity.ok().body("Comentario editado exitosamente");
            } else {
                return ResponseEntity.badRequest().body("No tienes permiso para editar este comentario");
            }
        } catch (Exception e) {
            logger.error("Error al editar comentario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al editar el comentario");
        }
    }

    @PostMapping("/me-encanta/{comentarioId}")
    @ResponseBody
    public ResponseEntity<?> toggleMeEncanta(@PathVariable Long comentarioId, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        Map<String, Object> resultado = foroService.toggleMeEncanta(comentarioId, usuarioId);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/responder")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> responderComentario(@RequestBody Map<String, String> payload, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            Long comentarioPadreId = Long.parseLong(payload.get("comentarioPadreId"));
            String contenido = payload.get("contenido");
            
            Foro_post respuesta = foroService.crearRespuesta(contenido, usuarioId, comentarioPadreId);
            UsuarioModel usuario = respuesta.getUsuario();
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", respuesta.getId());
            response.put("contenido", respuesta.getContenido());
            response.put("fechaCreacion", respuesta.getFechaCreacion());
            response.put("comentarioPadreId", comentarioPadreId);
            response.put("usuario", Map.of(
                "id", usuario.getId(),
                "nombre", usuario.getNombre(),
                "imagenPerfil", usuario.getImagenPerfil() != null ? usuario.getImagenPerfil() : "/Imagenes/ImagenPredeterminada.jpg"
            ));
            
            // Enviar la nueva respuesta a todos los clientes conectados
            messagingTemplate.convertAndSend("/topic/comentarios", response);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al crear respuesta: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al crear la respuesta: " + e.getMessage()));
        }
    }

    @DeleteMapping("/eliminarComentario/{id}")
    @ResponseBody
    public ResponseEntity<?> eliminarComentario(@PathVariable Long id, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        
        try {
            boolean eliminado = foroService.eliminarComentario(id, usuarioId);
            if (eliminado) {
                return ResponseEntity.ok().build();
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("No tienes permiso para eliminar este comentario");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error al eliminar el comentario: " + e.getMessage());
        }
    }

    @GetMapping("/cargarMas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cargarMasComentarios(
        @RequestParam int pagina, 
        @RequestParam int cantidad) {
        try {
            Pageable pageable = PageRequest.of(pagina, cantidad, Sort.by("fechaCreacion").descending());
            List<Foro_post> comentarios = foroService.findMainComments(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("comentarios", comentarios);
            response.put("pagina", pagina);
            response.put("cantidad", comentarios.size());
            response.put("hayMas", comentarios.size() >= cantidad);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error al cargar más comentarios: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al cargar más comentarios"));
        }
    }
}

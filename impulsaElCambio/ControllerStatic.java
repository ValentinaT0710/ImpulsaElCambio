package com.impulsaElCambio.Controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;


import com.impulsaElCambio.Servicios.ForoService;
import com.impulsaElCambio.Servicios.UsuarioService;


import jakarta.servlet.http.HttpSession;

import com.impulsaElCambio.Model.proyectoModel;
import com.impulsaElCambio.Model.UsuarioModel;
import com.impulsaElCambio.Repository.Repository;
import com.impulsaElCambio.Servicios.ProyectoService;
import com.impulsaElCambio.Servicios.RankingService;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;



/**
 * Controlador para manejar las estadísticas de la aplicación.
 */
@Controller
public class ControllerStatic {

    
    @SuppressWarnings("unused")
    private final Repository proyectoRepository;
    private final ForoService foroService;
    private final ProyectoService proyectoService;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private RankingService rankingService;

    
    public ControllerStatic( 
                            Repository proyectoRepository, 
                            ForoService foroService, 
                            ProyectoService proyectoService) {
        
        this.proyectoRepository = proyectoRepository;
        this.foroService = foroService;
        this.proyectoService = proyectoService;
    }

    private boolean isAdmin(HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return false;
        }
        Optional<UsuarioModel> usuarioOpt = usuarioService.findById(usuarioId);
        return usuarioOpt.isPresent() && usuarioOpt.get().getRol() == UsuarioModel.Rol.ADMIN;
    }

    /**
     * Maneja la solicitud GET para obtener las estadísticas.
     * 
     * @param model El modelo para agregar atributos que se mostrarán en la vista.
     * @return El nombre de la vista de estadísticas.
     */
    @GetMapping("/estadisticas")
    public String getEstadisticas(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/inicioSesion";
        }
        // Obtiene el total de usuarios registrados
        long totalUsuarios = usuarioService.contarUsuarios();
        
        // Obtiene el número de voluntarios activos
        int voluntariosActivos = usuarioService.contarVoluntariosActivos();
        
        // Obtiene el número de proyectos activos
        long proyectosActivos = proyectoService.countProyectosActivos();
        
        // Agrega los datos al modelo
        model.addAttribute("totalUsuarios", totalUsuarios);
        model.addAttribute("voluntariosActivos", voluntariosActivos);
        model.addAttribute("proyectosActivos", proyectosActivos);
        
        // Obtiene los detalles de los proyectos
        List<Map<String, Object>> detallesProyectos = proyectoService.getDetallesProyectos();
        model.addAttribute("detallesProyectos", detallesProyectos);

        // Retorna el nombre de la vista de estadísticas
        return "estadisticas";
    }
 
    @GetMapping("/Graficas")
    public String mostrarGraficas(HttpSession session, Model model) throws Exception {
        verificarAdmin(session);
        // Obtén los datos reales de tus servicios
        long totalUsuariosRegistrados = usuarioService.contarUsuarios();
        int totalPublicacionesForo = foroService.countAllPosts();
        int totalVoluntariosActivos = usuarioService.contarVoluntariosActivos();
        
        List<proyectoModel> proyectosActivos = proyectoService.getActiveProjects();
        
        List<String> nombresProyectos = new ArrayList<>();
        List<Integer> participantesProyectos = new ArrayList<>();
        
        for (proyectoModel proyecto : proyectosActivos) {
            if ("activo".equals(proyecto.getEstado()) || proyecto.getEstado() == null) {
                nombresProyectos.add(proyecto.getNombre());
                participantesProyectos.add(proyecto.getParticipantes().size());
            }
        }
        
        model.addAttribute("totalUsuariosRegistrados", totalUsuariosRegistrados);
        model.addAttribute("nombresProyectosActivos", nombresProyectos);
        model.addAttribute("participantesProyectosActivos", participantesProyectos);
        model.addAttribute("totalPublicacionesForo", totalPublicacionesForo);
        model.addAttribute("totalVoluntariosActivos", totalVoluntariosActivos);
        
        return "Graficas";
    }

    private void verificarAdmin(HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null || !isAdmin(session)) {
            throw new RuntimeException("Acceso no autorizado. Se requieren permisos de administrador.");
        }
    }

    @GetMapping("/api/estadisticas")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerEstadisticas(HttpSession session) throws Exception {
        verificarAdmin(session);
        Map<String, Object> estadisticas = new HashMap<>();
        
        estadisticas.put("usuariosRegistrados", usuarioService.contarUsuarios());
        estadisticas.put("proyectosActivos", proyectoService.countProyectosActivos());
        estadisticas.put("participacionesForo", foroService.countAllPosts());
        estadisticas.put("voluntariosActivos", usuarioService.contarVoluntariosActivos());

        return ResponseEntity.ok(estadisticas);
    }

    @GetMapping("/PerfilAdmin")
    public String mostrarPerfilAdmin(Model model, HttpSession session) {
        try {
            verificarAdmin(session);
            Long usuarioId = (Long) session.getAttribute("usuarioId");
            Optional<UsuarioModel> usuarioOpt = usuarioService.findById(usuarioId);
            if (usuarioOpt.isEmpty()) {
                return "redirect:/inicioSesion";
            }
            
            UsuarioModel usuario = usuarioOpt.get();
            model.addAttribute("usuario", usuario);
            return "PerfilAdmin";
        } catch (RuntimeException e) {
            // Log the error
            LoggerFactory.getLogger(ControllerStatic.class).error("Error al acceder al perfil de administrador: {}", e.getMessage(), e);
            model.addAttribute("error", "Ha ocurrido un error al acceder al perfil de administrador: " + e.getMessage());
            return "error";
        }
    }

   @SuppressWarnings("null")
@PostMapping("/actualizarPerfil")
public String actualizarPerfil(
                               @RequestParam("nombre") String nombre,
                               @RequestParam("correo") String correo,
                               @RequestParam(value = "imagen", required = false) MultipartFile imagen,
                               @RequestParam(value = "passwordActual", required = false) String passwordActual,
                               @RequestParam(value = "nuevaPassword", required = false) String nuevaPassword,
                               @RequestParam("numero") String numero,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
    if (!isAdmin(session)) {
        return "redirect:/inicioSesion";
    }
    Long usuarioId = (Long) session.getAttribute("usuarioId");
    if (usuarioId == null) {
        return "redirect:/inicioSesion";
    }

    try {
        Optional<UsuarioModel> usuarioOpt = usuarioService.findById(usuarioId);
        if (usuarioOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado");
            return "redirect:/PerfilAdmin";
        }

        UsuarioModel usuarioActual = usuarioOpt.get();
        
        // Verificar la contraseña actual antes de realizar cambios
        if (passwordActual != null && !passwordActual.isEmpty()) {
            if (!usuarioActual.getPassword().equals(passwordActual)) {
                redirectAttributes.addFlashAttribute("error", "La contraseña actual es incorrecta");
                return "redirect:/PerfilAdmin";
            }
            
            // Actualizar la contraseña si se proporciona una nueva
            if (nuevaPassword != null && !nuevaPassword.isEmpty()) {
                usuarioActual.setPassword(nuevaPassword);
            }
        }

        usuarioActual.setNombre(nombre);
        usuarioActual.setNumero(numero);
        
        if (imagen != null && !imagen.isEmpty()) {
            String originalFilename = imagen.getOriginalFilename();
            String fileName = org.springframework.util.StringUtils.hasText(originalFilename)
                ? originalFilename.replaceAll("[^a-zA-Z0-9.-]", "_")
                : "default.jpg";
            String uploadDir = "src/main/resources/static/imagenes/perfiles/";
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);

            if (!java.nio.file.Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            try (InputStream inputStream = imagen.getInputStream()) {
                java.nio.file.Path filePath = uploadPath.resolve(fileName);
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
                usuarioActual.setImagenPerfil("/imagenes/perfiles/" + fileName);
            }
        }

        usuarioService.saveUsuario(usuarioActual);
        
        // Actualizar la sesión con el usuario actualizado
        session.setAttribute("usuario", usuarioActual);
        
        redirectAttributes.addFlashAttribute("mensaje", "Perfil actualizado con éxito");
    } catch (IOException e) {
        LoggerFactory.getLogger(ControllerStatic.class).error("Error al guardar la imagen de perfil", e);
        redirectAttributes.addFlashAttribute("error", "Error al guardar la imagen de perfil: " + e.getMessage());
    } catch (Exception e) {
        LoggerFactory.getLogger(ControllerStatic.class).error("Error al actualizar el perfil", e);
        redirectAttributes.addFlashAttribute("error", "Error al actualizar el perfil: " + e.getMessage());
    }

    return "redirect:/PerfilAdmin";
}


@GetMapping("/ranking")
public String mostrarRanking(Model model, HttpSession session) {
    Long usuarioId = (Long) session.getAttribute("usuarioId");
    Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");

    if (usuarioId == null) {
        return "redirect:/inicioSesion";
    }

    try {
        if (isAdmin != null && isAdmin) {
            verificarAdmin(session);
        }

        Map<String, Object> estadisticas = rankingService.obtenerEstadisticasRanking();
        model.addAttribute("isAdmin", isAdmin);
        
        List<?> rawList = (List<?>) estadisticas.get("topUsuarios");
        List<UsuarioModel> topUsuarios = rawList.stream()
            .filter(obj -> obj instanceof UsuarioModel)
            .map(obj -> (UsuarioModel) obj)
            .collect(Collectors.toList());
        List<Map<String, Object>> rankingData = topUsuarios.stream().map(usuario -> {
        Map<String, Object> userData = new HashMap<>();
        userData.put("nombre", usuario.getNombre());
        userData.put("puntos", usuario.getPuntos());
        userData.put("insignias", usuario.getInsignias());
        userData.put("proyectosCompletados", rankingService.obtenerProyectosCompletados(usuario));
        return userData;
    }).collect(Collectors.toList());
    
    model.addAttribute("topUsuarios", rankingData);
        return "ranking";
    } catch (Exception e) {
        LoggerFactory.getLogger(ControllerStatic.class).error("Error al mostrar el ranking", e);
        return "redirect:/error?message=ErrorAlMostrarRanking";
    }
}

@GetMapping("/api/estadisticas/proyectos-activos")
@ResponseBody
public ResponseEntity<Map<String, Object>> obtenerProyectosActivos(HttpSession session) {
    try {
        verificarAdmin(session);
        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("proyectosActivos", proyectoService.countProyectosActivos());
        return ResponseEntity.ok(estadisticas);
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                           .body(Collections.singletonMap("error", e.getMessage()));
    }
}

} // Add this closing brace

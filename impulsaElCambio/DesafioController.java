package com.impulsaElCambio.Controller;

import com.impulsaElCambio.Model.DesafioModel;
import com.impulsaElCambio.Model.UsuarioModel;
import com.impulsaElCambio.Servicios.DesafioService;
import com.impulsaElCambio.Servicios.UsuarioService;
import com.impulsaElCambio.Servicios.ProyectoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Controller
public class DesafioController {

    private static final Logger logger = LoggerFactory.getLogger(DesafioController.class);

    @Autowired
    private DesafioService desafioService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProyectoService proyectoService;

    // Vista de administrador para crear desafíos
    @GetMapping("/admin/desafios/crear")
    public String mostrarFormularioCrearDesafio(Model model) {
        model.addAttribute("desafio", new DesafioModel());
        model.addAttribute("proyectos", proyectoService.getProyectosActivos());
        return "crearDesafio";
    }

    @PostMapping("/admin/desafios/crear")
    public String crearDesafio(@ModelAttribute DesafioModel desafio) {
        logger.info("Creando desafío: Tipo={}, FechaInicio={}, FechaFin={}", 
            desafio.getTipoDesafio(),
            desafio.getFechaInicio(),
            desafio.getFechaFin());

        // Validar fechas
        LocalDateTime ahora = LocalDateTime.now();
        if (desafio.getFechaInicio() == null) {
            desafio.setFechaInicio(ahora);
        }
        if (desafio.getFechaFin() == null || desafio.getFechaFin().isBefore(ahora)) {
            desafio.setFechaFin(ahora.plusDays(7)); // Por defecto, una semana
        }

        // Validar que si es tipo UNIRSE_PROYECTO, tenga un proyectoId
        if (desafio.getTipoDesafio() == DesafioModel.TipoDesafio.UNIRSE_PROYECTO 
            && desafio.getProyectoId() == null) {
            return "redirect:/admin/desafios/crear?error=Debe seleccionar un proyecto";
        }
        
        desafioService.crearDesafio(desafio);
        return "redirect:/admin/desafios";
    }

    // Vista de usuario para ver y participar en desafíos
    @GetMapping("/DesafioVoluntario")
    public String mostrarDesafiosVoluntario(Model model, HttpSession session) {
        // Verificar si el usuario está autenticado
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/inicioSesion";
        }

        // Obtener el usuario actual
        UsuarioModel usuario = usuarioService.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        List<DesafioModel> desafiosActivos = desafioService.obtenerDesafiosActivos();
        logger.info("Desafíos activos encontrados: {}", desafiosActivos.size());
        for (DesafioModel desafio : desafiosActivos) {
            logger.info("Desafío: ID={}, Tipo={}, Activo={}, FechaInicio={}, FechaFin={}", 
                desafio.getId(), 
                desafio.getTipoDesafio(), 
                desafio.isActivo(),
                desafio.getFechaInicio(),
                desafio.getFechaFin());
        }

        // Crear un mapa de proyectos para los desafíos de tipo UNIRSE_PROYECTO
        Map<Long, String> nombresProyectos = new HashMap<>();
        for (DesafioModel desafio : desafiosActivos) {
            if (desafio.getTipoDesafio() == DesafioModel.TipoDesafio.UNIRSE_PROYECTO 
                && desafio.getProyectoId() != null) {
                try {
                    String nombreProyecto = proyectoService.getProyectoById(desafio.getProyectoId()).getNombre();
                    nombresProyectos.put(desafio.getProyectoId(), nombreProyecto);
                } catch (Exception e) {
                    nombresProyectos.put(desafio.getProyectoId(), "Proyecto no encontrado");
                }
            }
        }

        // Agregar datos al modelo
        model.addAttribute("desafiosActivos", desafiosActivos);
        model.addAttribute("usuario", usuario);
        model.addAttribute("nombresProyectos", nombresProyectos);
        return "DesafioVoluntario";
    }

    @PostMapping("/desafios/{id}/participar")
    @ResponseBody
    public String participarEnDesafio(@PathVariable Long id, @RequestParam Long usuarioId) {
        return desafioService.participarEnDesafio(id, usuarioId);
    }

    @PostMapping("/desafios/{id}/completar")
    @ResponseBody
    public String completarDesafio(@PathVariable Long id, @RequestParam Long usuarioId) {
        return desafioService.completarDesafio(id, usuarioId);
    }

    @GetMapping("/admin/desafios")
    public String listarDesafios(Model model) {
        try {
            model.addAttribute("desafiosActivos", desafioService.obtenerDesafiosActivos());
            model.addAttribute("desafio", new DesafioModel());
            model.addAttribute("proyectos", proyectoService.getProyectosActivos());
            return "crearDesafio";
        } catch (Exception e) {
            // Log the error
            e.printStackTrace();
            model.addAttribute("error", "Error al cargar los desafíos");
            return "error";  // Create an error template
        }
    }

    @DeleteMapping("/admin/desafios/{id}/eliminar")
    @ResponseBody
    public ResponseEntity<String> eliminarDesafio(@PathVariable Long id) {
        try {
            desafioService.eliminarDesafio(id);
            return ResponseEntity.ok("Desafío eliminado exitosamente");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                               .body("Error al eliminar el desafío: " + e.getMessage());
        }
    }
} 
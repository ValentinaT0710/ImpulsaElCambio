package com.impulsaElCambio.Controller;

import com.impulsaElCambio.Model.DesafioModel;
import com.impulsaElCambio.Model.proyectoModel;
import com.impulsaElCambio.Model.UsuarioModel;
import com.impulsaElCambio.Servicios.UsuarioService;
import com.impulsaElCambio.Servicios.ProyectoService;
import com.impulsaElCambio.Servicios.DesafioService;
import com.impulsaElCambio.Servicios.ForoService;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import org.springframework.data.domain.PageRequest;



@Controller
public class UsuarioController {
    private final UsuarioService usuarioService;
    private final ProyectoService proyectoService;
    private final DesafioService desafioService;
    private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);


    
    public UsuarioController(UsuarioService usuarioService, ProyectoService proyectoService, DesafioService desafioService, ForoService foroService) {
        this.usuarioService = usuarioService;
        this.proyectoService = proyectoService;
        this.desafioService = desafioService;
    }

    // Página de inicio principal
    @GetMapping({"/", "/Inicio"})
    public String mostrarInicio(Model model) {
        try {
            // Obtener solo el proyecto más participado
            PageRequest topProyecto = PageRequest.of(0, 1);
            List<proyectoModel> proyectosDestacados = proyectoService.findMostParticipatedProjects(topProyecto);
            
            // Agregar el proyecto más destacado al modelo
            model.addAttribute("proyectosDestacados", proyectosDestacados);
            
            // Mantener la lógica existente para las estadísticas
            long voluntariosActivos = usuarioService.countActiveVolunteers();
            long proyectosActivos = proyectoService.countActiveProjects();
            long personasBeneficiadas = voluntariosActivos * 3; // o tu lógica actual
            
            model.addAttribute("voluntariosActivos", voluntariosActivos);
            model.addAttribute("proyectosActivos", proyectosActivos);
            model.addAttribute("personasBeneficiadas", personasBeneficiadas);
            
            return "inicio";
        } catch (Exception e) {
            logger.error("Error al cargar la página de inicio: ", e);
            return "error";
        }
    }

    @GetMapping("/proyectos")
    public String mostrarProyectos(Model model, HttpSession session) {
        if (session.getAttribute("usuarioId") == null) {
            return "redirect:/inicioSesion";
        }
        model.addAttribute("proyectosActivos", proyectoService.getActiveProjects());
        model.addAttribute("proyectosExpirados", proyectoService.getExpiredProjects());
        return "proyectos";
    }

    @PostMapping("/proyectos/participar")
    @ResponseBody
    public ResponseEntity<?> participarEnProyecto(@RequestParam Long proyectoId, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
        }
        
        boolean participacionExitosa = proyectoService.participarEnProyecto(proyectoId, usuarioId);
        if (participacionExitosa) {
            return ResponseEntity.ok("Participación registrada con éxito");
        } else {
            return ResponseEntity.badRequest().body("No se pudo registrar la participación");
        }
    }
    @PostMapping("/proyectos/eliminar-participacion")
    @ResponseBody
    public ResponseEntity<?> eliminarParticipacion(@RequestBody Map<String, Long> payload, HttpSession session) {
        try {
            Long usuarioId = (Long) session.getAttribute("usuarioId");
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Usuario no autenticado");
            }
            Long proyectoId = payload.get("proyectoId");
            proyectoService.eliminarParticipacionUsuario(proyectoId, usuarioId);
            return ResponseEntity.ok().body("Participación en el proyecto eliminada exitosamente");
        } catch (Exception e) {
            logger.error("Error al eliminar la participación en el proyecto: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al eliminar la participación en el proyecto: " + e.getMessage());
        }
    }


    @GetMapping("/inicioVoluntario")
    public String inicioVoluntario(Model model, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/inicioSesion";
        }

        try {
            UsuarioModel usuario = usuarioService.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Obtener todos los desafíos activos
            List<DesafioModel> desafiosActivos = desafioService.obtenerDesafiosActivos();
            // Obtener todos los proyectos activos
            List<proyectoModel> proyectosActivos = proyectoService.getActiveProjects();
            
            model.addAttribute("usuario", usuario);
            model.addAttribute("desafiosActivos", desafiosActivos);
            model.addAttribute("proyectosActivos", proyectosActivos);
            
            return "inicioVoluntario";
        } catch (Exception e) {
            logger.error("Error en inicioVoluntario: ", e);
            return "redirect:/error";
        }
    }

    // Mapeos para registro
    @GetMapping("/Registrarse")
    public String mostrarRegistro() {
        return "registrarse";
    }

    @PostMapping("/registrarse")
    public String procesarRegistro(@RequestParam String nombre, 
                                  @RequestParam String correo, 
                                  @RequestParam String numero, 
                                  @RequestParam String password,
                                  @RequestParam String confirm_password,
                                  @RequestParam boolean terms,
                                  @RequestParam String empresa, // Añade este parámetro
                                  Model model) {
        // Nuevas validaciones
        if (!password.equals(confirm_password)) {
            model.addAttribute("error", "Las contraseñas no coinciden");
            return "registrarse";
        }
        
        if (!terms) {
            model.addAttribute("error", "Debes aceptar los términos y condiciones");
            return "registrarse";
        }

        // Verificar si el correo ya está registrado
        if (usuarioService.findByCorreo(correo).isPresent()) {
            model.addAttribute("error", "El correo ya está registrado");
            return "registrarse";
        }

        UsuarioModel nuevoUsuario = new UsuarioModel();
        nuevoUsuario.setNombre(nombre);
        nuevoUsuario.setCorreo(correo);
        nuevoUsuario.setNumero(numero);
        nuevoUsuario.setPassword(password);
        nuevoUsuario.setRol(UsuarioModel.Rol.VOLUNTARIO);
        nuevoUsuario.setEmpresa(empresa); // Establece la empresa ingresada por el usuario
        
        if (empresa == null || empresa.trim().isEmpty()) {
            model.addAttribute("error", "La empresa es obligatoria");
            return "registrarse";
        }

        try {
            usuarioService.saveUsuario(nuevoUsuario);
            return "redirect:/inicioSesion?registroExitoso=true";
        } catch (Exception e) {
            model.addAttribute("error", "Error al registrar el usuario: " + e.getMessage());
            return "registrarse";
        }
    }

    // Mapeos para inicio de sesión
    @GetMapping("/inicioSesion")
    public String mostrarInicioSesion() {
        return "inicioSesion";
    }

    @PostMapping("/inicioSesion")
    public String inicioSesionUsuario(@RequestParam String correo, 
                                     @RequestParam String password, 
                                     Model model,
                                     HttpSession session) {
        // Este método maneja el proceso de inicio de sesión de un usuario
        Optional<UsuarioModel> usuarioOpt = usuarioService.findByCorreo(correo);
        if (usuarioOpt.isPresent()) {
            UsuarioModel usuario = usuarioOpt.get();
            if (usuario.getPassword().equals(password)) {
                // Si las credenciales son correctas, se establecen atributos de sesión
                session.setAttribute("usuarioId", usuario.getId());
                session.setAttribute("usuarioNombre", usuario.getNombre());
                if (usuario.getRol() == UsuarioModel.Rol.ADMIN) {
                    // Si el usuario es un administrador, se establece un atributo adicional
                    session.setAttribute("isAdmin", true);
                    return "redirect:/admin/inicio"; // Redirige a la página de inicio de administrador
                } else {
                    return "redirect:/inicioVoluntario"; // Redirige a la página de inicio de voluntario
                }
            }
        }
        // Si las credenciales son incorrectas, se añade un mensaje de error al modelo
        model.addAttribute("error", "Correo o contraseña incorrectos");
        return "inicioSesion"; // Vuelve a mostrar la página de inicio de sesión con el mensaje de error
    }

    @GetMapping("/Perfil")
    public String mostrarPerfil(Model model, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/inicioSesion";
        }
        
        Optional<UsuarioModel> usuarioOpt = usuarioService.findById(usuarioId);
        if (usuarioOpt.isPresent()) {
            UsuarioModel usuario = usuarioOpt.get();
            // Ensure insignias are loaded
            if (usuario.getInsignias() == null) {
                usuario.setInsignias(""); // Initialize if null
            }
            model.addAttribute("usuario", usuario);
            return "Perfil";
        } else {
            return "redirect:/inicioSesion";
        }
    }

    public String mostrarPerfilUsuario(Model model, HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        if (usuarioId == null) {
            return "redirect:/inicioSesion";
        }
        
        Optional<UsuarioModel> usuario = usuarioService.findById(usuarioId);
        if (usuario.isPresent()) {
            model.addAttribute("usuario", usuario.get());
            return "PerfilUsuario";
        } else {
            return "redirect:/inicioSesion";
        }
    }

    @PostMapping("/Perfil/actualizar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> actualizarPerfilUsuario(@ModelAttribute UsuarioModel usuarioActualizado, 
                                      @RequestParam String passwordActual,
                                      @RequestParam(required = false) String nuevaPassword,
                                      @RequestParam(required = false) MultipartFile imagen,
                                      HttpSession session) {
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        Map<String, Object> response = new HashMap<>();
        
        if (usuarioId == null) {
            response.put("success", false);
            response.put("error", "Sesión no válida");
            return ResponseEntity.badRequest().body(response);
        }

        UsuarioModel resultado = usuarioService.actualizarPerfilUsuario(usuarioId, 
            usuarioActualizado.getNombre(), 
            usuarioActualizado.getCorreo(), 
            usuarioActualizado.getNumero(),
            passwordActual, 
            nuevaPassword, 
            imagen);

        if (resultado != null) {
            response.put("success", true);
            response.put("imagenPerfil", resultado.getImagenPerfil());
            session.setAttribute("usuario", resultado);
        } else {
            response.put("success", false);
            response.put("error", "No se pudo actualizar el perfil. Verifica tus credenciales.");
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/recuperarPassword")
    @ResponseBody
    public ResponseEntity<Map<String, String>> recuperarPassword(@RequestParam String correo) {
        Map<String, String> response = new HashMap<>();
        
        try {
            String resultado = usuarioService.iniciarRecuperacionPassword(correo);
            
            if (resultado.startsWith("Se ha enviado")) {
                response.put("status", "success");
                response.put("mensaje", resultado);
            } else {
                response.put("status", "error");
                response.put("mensaje", resultado);
            }
            
        } catch (Exception e) {
            logger.error("Error al recuperar contraseña: ", e);
            response.put("status", "error");
            response.put("mensaje", "Error al procesar la solicitud");
        }
        
        return ResponseEntity.ok(response);
    }

    // Cierre de sesión para usuarios
    @GetMapping("/cerrarSesion")
    public String cerrarSesion(HttpSession session) {
        session.invalidate();
        return "redirect:/Inicio";
    }

    @GetMapping("/proyectos/todos")
    @ResponseBody
    public Map<String, List<proyectoModel>> getAllProyectos() {
        Map<String, List<proyectoModel>> proyectos = new HashMap<>();
        proyectos.put("activos", proyectoService.getActiveProjects());
        proyectos.put("expirados", proyectoService.getExpiredProjects());
        return proyectos;
    }

    @GetMapping("/cambiarPassword")
    public String mostrarCambiarPassword(@RequestParam String token, Model model) {
        try {
            // Validar que el token existe y no ha expirado
            usuarioService.validarToken(token);
            model.addAttribute("token", token);
            return "cambiarPassword";
        } catch (RuntimeException e) {
            // Si el token no es válido, redirigir a la página de inicio de sesión
            return "redirect:/inicioSesion?error=token-invalido";
        }
    }

    @PostMapping("/cambiarPassword")
    @ResponseBody
    public ResponseEntity<Map<String, String>> cambiarPassword(
            @RequestParam String token,
            @RequestParam String nuevaPassword) {
        Map<String, String> response = new HashMap<>();
        
        try {
            String resultado = usuarioService.cambiarPassword(token, nuevaPassword);
            response.put("status", "success");
            response.put("mensaje", resultado);
        } catch (RuntimeException e) {
            response.put("status", "error");
            response.put("mensaje", e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }

   
}


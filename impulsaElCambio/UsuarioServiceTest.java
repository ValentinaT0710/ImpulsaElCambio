package com.impulsaElCambio.Servicios;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.impulsaElCambio.Model.DesafioModel;
import com.impulsaElCambio.Model.UsuarioModel;
import com.impulsaElCambio.Repository.UsuarioRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepo;
    
    @Mock
    private ForoService foroService;
    
    @Mock
    private ProyectoService proyectoService;
    
    @Mock
    private EmailService emailService;

    @InjectMocks
    private UsuarioService usuarioService;

    private UsuarioModel usuarioMock;

    @BeforeEach
    void setUp() {
        usuarioMock = new UsuarioModel();
        usuarioMock.setId(1L);
        usuarioMock.setNombre("Test User");
        usuarioMock.setCorreo("test@test.com");
        usuarioMock.setPassword("password123");
    }

    @Test
    void saveUsuario_DeberiaGuardarUsuario() {
        when(usuarioRepo.save(any(UsuarioModel.class))).thenReturn(usuarioMock);

        UsuarioModel resultado = usuarioService.saveUsuario(usuarioMock);

        assertNotNull(resultado);
        assertEquals(usuarioMock.getNombre(), resultado.getNombre());
        verify(usuarioRepo).save(any(UsuarioModel.class));
    }

    @Test
    void findByCorreo_DeberiaEncontrarUsuario() {
        when(usuarioRepo.findByCorreo("test@test.com")).thenReturn(Optional.of(usuarioMock));

        Optional<UsuarioModel> resultado = usuarioService.findByCorreo("test@test.com");

        assertTrue(resultado.isPresent());
        assertEquals("test@test.com", resultado.get().getCorreo());
    }

    @Test
    void actualizarPerfil_DeberiaActualizarUsuario() throws IOException {
        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(usuarioRepo.save(any(UsuarioModel.class))).thenReturn(usuarioMock);

        UsuarioModel resultado = usuarioService.actualizarPerfil(
            1L, 
            "Nuevo Nombre", 
            "nuevo@test.com", 
            "123456789", 
            "newPassword", 
            null
        );

        assertNotNull(resultado);
        assertEquals("Nuevo Nombre", resultado.getNombre());
        assertEquals("nuevo@test.com", resultado.getCorreo());
        verify(usuarioRepo).save(any(UsuarioModel.class));
    }

    @Test
    void iniciarRecuperacionPassword_DeberiaGenerarToken() {
        when(usuarioRepo.findByCorreo("test@test.com")).thenReturn(Optional.of(usuarioMock));
        when(usuarioRepo.save(any(UsuarioModel.class))).thenReturn(usuarioMock);

        String resultado = usuarioService.iniciarRecuperacionPassword("test@test.com");

        assertNotNull(resultado);
        verify(emailService).enviarCorreoRecuperacion(eq("test@test.com"), anyString());
    }

    @Test
    void cambiarPassword_DeberiaActualizarPassword() {
        String token = "token-valido";
        usuarioMock.setResetPasswordToken(token);
        usuarioMock.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1));

        when(usuarioRepo.findByResetPasswordToken(token)).thenReturn(Optional.of(usuarioMock));
        when(usuarioRepo.save(any(UsuarioModel.class))).thenReturn(usuarioMock);

        String resultado = usuarioService.cambiarPassword(token, "nuevaPassword123");

        assertNotNull(resultado);
        assertEquals("Contraseña actualizada exitosamente", resultado);
        verify(usuarioRepo).save(any(UsuarioModel.class));
    }

    @Test
    void eliminarCuenta_DeberiaEliminarUsuario() {
        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(usuarioMock));

        boolean resultado = usuarioService.eliminarCuenta(1L);

        assertTrue(resultado);
        verify(foroService).eliminarMeEncantaDeUsuario(1L);
        verify(foroService).eliminarPostsDeUsuario(usuarioMock);
        verify(proyectoService).eliminarParticipacionesDeUsuario(1L);
        verify(usuarioRepo).delete(usuarioMock);
    }

    @Test
    void verificarYOtorgarMedalla_DeberiaOtorgarMedalla() {
        Set<Long> proyectosParticipados = new HashSet<>(Arrays.asList(1L, 2L, 3L));
        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(proyectoService.obtenerProyectosParticipados(1L)).thenReturn(proyectosParticipados);
        when(usuarioRepo.save(any(UsuarioModel.class))).thenReturn(usuarioMock);

        usuarioService.verificarYOtorgarMedalla(1L);

        verify(usuarioRepo).save(any(UsuarioModel.class));
    }

    @Test
    void agregarPuntos_DeberiaIncrementarPuntos() {
        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(usuarioRepo.save(any(UsuarioModel.class))).thenReturn(usuarioMock);

        usuarioService.agregarPuntos(1L, 100);

        assertEquals(100, usuarioMock.getPuntos());
        verify(usuarioRepo).save(usuarioMock);
    }

    @Test
    void obtenerDesafiosCompletados_DeberiaRetornarLista() {
        Set<DesafioModel> desafios = new HashSet<>();
        usuarioMock.setDesafiosCompletados(desafios);
        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(usuarioMock));

        List<DesafioModel> resultado = usuarioService.obtenerDesafiosCompletados(1L);

        assertNotNull(resultado);
        assertEquals(0, resultado.size());
    }

    @Test
    void validarToken_DeberiaLanzarExcepcionConTokenInvalido() {
        assertThrows(RuntimeException.class, () -> {
            usuarioService.validarToken(null);
        });
    }

    @Test
    void getEmpresaConMasUsuarios_DeberiaRetornarEmpresa() {
        Object[] empresaData = new Object[]{"Empresa1", 10L};
        when(usuarioRepo.findEmpresaConMasUsuarios()).thenReturn(Arrays.<Object[]>asList(empresaData));

        String resultado = usuarioService.getEmpresaConMasUsuarios();

        assertEquals("Empresa1", resultado);
    }
}

package com.impulsaElCambio.Servicios;

import com.impulsaElCambio.Model.DesafioModel;
import com.impulsaElCambio.Model.UsuarioModel;
import com.impulsaElCambio.Repository.DesafioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DesafioServiceTest {

    @InjectMocks
    private DesafioService desafioService;

    @Mock
    private DesafioRepository desafioRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private ForoService foroService;

    @Mock
    private ProyectoService proyectoService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private DesafioModel desafioMock;
    private UsuarioModel usuarioMock;

    @BeforeEach
    void setUp() {
        // Inicializar mocks comunes
        desafioMock = new DesafioModel();
        desafioMock.setId(1L);
        desafioMock.setNombre("Desafío Test");
        desafioMock.setTipoDesafio(DesafioModel.TipoDesafio.PUBLICAR_FORO);
        desafioMock.setFechaInicio(LocalDateTime.now());
        desafioMock.setFechaFin(LocalDateTime.now().plusDays(7));
        desafioMock.setPuntosRecompensa(100);
        desafioMock.setActivo(true);

        usuarioMock = new UsuarioModel();
        usuarioMock.setId(1L);
        usuarioMock.setNombre("Usuario Test");
    }

    @Test
    void crearDesafio_DebeCrearYNotificar() {
        when(desafioRepository.save(any(DesafioModel.class))).thenReturn(desafioMock);

        DesafioModel resultado = desafioService.crearDesafio(desafioMock);

        assertNotNull(resultado);
        assertEquals(desafioMock.getId(), resultado.getId());
        assertTrue(resultado.isActivo());
        verify(messagingTemplate).convertAndSend(eq("/topic/desafios"), any(Map.class));
    }

    @Test
    void obtenerDesafiosActivos_DebeRetornarListaDesafios() {
        List<DesafioModel> desafiosEsperados = Arrays.asList(desafioMock);
        when(desafioRepository.findActiveDesafios(any(LocalDateTime.class))).thenReturn(desafiosEsperados);

        List<DesafioModel> resultado = desafioService.obtenerDesafiosActivos();

        assertNotNull(resultado);
        assertFalse(resultado.isEmpty());
        assertEquals(desafiosEsperados.size(), resultado.size());
    }

    @Test
    void participarEnDesafio_DebeUnirUsuarioAlDesafio() {
        desafioMock.setActivo(true);
        desafioMock.setFechaFin(LocalDateTime.now().plusDays(7));
        
        when(desafioRepository.findById(anyLong())).thenReturn(Optional.of(desafioMock));
        when(usuarioService.findById(anyLong())).thenReturn(Optional.of(usuarioMock));
        when(desafioRepository.save(any(DesafioModel.class))).thenReturn(desafioMock);

        String resultado = desafioService.participarEnDesafio(1L, 1L);

        assertEquals("Te has unido al desafío exitosamente", resultado);
        verify(desafioRepository).save(any(DesafioModel.class));
    }

    @Test
    void completarDesafio_DebeCompletarYDarPuntos() {
        desafioMock.getParticipantes().add(usuarioMock);
        desafioMock.setActivo(true);
        desafioMock.setFechaFin(LocalDateTime.now().plusDays(7));
        
        when(desafioRepository.findById(anyLong())).thenReturn(Optional.of(desafioMock));
        when(usuarioService.findById(anyLong())).thenReturn(Optional.of(usuarioMock));
        when(foroService.hasUserPostedSince(anyLong(), any(LocalDateTime.class))).thenReturn(true);
        when(desafioRepository.save(any(DesafioModel.class))).thenReturn(desafioMock);
        doNothing().when(usuarioService).agregarPuntos(anyLong(), anyInt());
        when(usuarioService.saveUsuario(any(UsuarioModel.class))).thenReturn(usuarioMock);

        String resultado = desafioService.completarDesafio(1L, 1L);

        assertEquals("¡Felicitaciones! Has completado el desafío", resultado);
        verify(usuarioService).agregarPuntos(anyLong(), anyInt());
        verify(desafioRepository).save(any(DesafioModel.class));
        verify(usuarioService).saveUsuario(any(UsuarioModel.class));
    }

    @Test
    void verificarCompletitudDesafio_DebeVerificarYCompletarSiCumple() {
        // Preparar el desafío mock
        desafioMock.getParticipantes().add(usuarioMock);
        List<DesafioModel> desafiosActivos = Arrays.asList(desafioMock);
        
        // Configurar mocks
        when(desafioRepository.findActiveDesafios(any(LocalDateTime.class))).thenReturn(desafiosActivos);
        when(desafioRepository.findById(anyLong())).thenReturn(Optional.of(desafioMock));
        when(usuarioService.findById(anyLong())).thenReturn(Optional.of(usuarioMock));
        // Permitir múltiples llamadas a hasUserPostedSince
        lenient().when(foroService.hasUserPostedSince(anyLong(), any(LocalDateTime.class))).thenReturn(true);
        when(desafioRepository.save(any(DesafioModel.class))).thenReturn(desafioMock);
        doNothing().when(usuarioService).agregarPuntos(anyLong(), anyInt());
        when(usuarioService.saveUsuario(any(UsuarioModel.class))).thenReturn(usuarioMock);
        
        // Ejecutar el método
        desafioService.verificarCompletitudDesafio(1L);

        // Verificaciones
        verify(desafioRepository).save(any(DesafioModel.class));
        verify(usuarioService).agregarPuntos(eq(1L), eq(desafioMock.getPuntosRecompensa()));
        verify(usuarioService).saveUsuario(any(UsuarioModel.class));
        // Verificar que hasUserPostedSince se llamó al menos una vez
        verify(foroService, atLeastOnce()).hasUserPostedSince(anyLong(), any(LocalDateTime.class));
    }

    @Test
    void eliminarDesafio_DebeEliminarYNotificar() {
        doNothing().when(desafioRepository).deleteById(anyLong());

        desafioService.eliminarDesafio(1L);

        verify(desafioRepository).deleteById(1L);
        verify(messagingTemplate).convertAndSend(eq("/topic/desafios"), any(Map.class));
    }

    @Test
    void obtenerDesafiosActivosUsuario_DebeRetornarListaFormateada() {
        desafioMock.getParticipantes().add(usuarioMock);
        List<DesafioModel> desafiosActivos = Arrays.asList(desafioMock);
        when(desafioRepository.findActiveDesafios(any(LocalDateTime.class))).thenReturn(desafiosActivos);
        when(foroService.hasUserPostedSince(anyLong(), any(LocalDateTime.class))).thenReturn(true);

        List<Map<String, Object>> resultado = desafioService.obtenerDesafiosActivosUsuario(1L);

        assertNotNull(resultado);
        assertFalse(resultado.isEmpty());
        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).containsKey("progreso"));
        assertTrue(resultado.get(0).containsKey("diasRestantes"));
    }

    @Test
    void participarEnDesafio_DebeRetornarError_CuandoDesafioNoExiste() {
        when(desafioRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            desafioService.participarEnDesafio(1L, 1L);
        });
    }

    @Test
    void completarDesafio_DebeRetornarError_CuandoUsuarioNoParticipa() {
        when(desafioRepository.findById(anyLong())).thenReturn(Optional.of(desafioMock));
        when(usuarioService.findById(anyLong())).thenReturn(Optional.of(usuarioMock));

        String resultado = desafioService.completarDesafio(1L, 1L);

        assertEquals("No estás participando en este desafío", resultado);
    }
} 
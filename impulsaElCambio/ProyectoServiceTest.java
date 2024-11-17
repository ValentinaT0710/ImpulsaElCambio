package com.impulsaElCambio.Servicios;

import org.hibernate.cfg.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.impulsaElCambio.Model.UsuarioModel;
import com.impulsaElCambio.Model.proyectoModel;
import com.impulsaElCambio.Repository.Repository;
import com.impulsaElCambio.Repository.UsuarioRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

@SpringBootTest
public class ProyectoServiceTest {

    @MockBean
    private Repository proyectoRepository;
    
    @MockBean
    private UsuarioRepository usuarioRepository;
    
    @MockBean
    private UsuarioService usuarioService;
    
    @MockBean
    private Environment env;
    
    @MockBean
    private SimpMessagingTemplate messagingTemplate;
        
    @Autowired
    private ProyectoService proyectoService;

    private proyectoModel proyectoMock;
    private UsuarioModel usuarioMock;

    @BeforeEach
    void setUp() {
        proyectoMock = new proyectoModel();
        proyectoMock.setId(1L);
        proyectoMock.setNombre("Proyecto Test");
        proyectoMock.setDescripcion("Descripción Test");
        proyectoMock.setEstado("activo");
        proyectoMock.setFechaCreacion(LocalDateTime.now());
        proyectoMock.setFechaExpiracion(LocalDateTime.now().plusDays(30));

        usuarioMock = new UsuarioModel();
        usuarioMock.setId(1L);
    }

    @Test
    void crearProyecto_DeberiaCrearNuevoProyecto() throws IOException {
        when(proyectoRepository.save(any(proyectoModel.class))).thenAnswer(i -> i.getArguments()[0]);
        doNothing().when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        proyectoModel resultado = proyectoService.crearProyecto(
            "Test Proyecto", 
            "Test Descripción", 
            LocalDateTime.now().plusDays(30), 
            null
        );

        assertNotNull(resultado);
        assertEquals("Test Proyecto", resultado.getNombre());
        verify(proyectoRepository).save(any(proyectoModel.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/proyectos"), any(Object.class));
    }

    @Test
    void participarEnProyecto_DeberiaAgregarUsuarioAProyecto() {
        when(proyectoRepository.findById(anyLong())).thenReturn(Optional.of(proyectoMock));
        when(usuarioService.findById(anyLong())).thenReturn(Optional.of(usuarioMock));
        when(proyectoRepository.save(any())).thenReturn(proyectoMock);

        boolean resultado = proyectoService.participarEnProyecto(1L, 1L);

        assertTrue(resultado);
        verify(proyectoRepository).save(any());
        verify(usuarioService).verificarYOtorgarMedalla(anyLong());
    }

    @Test
    void eliminarParticipacionUsuario_DeberiaEliminarUsuarioDeProyecto() {
        proyectoMock.getParticipantes().add(usuarioMock);
        
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyectoMock));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioMock));
        when(proyectoRepository.save(any())).thenReturn(proyectoMock);

        int resultado = proyectoService.eliminarParticipacionUsuario(1L, 1L);

        assertEquals(1, resultado);
        verify(proyectoRepository).save(any());
        verify(messagingTemplate).convertAndSend(eq("/topic/proyectos"), any(Object.class));
    }

    @Test
    void getActiveProjects_DeberiaRetornarSoloProyectosActivos() {
        List<proyectoModel> proyectos = Arrays.asList(
            createProyecto(true),
            createProyecto(false)
        );
        when(proyectoRepository.findAll()).thenReturn(proyectos);

        List<proyectoModel> resultado = proyectoService.getActiveProjects();

        assertEquals(1, resultado.size());
        assertTrue(resultado.get(0).getFechaExpiracion().isAfter(LocalDateTime.now()));
    }

    @Test
    void editarProyecto_DeberiaActualizarProyecto() throws IOException {
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyectoMock));
        when(proyectoRepository.existsById(1L)).thenReturn(true);
        when(proyectoRepository.save(any(proyectoModel.class))).thenAnswer(i -> {
            proyectoModel proyecto = (proyectoModel) i.getArguments()[0];
            proyecto.setNombre("Nuevo Nombre");
            proyecto.setDescripcion("Nueva Descripción");
            return proyecto;
        });
        
        proyectoModel resultado = proyectoService.editarProyecto(1L, "Nuevo Nombre", "Nueva Descripción", null);
        
        assertNotNull(resultado);
        assertEquals("Nuevo Nombre", resultado.getNombre());
        assertEquals("Nueva Descripción", resultado.getDescripcion());
        verify(proyectoRepository).save(any(proyectoModel.class));
    }

    @Test
    void hasUserCompletedMinimumParticipation_DeberiaVerificarParticipacionMinima() {
        proyectoMock.setFechaCreacion(LocalDateTime.now().minusDays(10));
        proyectoMock.getParticipantes().add(usuarioMock);
        
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyectoMock));

        boolean resultado = proyectoService.hasUserCompletedMinimumParticipation(1L, 1L);

        assertFalse(resultado);
        verify(proyectoRepository).findById(1L);
    }

    @Test
    void isUserParticipatingInProject_DeberiaVerificarParticipacion() {
        proyectoMock.getParticipantes().add(usuarioMock);
        when(proyectoRepository.findById(1L)).thenReturn(Optional.of(proyectoMock));

        boolean resultado = proyectoService.isUserParticipatingInProject(1L, 1L);

        assertTrue(resultado);
        verify(proyectoRepository).findById(1L);
    }

    private proyectoModel createProyecto(boolean activo) {
        proyectoModel proyecto = new proyectoModel();
        proyecto.setFechaExpiracion(activo ? 
            LocalDateTime.now().plusDays(1) : 
            LocalDateTime.now().minusDays(1));
        proyecto.setEstado("activo");
        return proyecto;
    }
} 
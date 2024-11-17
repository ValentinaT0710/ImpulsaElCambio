package com.impulsaElCambio.Servicios;

import com.impulsaElCambio.Model.Foro_post;
import com.impulsaElCambio.Model.UsuarioModel;
import com.impulsaElCambio.Repository.ForoPostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ForoServiceTest {

    @Mock
    private ForoPostRepository foroPostRepository;

    @Mock
    private UsuarioService usuarioService;

    private ForoService foroService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        foroService = new ForoService(foroPostRepository, usuarioService);
    }

    @Test
    void getAllComentarios_DebeRetornarListaDeComentarios() {
        // Arrange
        List<Foro_post> comentariosMock = Arrays.asList(new Foro_post(), new Foro_post());
        when(foroPostRepository.findMainComments(any(Pageable.class))).thenReturn(comentariosMock);

        // Act
        List<Foro_post> resultado = foroService.getAllComentarios();

        // Assert
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
    }

    @Test
    void crearComentario_DebeCrearComentarioExitosamente() {
        // Arrange
        String contenido = "Test contenido";
        Long usuarioId = 1L;
        UsuarioModel usuario = new UsuarioModel();
        usuario.setId(usuarioId);
        
        when(usuarioService.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(foroPostRepository.save(any(Foro_post.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Foro_post resultado = foroService.crearComentario(contenido, usuarioId);

        // Assert
        assertNotNull(resultado);
        assertEquals(contenido, resultado.getContenido());
        assertEquals(usuario, resultado.getUsuario());
    }

    @Test
    void borrarComentario_DebeEliminarComentarioExitosamente() {
        // Arrange
        Long comentarioId = 1L;
        Long usuarioId = 1L;
        
        UsuarioModel usuario = new UsuarioModel();
        usuario.setId(usuarioId);
        
        Foro_post comentario = new Foro_post();
        comentario.setUsuario(usuario);
        
        when(foroPostRepository.findById(comentarioId)).thenReturn(Optional.of(comentario));
        when(usuarioService.findById(usuarioId)).thenReturn(Optional.of(usuario));

        // Act
        String resultado = foroService.borrarComentario(comentarioId, usuarioId);

        // Assert
        assertEquals("Comentario eliminado exitosamente", resultado);
        verify(foroPostRepository).deleteById(comentarioId);
    }

    @Test
    void editarComentario_DebeEditarComentarioExitosamente() {
        // Arrange
        Long comentarioId = 1L;
        Long usuarioId = 1L;
        String nuevoContenido = "Contenido editado";
        
        UsuarioModel usuario = new UsuarioModel();
        usuario.setId(usuarioId);
        
        Foro_post comentario = new Foro_post();
        comentario.setUsuario(usuario);
        
        when(foroPostRepository.findById(comentarioId)).thenReturn(Optional.of(comentario));
        when(usuarioService.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(foroPostRepository.save(any(Foro_post.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Foro_post resultado = foroService.editarComentario(comentarioId, nuevoContenido, usuarioId);

        // Assert
        assertNotNull(resultado);
        assertEquals(nuevoContenido, resultado.getContenido());
    }

    @Test
    void toggleMeEncanta_DebeToggleCorrectamente() {
        // Arrange
        Long comentarioId = 1L;
        Long usuarioId = 1L;
        
        UsuarioModel usuario = new UsuarioModel();
        usuario.setId(usuarioId);
        
        Foro_post comentario = new Foro_post();
        comentario.setMeEncanta(new HashSet<>());
        comentario.setContadorMeEncanta(0);
        
        when(foroPostRepository.findById(comentarioId)).thenReturn(Optional.of(comentario));
        when(usuarioService.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(foroPostRepository.save(any(Foro_post.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Map<String, Object> resultado = foroService.toggleMeEncanta(comentarioId, usuarioId);

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.get("contadorMeEncanta"));
        assertTrue((Boolean) resultado.get("meEncantaActivo"));
    }

    @Test
    void crearRespuesta_DebeCrearRespuestaExitosamente() {
        // Arrange
        String contenido = "Respuesta test";
        Long usuarioId = 1L;
        Long comentarioPadreId = 1L;
        
        UsuarioModel usuario = new UsuarioModel();
        usuario.setId(usuarioId);
        
        Foro_post comentarioPadre = new Foro_post();
        
        when(foroPostRepository.findById(comentarioPadreId)).thenReturn(Optional.of(comentarioPadre));
        when(usuarioService.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(foroPostRepository.save(any(Foro_post.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Foro_post resultado = foroService.crearRespuesta(contenido, usuarioId, comentarioPadreId);

        // Assert
        assertNotNull(resultado);
        assertEquals(contenido, resultado.getContenido());
        assertEquals(comentarioPadre, resultado.getComentarioPadre());
    }

    @Test
    void countRecentPosts_DebeContarPostsRecientes() {
        // Arrange
        int dias = 7;
        when(foroPostRepository.countByFechaCreacionAfter(any(LocalDateTime.class))).thenReturn(5L);

        // Act
        long resultado = foroService.countRecentPosts(dias);

        // Assert
        assertEquals(5L, resultado);
    }

    @Test
    void hasUserPostedSince_DebeVerificarPostsRecientes() {
        // Arrange
        Long usuarioId = 1L;
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        when(foroPostRepository.existsByUsuario_IdAndFechaCreacionAfter(usuarioId, since)).thenReturn(true);

        // Act
        boolean resultado = foroService.hasUserPostedSince(usuarioId, since);

        // Assert
        assertTrue(resultado);
    }
} 
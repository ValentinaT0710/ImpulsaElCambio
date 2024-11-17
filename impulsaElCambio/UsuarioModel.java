package com.impulsaElCambio.Model;

import jakarta.persistence.*;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;


/**
 * Representa un usuario en el sistema.
 * Esta clase es una entidad JPA que se mapea a la tabla "usuarios" en la base de datos.
 */
@Entity
@Table(name = "usuarios")
public class UsuarioModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    private String nombre;
    private String correo;
    private String numero;
    private String password;
    @Column(name = "empresa")
    private String empresa; // Nuevo campo
    @Column(name = "imagen_perfil")
    private String imagenPerfil;
    @Enumerated(EnumType.STRING)
    private Rol rol;

    /**
     * Enumeración que define los roles posibles para un usuario.
     */
    public enum Rol {
        ADMIN, VOLUNTARIO
    }

    @OneToMany(mappedBy = "usuario")
    private List<Foro_post> posts;

    /**
     * Constructor por defecto requerido por JPA.
     */
    public UsuarioModel() {
    }

    /**
     * Constructor con todos los campos.
     * @param id Identificador único del usuario.
     * @param nombre Nombre del usuario.
     * @param correo Correo electrónico del usuario.
     * @param numero Número de teléfono del usuario.
     * @param password Contraseña del usuario.
     * @param empresa Nombre de la empresa del usuario.
     * @param rol Rol del usuario.
     */
        public UsuarioModel(Long id, String nombre, String correo, String numero, String password, String empresa, Rol rol, String imagenPerfil) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.numero = numero;
        this.password = password;
        this.empresa = empresa;
        this.rol = rol;
        this.imagenPerfil = imagenPerfil;
    }

    /**
     * Obtiene el ID del usuario.
     * @return El ID del usuario.
     */
    public Long getId() {
        return id;
    }

    /**
     * Establece el ID del usuario.
     * @param id El ID a establecer.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Obtiene el nombre del usuario.
     * @return El nombre del usuario.
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * Establece el nombre del usuario.
     * @param nombre El nombre a establecer.
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * Obtiene el correo electrónico del usuario.
     * @return El correo electrónico del usuario.
     */
    public String getCorreo() {
        return correo;
    }

    /**
     * Establece el correo electrónico del usuario.
     * @param correo El correo electrónico a establecer.
     */
    public void setCorreo(String correo) {
        this.correo = correo;
    }

    /**
     * Obtiene el número de teléfono del usuario.
     * @return El número de teléfono del usuario.
     */
    public String getNumero() {
        return numero;
    }

    /**
     * Establece el número de teléfono del usuario.
     * @param numero El número de teléfono a establecer.
     */
    public void setNumero(String numero) {
        this.numero = numero;
    }

    /**
     * Obtiene la contraseña del usuario.
     * @return La contraseña del usuario.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Establece la contraseña del usuario.
     * @param password La contraseña a establecer.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Obtiene el rol del usuario.
     * @return El rol del usuario.
     */
    public Rol getRol() {
        return rol;
    }

    /**
     * Establece el rol del usuario.
     * @param rol El rol a establecer.
     */
    public void setRol(Rol rol) {
        this.rol = rol;
    }

    /**
     * Obtiene el nombre de la empresa del usuario.
     * @return El nombre de la empresa del usuario.
     */
    public String getEmpresa() {
        return empresa;
    }

    /**
     * Establece el nombre de la empresa del usuario.
     * @param empresa El nombre de la empresa a establecer.
     */
    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @PrePersist
    protected void onCreate() {
        fechaRegistro = LocalDateTime.now();
    }

    /**
     * Obtiene la fecha de registro del usuario.
     * @return La fecha de registro del usuario.
     */
    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    /**
     * Establece la fecha de registro del usuario.
     * @param fechaRegistro La fecha de registro a establecer.
     */
    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    @ManyToMany(mappedBy = "participantes")
    private Set<proyectoModel> proyectos = new HashSet<>();

    /**
     * Obtiene los proyectos del usuario.
     * @return Los proyectos del usuario.
     */
    public Set<proyectoModel> getProyectos() {
        return proyectos;
    }

    /**
     * Establece los proyectos del usuario.
     * @param proyectos Los proyectos a establecer.
     */
    public void setProyectos(Set<proyectoModel> proyectos) {
        this.proyectos = proyectos;
    }

    public void addProyecto(proyectoModel proyecto) {
        this.proyectos.add(proyecto);
        proyecto.getParticipantes().add(this);
    }

    public void removeProyecto(proyectoModel proyecto) {
        this.proyectos.remove(proyecto);
        proyecto.getParticipantes().remove(this);
    }

    public String getImagenPerfil() {
        return imagenPerfil;
    }

    public void setImagenPerfil(String imagenPerfil) {
        this.imagenPerfil = imagenPerfil;
    }

    private int puntos = 0;
    @Column(name = "insignias")
    private String insignias = "";

    public int getPuntos() {
        return puntos;
    }

    public void setPuntos(int puntos) {
        this.puntos = puntos;
    }

    public String getInsignias() {
        return insignias;
    }

    public void setInsignias(String insignias) {
        this.insignias = insignias;
    }

    public void agregarPuntos(int puntos) {
        this.puntos += puntos;
    }

    public void agregarInsignia(String nuevaInsignia) {
        if (this.insignias == null || this.insignias.isEmpty()) {
            this.insignias = nuevaInsignia;
        } else if (!this.insignias.contains(nuevaInsignia)) {
            this.insignias += "," + nuevaInsignia;
        }
    }

    public List<String> getInsigniasList() {
        if (this.insignias == null || this.insignias.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(this.insignias.split(","));
    }

    @ManyToMany(mappedBy = "participantes")
    private Set<DesafioModel> desafiosActivos = new HashSet<>();

    @ManyToMany(mappedBy = "usuariosCompletados")
    private Set<DesafioModel> desafiosCompletados = new HashSet<>();

    public Set<DesafioModel> getDesafiosActivos() {
        return desafiosActivos;
    }

    public void setDesafiosActivos(Set<DesafioModel> desafiosActivos) {
        this.desafiosActivos = desafiosActivos;
    }

    public Set<DesafioModel> getDesafiosCompletados() {
        return desafiosCompletados;
    }

    public void setDesafiosCompletados(Set<DesafioModel> desafiosCompletados) {
        this.desafiosCompletados = desafiosCompletados;
    }

    public void addDesafioActivo(DesafioModel desafio) {
        desafiosActivos.add(desafio);
        desafio.getParticipantes().add(this);
    }

    public void removeDesafioActivo(DesafioModel desafio) {
        desafiosActivos.remove(desafio);
        desafio.getParticipantes().remove(this);
    }

    public void addDesafioCompletado(DesafioModel desafio) {
        desafiosCompletados.add(desafio);
        desafio.getUsuariosCompletados().add(this);
    }

    public void removeDesafioCompletado(DesafioModel desafio) {
        desafiosCompletados.remove(desafio);
        desafio.getUsuariosCompletados().remove(this);
    }

    @Column(name = "resetPasswordToken")
    private String resetPasswordToken;

    @Column(name = "resetPasswordTokenExpiry")
    private LocalDateTime resetPasswordTokenExpiry;

    public String getResetPasswordToken() {
        return resetPasswordToken;
    }

    public void setResetPasswordToken(String resetPasswordToken) {
        this.resetPasswordToken = resetPasswordToken;
    }

    public LocalDateTime getResetPasswordTokenExpiry() {
        return resetPasswordTokenExpiry;
    }

    public void setResetPasswordTokenExpiry(LocalDateTime resetPasswordTokenExpiry) {
        this.resetPasswordTokenExpiry = resetPasswordTokenExpiry;
    }

    public void completarDesafio(DesafioModel desafio) {
        // Primero removemos el desafío de los activos
        removeDesafioActivo(desafio);
        
        // Luego lo agregamos a los completados
        addDesafioCompletado(desafio);
    }

    
   
}

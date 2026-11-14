/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.model;

/**
 *
 * @author richard
 * 
 * Modos de uso de AvilaOS. El administrador puede hacer todas las operaciones; el
 * usuario solo lee sus archivos o los publicos y opera sobre sus propios archivos.
 */
public enum ModoUsuario {

    ADMINISTRADOR("Administrador"),
    USUARIO("Usuario");

    private final String nombreVisible;

    ModoUsuario(String nombreVisible) {
        this.nombreVisible = nombreVisible;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    @Override
    public String toString() {
        return nombreVisible;
    }
}

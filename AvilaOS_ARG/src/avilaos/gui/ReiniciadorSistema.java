/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.gui;

import avilaos.config.ConfiguracionSimulacion;

/** 
 * @author richard
 * 
 * Lo que la vista de configuracion le pide a la aplicacion: armar de nuevo el sistema con otra configuracion. 
 */
public interface ReiniciadorSistema {

    /** @return null si se aplico, o un mensaje de error para mostrar en pantalla */
    String reiniciar(ConfiguracionSimulacion configuracion);
}

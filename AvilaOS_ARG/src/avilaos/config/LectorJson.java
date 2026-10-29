/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package avilaos.config;

import avilaos.util.Lista;

/**
 * @author richard
 * 
 * Lector de JSON escrito a mano (no se permiten librerias externas ni colecciones de
 * java.util). Soporta objetos, listas, textos, numeros enteros y decimales, true, false y null.
 * Ante un error lanza EntradaInvalidaException indicando la posicion.
 */
public final class LectorJson {

    private final String texto;
    private int pos = 0;

    private LectorJson(String texto) {
        this.texto = texto;
    }

    public static Object leer(String texto) {
        if (texto == null) {
            throw new EntradaInvalidaException("JSON: el texto está vacío");
        }
        LectorJson lector = new LectorJson(texto);
        lector.saltarEspacios();
        Object valor = lector.valor();
        lector.saltarEspacios();
        if (lector.pos < texto.length()) {
            throw lector.error("hay texto después del final del documento");
        }
        return valor;
    }

    private Object valor() {
        if (pos >= texto.length()) {
            throw error("el documento terminó antes de tiempo");
        }
        char c = texto.charAt(pos);
        if (c == '{') {
            return objeto();
        }
        if (c == '[') {
            return lista();
        }
        if (c == '"') {
            return cadena();
        }
        if (c == '-' || (c >= '0' && c <= '9')) {
            return numero();
        }
        if (texto.startsWith("true", pos)) {
            pos += 4;
            return Boolean.TRUE;
        }
        if (texto.startsWith("false", pos)) {
            pos += 5;
            return Boolean.FALSE;
        }
        if (texto.startsWith("null", pos)) {
            pos += 4;
            return null;
        }
        throw error("carácter inesperado '" + c + "'");
    }

    private JsonObjeto objeto() {
        JsonObjeto resultado = new JsonObjeto();
        pos++;   // {
        saltarEspacios();
        if (actual() == '}') {
            pos++;
            return resultado;
        }
        while (true) {
            saltarEspacios();
            if (actual() != '"') {
                throw error("se esperaba el nombre de una clave entre comillas");
            }
            String clave = cadena();
            saltarEspacios();
            esperar(':');
            saltarEspacios();
            resultado.poner(clave, valor());
            saltarEspacios();
            if (actual() == ',') {
                pos++;
            } else if (actual() == '}') {
                pos++;
                return resultado;
            } else {
                throw error("se esperaba ',' o '}'");
            }
        }
    }

    private Lista<Object> lista() {
        Lista<Object> resultado = new Lista<>();
        pos++;   // [
        saltarEspacios();
        if (actual() == ']') {
            pos++;
            return resultado;
        }
        while (true) {
            saltarEspacios();
            resultado.agregarAlFinal(valor());
            saltarEspacios();
            if (actual() == ',') {
                pos++;
            } else if (actual() == ']') {
                pos++;
                return resultado;
            } else {
                throw error("se esperaba ',' o ']'");
            }
        }
    }

    private String cadena() {
        pos++;   // comilla inicial
        StringBuilder sb = new StringBuilder();
        while (pos < texto.length()) {
            char c = texto.charAt(pos++);
            if (c == '"') {
                return sb.toString();
            }
            if (c == '\\') {
                if (pos >= texto.length()) {
                    break;
                }
                char e = texto.charAt(pos++);
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'n': sb.append('\n'); break;
                    case 't': sb.append('\t'); break;
                    case 'r': sb.append('\r'); break;
                    case 'u':
                        if (pos + 4 > texto.length()) {
                            throw error("secuencia \\u incompleta");
                        }
                        try {
                            sb.append((char) Integer.parseInt(texto.substring(pos, pos + 4), 16));
                        } catch (NumberFormatException ex) {
                            throw error("secuencia \\u inválida");
                        }
                        pos += 4;
                        break;
                    default:
                        throw error("escape inválido \\" + e);
                }
            } else {
                sb.append(c);
            }
        }
        throw error("texto sin cerrar (falta la comilla final)");
    }

    private Object numero() {
        int inicio = pos;
        if (actual() == '-') {
            pos++;
        }
        boolean decimal = false;
        while (pos < texto.length()) {
            char c = texto.charAt(pos);
            if (c >= '0' && c <= '9') {
                pos++;
            } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                decimal = true;
                pos++;
            } else {
                break;
            }
        }
        String fragmento = texto.substring(inicio, pos);
        try {
            return decimal ? (Object) Double.valueOf(fragmento) : (Object) Long.valueOf(fragmento);
        } catch (NumberFormatException e) {
            pos = inicio;
            throw error("número inválido \"" + fragmento + "\"");
        }
    }

    private char actual() {
        if (pos >= texto.length()) {
            throw error("el documento terminó antes de tiempo");
        }
        return texto.charAt(pos);
    }

    private void esperar(char c) {
        if (actual() != c) {
            throw error("se esperaba '" + c + "'");
        }
        pos++;
    }

    private void saltarEspacios() {
        while (pos < texto.length() && Character.isWhitespace(texto.charAt(pos))) {
            pos++;
        }
    }

    private EntradaInvalidaException error(String mensaje) {
        int linea = 1;
        int columna = 1;
        for (int i = 0; i < pos && i < texto.length(); i++) {
            if (texto.charAt(i) == '\n') {
                linea++;
                columna = 1;
            } else {
                columna++;
            }
        }
        return new EntradaInvalidaException("JSON inválido (línea " + linea + ", columna " + columna + "): " + mensaje);
    }
}

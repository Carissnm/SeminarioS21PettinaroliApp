package ar.edu.csp.sistemadegestioncspgui.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public abstract class BaseController {
    protected void info(String m){
        new Alert(Alert.AlertType.INFORMATION,m, ButtonType.OK).showAndWait();
    }
    protected void warn(String m){
        new Alert(Alert.AlertType.WARNING,m, ButtonType.OK).showAndWait();
    }
    protected void error(String m){
        new Alert(Alert.AlertType.ERROR,m, ButtonType.OK).showAndWait();
    }
    protected static String nv(String s){
        return s==null ? "" : s;
    }
}
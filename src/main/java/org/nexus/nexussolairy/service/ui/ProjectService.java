package org.nexus.nexussolairy.service.ui;

import org.nexus.nexussolairy.model.enums.LanguageType;
import org.nexus.nexussolairy.model.file.Project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProjectService {
    private final FileService fileService;
    private final List<String> recentProjects;

    public ProjectService(FileService fileService) {
        this.fileService = fileService;
        this.recentProjects = new ArrayList<>();
    }

    // creacion de nuevos proyectos con estructura de carpetas de test y necesarias
    public Project createProject(String name, File location, LanguageType initialLanguage) throws IOException {
        File projectDir = new File(location, name);
        if (projectDir.exists()) {
            throw new IOException("Project directory already exists: " + projectDir.getAbsolutePath());
        }
        if (!projectDir.mkdirs()) {
            throw new IOException("Could not create directory: " + projectDir.getAbsolutePath());
        }

        File srcDir = new File(projectDir, "src");
        File genDir = new File(projectDir, "generated");

        srcDir.mkdirs();
        genDir.mkdirs();

        File mainPig = new File(genDir, "main.pig");
        File mainY = new File(genDir, "python.y");
        File mainZet = new File(genDir, "zet.z");

        fileService.writeFile(mainPig, getStarterCode(LanguageType.PIG_LATIN));
        fileService.writeFile(mainY, getStarterCode(LanguageType.Y_LANG));
        fileService.writeFile(mainZet, getStarterCode(LanguageType.ZETARIANO));

        addRecentProject(projectDir.getAbsolutePath());

        Project project = new Project(name, projectDir);
        project.setActiveFile(mainPig);

        return project;
    }

    public Project openProject(File projectDir) {
        if (projectDir == null || !projectDir.exists() || !projectDir.isDirectory()) {
            return null;
        }
        addRecentProject(projectDir.getAbsolutePath());
        return new Project(projectDir.getName(), projectDir);
    }

    public List<String> getRecentProjects() {
        return recentProjects;
    }

    public void addRecentProject(String path) {
        recentProjects.remove(path);
        recentProjects.add(0, path);
        if (recentProjects.size() > 10) {
            recentProjects.remove(recentProjects.size() - 1);
        }
    }

    public String getStarterCode(LanguageType language) {
        return switch (language) {
            case PIG_LATIN -> """
                    VARIABILES>
                    
                    esto edad : numerus 20;
                    esto nombre : textum "Carlos";
                    esto activo : verum;
                    
                    MAIOR>
                    
                    >> "Hola comandante Nexu!";
                    >> "Iniciando compilacion de Pig Latin...";
                    >> nombre;
                    >> edad;
                    
                    FINIS;
                    """;
            case Y_LANG -> """
                    // Estructuras globales
                    %estructuras
                    estructura Persona:
                        cadena nombre
                        entero edad
                        flotante promedio
                    
                    estructura Direccion:
                        cadena calle
                        entero numero
                    
                    // Funciones
                    %funciones
                    definir saludar(cadena mensaje):
                        imprimir(mensaje)
                    
                    definir esMayor(entero edad) -> bool:
                        si (edad >= 18) entonces
                            retornar verdadero
                        contrario
                            retornar falso
                    
                    definir principal():
                        // Variables
                        entero contador = 0
                        flotante pi = 3.14
                        bool activo = falso
                        cadena nombre = "Resistencia"
                    
                        // Arreglo y struct
                        entero valores[3] = {1, 2, 3}
                        Persona p = {"Ana", 25, 85.5}
                    
                        // Ciclo mientras
                        mientras (contador < 5) hacer
                            si (contador == 2) entonces
                                continuar
                            imprimir(contador)
                            contador++
                    
                        // Ciclo para
                        para (entero i = 0; i < 3; i++):
                            imprimir(i)
                    
                        // Elegir
                        elegir (contador):
                            caso 1:
                                imprimir("Uno")
                                romper
                            caso 2:
                                imprimir("Dos")
                                romper
                            siempre:
                                imprimir("Otro")
                                romper
                    """;
            case ZETARIANO -> """
                    clase Explorador {
                        entero id;
                        cadena alias;
                    
                        metodo inicializar(entero nuevoId, cadena nuevoAlias) {
                            este.id = nuevoId;
                            este.alias = nuevoAlias;
                        }
                    
                        metodo ejecutarMision() {
                            si este.id > 0 {
                                imprimir("Mision espacial iniciada por: " + este.alias);
                            }
                        }
                    }
                    """;
            default -> "";
        };
    }
}

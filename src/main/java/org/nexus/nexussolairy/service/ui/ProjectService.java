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
                    import estructuras.y
                    
                    VARIABILES>
                    esto edad : numerus 20;
                    esto gravedad : decimalis 9.81;
                    esto activo : bool verum;
                    series numeros[3]: numerus {1, 2, 3};
                    esto p : Persona {"Ana", 25, {"Central", 100}};
                    
                    MAIOR>
                    >> "Hola";
                    esto nombre : textum;
                    <<
                    >> "Bienvenido" >> nombre;
                    
                    si (edad >= 18 && activo == verum) {
                        >> "Mayor";
                    } aliter {
                        >> "Menor";
                    } finis;
                    
                    dum (edad < 25) {
                        edad++;
                    } finis;
                    
                    facere {
                        edad--;
                    } dum (edad > 0);
                    
                    per (esto i : numerus 0; i < 5; i++) {
                        >> i;
                    }
                    
                    esto obj : novus Persona(20, "Carlos");
                    obj.nombre = "Yennifer";
                    numeros[0] = 99;
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
            public class Persona {
                String nombre;
                int edad;

                public Persona(String nombreParametro, int edadParametro) {
                    nombre = nombreParametro;
                    edad = edadParametro;
                }

                public Persona() {
                    nombre = "Sin nombre";
                    edad = 0;
                }

                public void saludar() {
                    println("Hola! Me llamo " + nombre);
                }

                public int calcularAnioNacimiento(int anioActual) {
                    return anioActual - edad;
                }
            }

            public class Principal {
                public static void main(String[] args) {
                    int a = 10;
                    int b = 3;
                    int suma = a + b;
                    boolean esMayor = (a > b) && (a != 0);

                    int[] numeros = {10, 20, 30, 40, 50};
                    int[] calificaciones = new int[5];
                    String[] nombres = {"Carlos", "Ana", "Pedro"};
                    int[][] matriz = new int[3][3];

                    String mensaje = (esMayor) ? "Mayor" : "Menor";

                    Persona p1 = new Persona("Carlos", 25);
                    Persona p2 = new Persona();
                    int sumaEdades = p1.edad + p2.edad;

                    if (p1 == null) {
                        print("Nulo");
                    } else if (edad >= 18) {
                        println("Adulto");
                    } else {
                        println("Menor");
                    }

                    for (int i = 0; i < 5; i++) {
                        if (i == 2) continue;
                        if (i == 4) break;
                        println(i);
                    }

                    for (;;) {
                        println("Bucle infinito escapado");
                        break;
                    }

                    int contador = 0;
                    while (contador < 3) {
                        contador++;
                    }

                    int intentos = 0;
                    do {
                        intentos++;
                        if (intentos == 2) break;
                    } while (intentos < 5);

                    String entrada = readln();
                    println(entrada);

                    int x = 5;
                    x += 3;
                    x -= 2;
                    x *= 2;
                    x /= 2;
                    x++;
                    x--;
                }
            }
            """;
            default -> "";
        };
    }
}

package org.nexus.nexussolairy.service;

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

        File mainPig = new File(genDir, "pig.pig");
        File mainY = new  File(genDir, "python.y");
        File mainZet = new   File(genDir, "python.z");

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
def main() -> void {
    let mut contador: int = 0;
    let mensaje: string = "Nexus Antiquus Core";
    
    while contador < 10 {
        print(mensaje);
        contador = contador + 1;
    }
    
    return;
}
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

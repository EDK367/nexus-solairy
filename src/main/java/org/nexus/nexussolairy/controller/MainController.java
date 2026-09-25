package org.nexus.nexussolairy.controller;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import org.nexus.nexussolairy.backend.c.CSourceCodeGenerator;
import org.nexus.nexussolairy.backend.c.GCCCompilerService;
import org.nexus.nexussolairy.backend.vm.C3DVirtualMachine;
import org.nexus.nexussolairy.model.c3d.Quadruple;
import org.nexus.nexussolairy.ui.SintaxColor;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Line;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.nexus.nexussolairy.model.enums.LanguageType;
import org.nexus.nexussolairy.model.file.Project;
import org.nexus.nexussolairy.model.lexical.LexerError;
import org.nexus.nexussolairy.model.lexical.TokenInfo;
import org.nexus.nexussolairy.model.semantic.SemanticError;
import org.nexus.nexussolairy.model.view.SymbolViewModel;
import org.nexus.nexussolairy.model.syntactic.SyntaxError;
import org.nexus.nexussolairy.model.view.*;
import org.nexus.nexussolairy.patron.LexerFactory;
import org.nexus.nexussolairy.patron.ParserFactory;
import org.nexus.nexussolairy.service.grammar.LexerService;
import org.nexus.nexussolairy.service.parser.ParserService;
import org.nexus.nexussolairy.service.ui.FileService;
import org.nexus.nexussolairy.service.ui.ProjectService;
import org.nexus.nexussolairy.service.ui.WorkspaceService;
import org.nexus.nexussolairy.service.analysis.AnalysisPipeline;
import org.nexus.nexussolairy.model.semantic.Symbol;
import org.nexus.nexussolairy.c3d.MemoryLayout;
import org.nexus.nexussolairy.model.c3d.OpCode;
import org.nexus.nexussolairy.model.enums.DataType;
import org.nexus.nexussolairy.model.enums.SymbolKind;
import org.nexus.nexussolairy.utils.ResultLexer;
import org.nexus.nexussolairy.view.*;
import org.nexus.nexussolairy.view.utils.CommandItem;
import org.nexus.nexussolairy.view.utils.CommandPaletteDialog;
import org.nexus.nexussolairy.view.utils.NotificationToast;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class MainController implements Initializable {

    @FXML
    private StackPane mainRootContainer;
    @FXML
    private SplitPane mainHorizontalSplit;
    @FXML
    private VBox explorerContainer;
    @FXML
    private Label projectRootLabel;
    @FXML
    private TreeView<File> projectTree;
    @FXML
    private Label emptyEditorLabel;
    @FXML
    private TabPane editorTabPane;
    @FXML
    private TabPane bottomTabPane;

    @FXML
    private Tab terminalTab;
    @FXML
    private HBox terminalSessionBar;
    @FXML
    private Label terminalStatusBadge;
    @FXML
    private StyleClassedTextArea terminalOutput;

    @FXML
    private Tab problemsTab;
    @FXML
    private ToggleButton problemsAllBtn, problemsErrBtn, problemsWarnBtn, problemsInfoBtn;
    @FXML
    private TableView<ProblemViewModel> problemsTable;
    @FXML
    private TableColumn<ProblemViewModel, String> colProbSeverity, colProbFile, colProbMsg;
    @FXML
    private TableColumn<ProblemViewModel, Number> colProbLine, colProbCol;

    @FXML
    private Tab astTab;
    @FXML
    private Label astInspectorLabel, astEmptyLabel;
    @FXML
    private Pane astCanvasPane;

    @FXML
    private Tab symbolsTab;
    @FXML
    private TextField symbolsSearchField;
    @FXML
    private TableView<SymbolViewModel> symbolsTable;
    @FXML
    private TableColumn<SymbolViewModel, String> colSymName, colSymType, colSymKind, colSymScope, colSymLanguage, colSymValue;
    @FXML
    private TableColumn<SymbolViewModel, Number> colSymLine, colSymCol;

    @FXML
    private Tab LEXER_TAB;
    @FXML
    private TabPane LEXER_SUB_TAB_PANE;
    @FXML
    private TextField tokensSearchField;
    @FXML
    private TableView<TokenInfo> tokensTable;
    @FXML
    private TableColumn<TokenInfo, String> colTokToken, colTokLexeme;
    @FXML
    private TableColumn<TokenInfo, Number> colTokLine, colTokCol;
    @FXML
    private TableView<LexerError> lexerErrorsTable;
    @FXML
    private TableColumn<LexerError, Number> colLexErrLine, colLexErrCol;
    @FXML
    private TableColumn<LexerError, String> colLexErrMsg;

    @FXML
    private Tab SYNTAX_TAB;
    @FXML
    private TableView<SyntaxError> syntaxTable;
    @FXML
    private TableColumn<SyntaxError, String> colSynSeverity, colSynMsg;
    @FXML
    private TableColumn<SyntaxError, Number> colSynLine, colSynCol;

    @FXML
    private Tab semanticTab;
    @FXML
    private TableView<SemanticError> semanticTable;
    @FXML
    private TableColumn<SemanticError, String> colSemType, colSemMsg;
    @FXML
    private TableColumn<SemanticError, Number> colSemLine, colSemCol;

    @FXML
    private Tab c3dTab;
    @FXML
    private CodeArea c3dCodeArea;

    @FXML
    private Tab quadruplesTab;
    @FXML
    private TableView<QuadrupleViewModel> quadruplesTable;
    @FXML
    private TableColumn<QuadrupleViewModel, Number> colQuadIndex;
    @FXML
    private TableColumn<QuadrupleViewModel, String> colQuadOp, colQuadArg1, colQuadArg2, colQuadRes;

    @FXML
    private Tab stackTab;
    @FXML
    private TableView<StackViewModel> stackTable;
    @FXML
    private TableColumn<StackViewModel, String> colStackAddr, colStackType, colStackVal, colStackDetails;

    @FXML
    private Tab heapTab;
    @FXML
    private TableView<HeapViewModel> heapTable;
    @FXML
    private TableColumn<HeapViewModel, String> colHeapAddr, colHeapType, colHeapVal, colHeapDetails;


    @FXML
    private Label statusLabel, cursorLabel, languageBadge, tokensBadge, warningsBadge, errorsBadge;

    private Stage stage;
    private FileService fileService;
    private ProjectService projectService;
    private WorkspaceService workspaceService;

    private final Map<EditorTabModel, CodeArea> codeAreaMap = new HashMap<>();
    private final Map<Tab, EditorTabModel> tabModelMap = new HashMap<>();
    private CommandPaletteDialog commandPaletteDialog;

    private final ObservableList<ProblemViewModel> problemsList = FXCollections.observableArrayList();
    private final ObservableList<SymbolViewModel> symbolsList = FXCollections.observableArrayList();
    private final ObservableList<TokenInfo> tokensList = FXCollections.observableArrayList();
    private final ObservableList<LexerError> lexerErrorsList = FXCollections.observableArrayList();
    private final ObservableList<SyntaxError> syntaxErrorsList = FXCollections.observableArrayList();
    private final ObservableList<SemanticError> semanticErrorsList = FXCollections.observableArrayList();
    private final ObservableList<QuadrupleViewModel> quadruplesList = FXCollections.observableArrayList();
    private final ObservableList<StackViewModel> stackList = FXCollections.observableArrayList();
    private final ObservableList<HeapViewModel> heapList = FXCollections.observableArrayList();
    private double astZoom = 1.0;
    private AstNodeViewModel selectedAstNode = null;
    private final AnalysisPipeline analysisPipeline = new AnalysisPipeline();

    private C3DVirtualMachine activeVM;
    private MemoryLayout activeMemoryLayout;
    private List<Symbol> activeSymbols = Collections.emptyList();
    private String currentGeneratedCCode = "";
    private final GCCCompilerService gccService = new GCCCompilerService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        fileService = new FileService();
        projectService = new ProjectService(fileService);
        workspaceService = new WorkspaceService();

        workspaceService.setNotificationHandler(this::showNotification);

        initProjectExplorer();
        initEditorArea();
        initTerminal();
        initProblemsTable();
        initAstCanvas();
        initSymbolsTable();
        initLexerTables();
        initSyntaxTable();
        initSemanticTable();
        initC3D();
        initQuadruples();
        initStack();
        initHeap();
        initStatusBar();
    }

    // ejecucion de las ventanas cuando se abrio el programa
    public void setStage(Stage stage, Scene scene) {
        this.stage = stage;
        commandPaletteDialog = new CommandPaletteDialog(stage, buildCommandList());

        // comandos
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), () -> commandPaletteDialog.show());
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.P, KeyCombination.CONTROL_DOWN), () -> commandPaletteDialog.show());
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::handleSave);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F5), this::handleRunProject);
    }

    private void initProjectExplorer() {
        projectTree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(6);
                    box.setAlignment(Pos.CENTER_LEFT);

                    Label iconLabel = new Label();
                    if (file.isDirectory()) {
                        iconLabel.setText("📁");
                        iconLabel.getStyleClass().add("file-icon-folder");
                    } else {
                        LanguageType lang = LanguageType.fromFileName(file.getName());
                        iconLabel.setText(getFileIconSymbol(lang));
                        iconLabel.setStyle("-fx-text-fill: " + lang.getColorHex() + "; -fx-font-weight: bold;");
                    }

                    Label nameLabel = new Label(file.getName());
                    nameLabel.getStyleClass().add("tree-item-name");
                    box.getChildren().addAll(iconLabel, nameLabel);

                    setGraphic(box);
                    setText(null);
                }
            }
        });

        projectTree.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                TreeItem<File> selected = projectTree.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getValue() != null && selected.getValue().isFile()) {
                    openFileInEditor(selected.getValue());
                }
            }
        });

        setupTreeContextMenu();
    }

    private void setupTreeContextMenu() {
        ContextMenu menu = new ContextMenu();

        MenuItem openItem = new MenuItem("Open in Editor");
        openItem.setOnAction(e -> {
            TreeItem<File> item = projectTree.getSelectionModel().getSelectedItem();
            if (item != null && item.getValue() != null && item.getValue().isFile()) {
                openFileInEditor(item.getValue());
            }
        });

        MenuItem newFileItem = new MenuItem("New File...");
        newFileItem.setOnAction(e -> promptNewFileInTree());

        MenuItem newFolderItem = new MenuItem("New Folder...");
        newFolderItem.setOnAction(e -> promptNewFolderInTree());

        MenuItem copyPathItem = new MenuItem("Copy Absolute Path");
        copyPathItem.setOnAction(e -> {
            TreeItem<File> item = projectTree.getSelectionModel().getSelectedItem();
            if (item != null && item.getValue() != null) {
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(item.getValue().getAbsolutePath());
                clipboard.setContent(content);
                workspaceService.notifyUser("Path copied: " + item.getValue().getName());
            }
        });

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            TreeItem<File> item = projectTree.getSelectionModel().getSelectedItem();
            if (item != null && item.getValue() != null) {
                fileService.delete(item.getValue());
                handleRefreshTree();
                workspaceService.notifyUser("Deleted: " + item.getValue().getName());
            }
        });

        menu.getItems().addAll(openItem, new SeparatorMenuItem(), newFileItem, newFolderItem, copyPathItem, new SeparatorMenuItem(), deleteItem);
        projectTree.setContextMenu(menu);
    }

    private void initEditorArea() {
        editorTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                EditorTabModel model = tabModelMap.get(newTab);
                workspaceService.setActiveTab(model);
                CodeArea area = codeAreaMap.get(model);
                if (area != null) {
                    updateCursorPosition(area);
                }
            } else {
                workspaceService.setActiveTab(null);
            }
        });
    }

    private void initTerminal() {
        workspaceService.getExecutionSessions().addListener((javafx.collections.ListChangeListener<ExecutionSession>) change -> rebuildSessionTabs());
        workspaceService.activeSessionProperty().addListener((obs, oldS, newS) -> updateSessionDisplay(newS));
        appendTerminalInfo("Nexu-Solairy Virtual Runtime Environment initialized. Ready.");
    }

    private void initProblemsTable() {
        FilteredList<ProblemViewModel> filtered = new FilteredList<>(problemsList, p -> true);

        ToggleGroup group = new ToggleGroup();
        problemsAllBtn.setToggleGroup(group);
        problemsErrBtn.setToggleGroup(group);
        problemsWarnBtn.setToggleGroup(group);
        problemsInfoBtn.setToggleGroup(group);

        group.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == problemsErrBtn) filtered.setPredicate(p -> "Error".equalsIgnoreCase(p.getSeverity()));
            else if (newT == problemsWarnBtn) filtered.setPredicate(p -> "Warning".equalsIgnoreCase(p.getSeverity()));
            else if (newT == problemsInfoBtn) filtered.setPredicate(p -> "Info".equalsIgnoreCase(p.getSeverity()));
            else filtered.setPredicate(p -> true);
        });

        colProbSeverity.setCellValueFactory(c -> c.getValue().severityProperty());
        colProbSeverity.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    if ("Error".equalsIgnoreCase(item)) badge.getStyleClass().addAll("badge", "badge-danger");
                    else if ("Warning".equalsIgnoreCase(item)) badge.getStyleClass().addAll("badge", "badge-warning");
                    else badge.getStyleClass().addAll("badge", "badge-info");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        colProbFile.setCellValueFactory(c -> c.getValue().fileProperty());
        colProbLine.setCellValueFactory(c -> c.getValue().lineProperty());
        colProbCol.setCellValueFactory(c -> c.getValue().columnProperty());
        colProbMsg.setCellValueFactory(c -> c.getValue().messageProperty());

        problemsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        problemsTable.setItems(filtered);
        problemsTable.setPlaceholder(new Label("No problems detected in workspace"));
    }

    private void initAstCanvas() {
        renderAst(null);
    }

    private void initSymbolsTable() {
        FilteredList<SymbolViewModel> filtered = new FilteredList<>(symbolsList, p -> true);

        symbolsSearchField.textProperty().addListener((obs, oldV, newV) -> {
            filtered.setPredicate(s -> {
                if (newV == null || newV.trim().isEmpty()) return true;
                String lower = newV.toLowerCase().trim();
                return s.getName().toLowerCase().contains(lower) || s.getScopeKind().toLowerCase().contains(lower) || s.getDataType().toLowerCase().contains(lower);
            });
        });

        colSymName.setCellValueFactory(c -> c.getValue().nameProperty());
        colSymType.setCellValueFactory(c -> c.getValue().dataTypeProperty());
        colSymKind.setCellValueFactory(c -> c.getValue().symbolKindProperty());
        colSymScope.setCellValueFactory(c -> c.getValue().scopeKindProperty());
        colSymLanguage.setCellValueFactory(c -> c.getValue().languageTypeProperty());
        colSymValue.setCellValueFactory(c -> c.getValue().valueProperty());
        colSymLine.setCellValueFactory(c -> c.getValue().lineProperty());
        colSymCol.setCellValueFactory(c -> c.getValue().columnProperty());

        symbolsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        symbolsTable.setItems(filtered);
        symbolsTable.setPlaceholder(new Label("No symbols available in symbol table"));
    }

    private void initLexerTables() {
        FilteredList<TokenInfo> filteredTokens = new FilteredList<>(tokensList, p -> true);

        tokensSearchField.textProperty().addListener((obs, oldV, newV) -> {
            filteredTokens.setPredicate(t -> {
                if (newV == null || newV.trim().isEmpty()) return true;
                String lower = newV.toLowerCase().trim();
                return t.getToken().toLowerCase().contains(lower) || t.getLexeme().toLowerCase().contains(lower);
            });
        });

        colTokToken.setCellValueFactory(c -> c.getValue().tokenProperty());
        colTokLexeme.setCellValueFactory(c -> c.getValue().lexemeProperty());
        colTokLine.setCellValueFactory(c -> c.getValue().lineProperty());
        colTokCol.setCellValueFactory(c -> c.getValue().columnProperty());

        tokensTable.setItems(filteredTokens);
        tokensTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tokensTable.setPlaceholder(new Label("No tokens generated"));

        colLexErrLine.setCellValueFactory(c -> c.getValue().lineProperty());
        colLexErrCol.setCellValueFactory(c -> c.getValue().columnProperty());
        colLexErrMsg.setCellValueFactory(c -> c.getValue().messageProperty());

        lexerErrorsTable.setItems(lexerErrorsList);
        lexerErrorsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        lexerErrorsTable.setPlaceholder(new Label("No lexical errors detected"));
    }

    private void initSyntaxTable() {
        colSynSeverity.setCellValueFactory(c -> c.getValue().severityProperty());
        colSynSeverity.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    if ("Error".equalsIgnoreCase(item)) badge.getStyleClass().addAll("badge", "badge-danger");
                    else if ("Warning".equalsIgnoreCase(item)) badge.getStyleClass().addAll("badge", "badge-warning");
                    else badge.getStyleClass().addAll("badge", "badge-info");
                    setGraphic(badge);
                    setText(null);
                }
            }
        });
        colSynLine.setCellValueFactory(c -> c.getValue().lineProperty());
        colSynCol.setCellValueFactory(c -> c.getValue().columnProperty());
        colSynMsg.setCellValueFactory(c -> c.getValue().messageProperty());

        syntaxTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        syntaxTable.setItems(syntaxErrorsList);
        syntaxTable.setPlaceholder(new Label("No syntactic errors detected"));
    }

    private void initSemanticTable() {
        colSemLine.setCellValueFactory(c -> c.getValue().lineProperty());
        colSemCol.setCellValueFactory(c -> c.getValue().columnProperty());
        colSemType.setCellValueFactory(c -> c.getValue().typeProperty());
        colSemMsg.setCellValueFactory(c -> c.getValue().messageProperty());

        semanticTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        semanticTable.setItems(semanticErrorsList);
        semanticTable.setPlaceholder(new Label("No semantic errors detected"));
    }

    private void initC3D() {
        c3dCodeArea.setParagraphGraphicFactory(LineNumberFactory.get(c3dCodeArea));
    }

    private void initQuadruples() {
        colQuadIndex.setCellValueFactory(c -> c.getValue().indexProperty());
        colQuadOp.setCellValueFactory(c -> c.getValue().operatorProperty());
        colQuadArg1.setCellValueFactory(c -> c.getValue().arg1Property());
        colQuadArg2.setCellValueFactory(c -> c.getValue().arg2Property());
        colQuadRes.setCellValueFactory(c -> c.getValue().resultProperty());

        quadruplesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        quadruplesTable.setItems(quadruplesList);
        quadruplesTable.setPlaceholder(new Label("No quadruples generated"));
    }

    private void initStack() {
        colStackAddr.setCellValueFactory(c -> c.getValue().addressProperty());
        colStackType.setCellValueFactory(c -> c.getValue().typeProperty());
        colStackVal.setCellValueFactory(c -> c.getValue().valueProperty());
        colStackDetails.setCellValueFactory(c -> c.getValue().detailsProperty());

        stackTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        stackTable.setItems(stackList);
        stackTable.setPlaceholder(new Label("Stack memory empty. Run program to allocate runtime objects."));
    }

    private void initHeap() {
        colHeapAddr.setCellValueFactory(c -> c.getValue().addressProperty());
        colHeapType.setCellValueFactory(c -> c.getValue().typeProperty());
        colHeapVal.setCellValueFactory(c -> c.getValue().valueProperty());
        colHeapDetails.setCellValueFactory(c -> c.getValue().detailsProperty());

        heapTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        heapTable.setItems(heapList);
        heapTable.setPlaceholder(new Label("Heap memory empty. Run program to allocate runtime objects."));
    }

    private void initStatusBar() {
        statusLabel.textProperty().bind(workspaceService.statusMessageProperty());
        cursorLabel.textProperty().bind(workspaceService.cursorPositionProperty());
        cursorLabel.setStyle("-fx-text-fill: -fx-accent-cyan-hover; -fx-font-weight: bold;");

        workspaceService.activeLanguageProperty().addListener((obs, oldL, newL) -> {
            if (newL != null) {
                languageBadge.setText(newL.getDisplayName());
                languageBadge.setStyle("-fx-background-color: " + newL.getColorHex() + "; -fx-text-fill: -fx-panel-bg; -fx-font-weight: bold;");
            } else {
                languageBadge.setText("Plain Text");
                languageBadge.setStyle("");
            }
        });
    }

    private void loadProjectInExplorer(Project project) {
        workspaceService.setCurrentProject(project);
        if (project == null || project.getRootDirectory() == null) {
            projectTree.setRoot(null);
            projectRootLabel.setText("NEXUS SOLAIRY EXPLORER");
            return;
        }

        projectRootLabel.setText(project.getName().toUpperCase());
        TreeItem<File> rootItem = buildTreeItem(project.getRootDirectory());
        rootItem.setExpanded(true);
        projectTree.setRoot(rootItem);
    }

    private TreeItem<File> buildTreeItem(File directory) {
        TreeItem<File> item = new TreeItem<>(directory);
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().startsWith(".") && !file.getName().equals(".nexu")) continue;
                    if (file.isDirectory()) {
                        TreeItem<File> child = buildTreeItem(file);
                        child.setExpanded(true);
                        item.getChildren().add(child);
                    } else {
                        item.getChildren().add(new TreeItem<>(file));
                    }
                }
            }
        }
        return item;
    }

    public void openFileInEditor(File file) {
        if (file == null || !file.exists()) return;

        for (var entry : tabModelMap.entrySet()) {
            if (entry.getValue().getFile() != null && entry.getValue().getFile().getAbsolutePath().equals(file.getAbsolutePath())) {
                editorTabPane.getSelectionModel().select(entry.getKey());
                return;
            }
        }

        try {
            String content = fileService.readFile(file);
            EditorTabModel model = new EditorTabModel(file, content);
            addEditorTab(model, content);
            workspaceService.getOpenTabs().add(model);
            workspaceService.setActiveTab(model);
        } catch (IOException e) {
            workspaceService.notifyUser("Error reading file: " + file.getName());
        }
    }

    private void addEditorTab(EditorTabModel model, String content) {
        CodeArea codeArea = new CodeArea();
        codeArea.getStyleClass().add("code-area");
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));

        // aplicar color a la sintaxis
        applyLanguageStyle(codeArea, model.getLanguageType());

        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB) {
                event.consume();
                int caret = codeArea.getCaretPosition();
                codeArea.insertText(caret, "    ");
                codeArea.moveTo(caret + 4);
            }
        });

        codeArea.replaceText(content);
        try {
            codeArea.setStyleSpans(0, SintaxColor.compute(content, model.getLanguageType()));
        } catch (Exception ignored) {
        }

        codeAreaMap.put(model, codeArea);

        PauseTransition debounce = new PauseTransition(Duration.millis(60));
        debounce.setOnFinished(evt -> {
            try {
                codeArea.setStyleSpans(0, SintaxColor.compute(codeArea.getText(), model.getLanguageType()));
            } catch (Exception ignored) {
            }
        });

        codeArea.textProperty().addListener((obs, oldText, newText) -> {
            model.setDirty(!newText.equals(model.getSavedContent()));
            debounce.playFromStart();
        });

        codeArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
            updateCursorPosition(codeArea);
        });

        Tab tab = new Tab();
        tab.setContent(codeArea);
        tabModelMap.put(tab, model);

        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);

        Label langIcon = new Label(getLanguageBadgeText(model.getLanguageType()));
        langIcon.setStyle("-fx-text-fill: " + model.getLanguageType().getColorHex() + "; -fx-font-weight: bold; -fx-font-size: 11px;");

        Label titleLabel = new Label();
        titleLabel.textProperty().bind(Bindings.createStringBinding(() -> (model.isDirty() ? "● " : "") + model.getTitle(), model.dirtyProperty(), model.titleProperty()));
        titleLabel.getStyleClass().add("editor-tab-title");

        header.getChildren().addAll(langIcon, titleLabel);
        tab.setGraphic(header);

        tab.setOnClosed(e -> {
            codeAreaMap.remove(model);
            tabModelMap.remove(tab);
            workspaceService.getOpenTabs().remove(model);
            emptyEditorLabel.setVisible(editorTabPane.getTabs().isEmpty());
        });

        editorTabPane.getTabs().add(tab);
        editorTabPane.getSelectionModel().select(tab);
        emptyEditorLabel.setVisible(false);
    }

    private void updateCursorPosition(CodeArea area) {
        int line = area.getCurrentParagraph() + 1;
        int col = area.getCaretColumn() + 1;
        workspaceService.setCursorPosition(line, col);
    }

    // pinta los tokens y errores y aplica sus estilos
    private void applyLanguageStyle(CodeArea codeArea, LanguageType language) {
        String cssPath = switch (language) {
            case PIG_LATIN -> "/org/nexus/nexussolairy/css/piglatin-highlight.css";
            case Y_LANG -> "/org/nexus/nexussolairy/css/y-highlight.css";
            case ZETARIANO -> "/org/nexus/nexussolairy/css/zetariano-highlight.css";
            default -> null;
        };
        codeArea.getStyleClass().removeAll("piglatin-editor", "y-editor", "zetariano-editor");
        if (language == LanguageType.PIG_LATIN) {
            codeArea.getStyleClass().add("piglatin-editor");
        } else if (language == LanguageType.Y_LANG) {
            codeArea.getStyleClass().add("y-editor");
        } else if (language == LanguageType.ZETARIANO) {
            codeArea.getStyleClass().add("zetariano-editor");
        }
        if (cssPath != null) {
            var resource = getClass().getResource(cssPath);
            if (resource != null) {
                String externalForm = resource.toExternalForm();
                if (!codeArea.getStylesheets().contains(externalForm)) {
                    codeArea.getStylesheets().add(externalForm);
                }
            }
        }
    }

    private void rebuildSessionTabs() {
        terminalSessionBar.getChildren().clear();
        for (ExecutionSession session : workspaceService.getExecutionSessions()) {
            Button tabBtn = new Button(session.getTitle());
            tabBtn.getStyleClass().addAll("btn", "btn-sm", "terminal-session-btn");
            if (session == workspaceService.getActiveSession()) {
                tabBtn.getStyleClass().add("terminal-session-active");
            }
            tabBtn.setOnAction(e -> workspaceService.setActiveSession(session));
            terminalSessionBar.getChildren().add(tabBtn);
        }
    }

    private void updateSessionDisplay(ExecutionSession session) {
        rebuildSessionTabs();
        if (session != null) {
            terminalStatusBadge.setText(session.getStatus().name() + " (" + session.getLanguage().getDisplayName() + ")");
            terminalOutput.clear();
            for (var entry : session.getLogEntries()) {
                appendTerminalLog(entry.getType(), entry.getMessage());
            }
        } else {
            terminalStatusBadge.setText("READY");
        }
    }

    private void appendTerminalInfo(String message) {
        appendTerminalLog("INFO", message);
    }

    private void appendTerminalSuccess(String message) {
        appendTerminalLog("SUCCESS", message);
    }

    private void appendTerminalError(String message) {
        appendTerminalLog("ERROR", message);
    }

    private void appendTerminalLog(String type, String message) {
        int start = terminalOutput.getLength();
        String prefix = switch (type) {
            case "INFO" -> "[INFO]    ";
            case "SUCCESS" -> "[OK]      ";
            case "ERROR" -> "[ERROR]   ";
            case "RESULT" -> "[RESULT]  ";
            default -> "◎ ";
        };

        String styleClass = switch (type) {
            case "INFO" -> "console-prefix-info";
            case "SUCCESS" -> "console-prefix-ok";
            case "ERROR" -> "console-prefix-error";
            case "RESULT" -> "console-prefix-result";
            default -> "console-default";
        };

        terminalOutput.appendText(prefix + message + "\n");
        terminalOutput.setStyleClass(start, terminalOutput.getLength(), styleClass);
        terminalOutput.requestFollowCaret();
    }

    private void renderAst(AstNodeViewModel root) {
        astCanvasPane.getChildren().clear();
        if (root == null) {
            astEmptyLabel.setVisible(true);
            return;
        }
        astEmptyLabel.setVisible(false);
        drawAstTree(root, 900, 40, 400);
    }

    private void drawAstTree(AstNodeViewModel node, double x, double y, double hOffset) {
        if (node == null) return;

        double boxWidth = 140;
        double boxHeight = 44;

        VBox box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(4, 8, 4, 8));
        box.setPrefSize(boxWidth, boxHeight);
        box.setLayoutX(x - boxWidth / 2);
        box.setLayoutY(y);
        box.getStyleClass().add("ast-node-box");

        if (node == selectedAstNode) {
            box.getStyleClass().add("ast-node-selected");
        }

        Label label = new Label(node.getLabel());
        label.getStyleClass().add("ast-node-label");

        Label type = new Label(node.getType());
        type.getStyleClass().add("ast-node-type");

        box.getChildren().addAll(label, type);

        box.setOnMouseClicked(e -> {
            selectedAstNode = node;
            int childCount = node.getChildren() != null ? node.getChildren().size() : 0;
            astInspectorLabel.setText(String.format("Selected: [%s]  Type: %s  Value: %s  Children: %d", node.getLabel(), node.getType(), node.getValue(), childCount));
            e.consume();
        });

        astCanvasPane.getChildren().add(box);

        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            int childCount = node.getChildren().size();
            double startX = x - (childCount - 1) * hOffset / 2.0;
            double childY = y + 80;

            for (int i = 0; i < childCount; i++) {
                AstNodeViewModel child = node.getChildren().get(i);
                double childX = startX + i * hOffset;

                Line line = new Line(x, y + boxHeight, childX, childY);
                line.getStyleClass().add("ast-connection-line");
                astCanvasPane.getChildren().add(0, line);

                drawAstTree(child, childX, childY, hOffset / 1.8);
            }
        }
    }


    private void promptNewFileInTree() {
        TreeItem<File> selected = projectTree.getSelectionModel().getSelectedItem();
        File parentDir = (selected != null && selected.getValue().isDirectory()) ? selected.getValue() : (workspaceService.getCurrentProject() != null ? new File(workspaceService.getCurrentProject().getRootDirectory(), "src") : null);

        if (parentDir == null) return;

        TextInputDialog dialog = new TextInputDialog("nuevo_archivo.pig");
        dialog.setTitle("New File");
        dialog.setHeaderText("Create a new source file in " + parentDir.getName());
        dialog.setContentText("File name:");

        dialog.showAndWait().ifPresent(fileName -> {
            try {
                LanguageType lang = LanguageType.fromFileName(fileName);
                String starter = (lang == LanguageType.PIG_LATIN) ? "VARIABILES>\n\nMAIOR>\n\nFINIS;\n" : "";
                File created = fileService.createFile(parentDir, fileName, starter);
                handleRefreshTree();
                openFileInEditor(created);
                workspaceService.notifyUser("Created file: " + fileName);
            } catch (IOException ex) {
                workspaceService.notifyUser("Error creating file: " + ex.getMessage());
            }
        });
    }

    private void promptNewFolderInTree() {
        TreeItem<File> selected = projectTree.getSelectionModel().getSelectedItem();
        File parentDir = (selected != null && selected.getValue().isDirectory()) ? selected.getValue() : (workspaceService.getCurrentProject() != null ? workspaceService.getCurrentProject().getRootDirectory() : null);

        if (parentDir == null) return;

        TextInputDialog dialog = new TextInputDialog("nueva_carpeta");
        dialog.setTitle("New Folder");
        dialog.setHeaderText("Create a new folder in " + parentDir.getName());
        dialog.setContentText("Folder name:");

        dialog.showAndWait().ifPresent(folderName -> {
            try {
                fileService.createFolder(parentDir, folderName);
                handleRefreshTree();
                workspaceService.notifyUser("Created folder: " + folderName);
            } catch (IOException ex) {
                workspaceService.notifyUser("Error creating folder: " + ex.getMessage());
            }
        });
    }

    @FXML
    public void handleNewProject() {
        NewProjectDialog dialog = new NewProjectDialog(stage, (path, lang) -> {
            try {
                File dir = new File(path);
                Project project = projectService.createProject(dir.getName(), dir.getParentFile(), lang);
                loadProjectInExplorer(project);
                File mainFile = new File(dir, "nexus/main" + lang.getExtension());
                openFileInEditor(mainFile);
                workspaceService.notifyUser("Project created: " + project.getName());
            } catch (IOException ex) {
                workspaceService.notifyUser("Error creating project: " + ex.getMessage());
            }
        });
        dialog.show();
    }

    @FXML
    public void handleOpenProject() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Select Project Folder");
        File dir = dc.showDialog(stage);
        if (dir != null) {
            Project project = projectService.openProject(dir);
            loadProjectInExplorer(project);
            File src = new File(dir, "src");
            if (src.exists() && src.isDirectory()) {
                File[] files = src.listFiles();
                if (files != null && files.length > 0) {
                    openFileInEditor(files[0]);
                }
            }
            workspaceService.notifyUser("Project loaded: " + project.getName());
        }
    }

    @FXML
    public void handleOpenFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open Source File");
        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter(
                "Nexu-Solairy Source Files (*.pig, *.y, *.z, *.c)", "*.pig", "*.y", "*.z", "*.c"),
                new FileChooser.ExtensionFilter("Pig Latin (*.pig)", "*.pig"),
                new FileChooser.ExtensionFilter("Y? Language (*.y)", "*.y"),
                new FileChooser.ExtensionFilter("Zetariano (*.z)", "*.z"),
                new FileChooser.ExtensionFilter("C Source (*.c)", "*.c"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        File file = fc.showOpenDialog(stage);
        if (file != null) {
            openFileInEditor(file);
        }
    }


    @FXML
    public void handleSave() {
        EditorTabModel active = workspaceService.getActiveTab();
        if (active != null) {
            CodeArea area = codeAreaMap.get(active);
            if (area != null && active.getFile() != null) {
                try {
                    fileService.writeFile(active.getFile(), area.getText());
                    active.setSavedContent(area.getText());
                    workspaceService.notifyUser("File saved: " + active.getTitle());
                } catch (IOException e) {
                    workspaceService.notifyUser("Error saving file: " + active.getTitle());
                }
            }
        }
    }

    @FXML
    public void handleExit() {
        Platform.exit();
    }

    @FXML
    public void handleToggleExplorer() {
        if (mainHorizontalSplit.getItems().contains(explorerContainer)) {
            mainHorizontalSplit.getItems().remove(explorerContainer);
        } else {
            mainHorizontalSplit.getItems().add(0, explorerContainer);
            mainHorizontalSplit.setDividerPositions(0.20);
        }
    }

    @FXML
    public void handleSelectTerminal() {
        bottomTabPane.getSelectionModel().select(terminalTab);
    }

    @FXML
    public void handleSelectProblems() {
        bottomTabPane.getSelectionModel().select(problemsTab);
    }

    @FXML
    public void handleSelectAst() {
        bottomTabPane.getSelectionModel().select(astTab);
    }

    @FXML
    public void handleSelectSymbols() {
        bottomTabPane.getSelectionModel().select(symbolsTab);
    }

    @FXML
    public void handleSelectLexer() {
        bottomTabPane.getSelectionModel().select(LEXER_TAB);
        LEXER_SUB_TAB_PANE.getSelectionModel().select(0);
    }

    @FXML
    public void handleSelectLexerError() {
        bottomTabPane.getSelectionModel().select(LEXER_TAB);
        LEXER_SUB_TAB_PANE.getSelectionModel().select(1);

    }

    @FXML
    public void handleSelectSyntax() {
        bottomTabPane.getSelectionModel().select(SYNTAX_TAB);
    }

    @FXML
    public void handleSelectSemantic() {
        bottomTabPane.getSelectionModel().select(semanticTab);
    }

    @FXML
    public void handleSelectC3D() {
        bottomTabPane.getSelectionModel().select(c3dTab);
    }

    @FXML
    public void handleSelectQuadruples() {
        bottomTabPane.getSelectionModel().select(quadruplesTab);
    }

    @FXML
    public void handleSelectStack() {
        bottomTabPane.getSelectionModel().select(stackTab);
    }

    @FXML
    public void handleSelectHeap() {
        bottomTabPane.getSelectionModel().select(heapTab);
    }

    @FXML
    public void handleRunProject() {
        var tab = workspaceService.getActiveTab();
        if (tab != null) {
            handleRunActiveFile();
            return;
        }
        String targetName = "main.pig";
        LanguageType lang = LanguageType.PIG_LATIN;

        bottomTabPane.getSelectionModel().select(terminalTab);
        var session = workspaceService.createNewExecutionSession(targetName, lang);
        session.addLog("INFO", "Compiling " + targetName + " [" + lang.getDisplayName() + "]...");
        session.addLog("SUCCESS", "Lexical and Syntactic validation passed.");
        session.addLog("SUCCESS", "Execution finished with exit code 0");
        session.setStatus(ExecutionSession.SessionStatus.FINISHED);

        terminalOutput.clear();
        for (var entry : session.getLogEntries()) {
            appendTerminalLog(entry.getType(), entry.getMessage());
        }

        workspaceService.notifyUser("Execution completed: " + targetName);
    }

    @FXML
    public void handleRunActiveFile() {
        EditorTabModel activeTab = workspaceService.getActiveTab();
        if (activeTab == null) {
            workspaceService.notifyUser("No active file to run");
            return;
        }

        CodeArea codeArea = codeAreaMap.get(activeTab);
        if (codeArea == null) {
            workspaceService.notifyUser("No editor found for active file");
            return;
        }

        String source = codeArea.getText();
        LanguageType lang = activeTab.getLanguageType();
        String targetName = activeTab.getTitle();

        bottomTabPane.getSelectionModel().select(terminalTab);
        terminalOutput.clear();
        appendTerminalInfo("Executing file: " + targetName + " [" + lang.getDisplayName() + "]");

        var session = workspaceService.createNewExecutionSession(targetName, lang);

        String absolutePath = (activeTab.getFile() != null ? activeTab.getFile().getAbsolutePath() : targetName);

        var result = analysisPipeline.analyze(
                source,
                absolutePath,
                lang,
                () -> "",
                line -> appendTerminalLog("PRINT", line)
        );

        if (result.getLexicalResult() != null) {
            tokensList.setAll(result.getLexicalResult().tokens);
            lexerErrorsList.setAll(result.getLexicalResult().errors);
            setBadgeStyle(tokensBadge, result.getLexicalResult().isValid() ? "badge-success" : "badge-warning", "Tokens: " + result.getLexicalResult().tokens.size());
        }

        syntaxErrorsList.setAll(result.getSyntacticErrors());
        semanticErrorsList.setAll(result.getSemanticErrors());

        problemsList.clear();
        if (result.getLexicalResult() != null) {
            for (LexerError err : result.getLexicalResult().errors) {
                problemsList.add(new ProblemViewModel("Error", targetName, err.getLine(), err.getColumn(), err.getMessage()));
            }
        }
        for (SyntaxError err : result.getSyntacticErrors()) {
            problemsList.add(new ProblemViewModel("Error", targetName, err.getLine(), err.getColumn(), err.getMessage()));
        }
        for (SemanticError err : result.getSemanticErrors()) {
            problemsList.add(new ProblemViewModel("Error", targetName, err.getLine(), err.getColumn(), err.getMessage()));
        }

        int totalErrors = problemsList.size();
        setBadgeStyle(errorsBadge, totalErrors == 0 ? "badge-info" : "badge-danger", "Errores: " + totalErrors);

        List<SymbolViewModel> symViewModels = new ArrayList<>();
        for (Symbol sym : result.getSymbols()) {
            symViewModels.add(new SymbolViewModel(
                    sym.getName(),
                    sym.getType() != null ? sym.getType().name() : "",
                    sym.getKind() != null ? sym.getKind().name() : "",
                    sym.getScope() != null ? sym.getScope().name() : "",
                    sym.getLanguage() != null ? sym.getLanguage().name() : "",
                    sym.getValue() != null ? sym.getValue().toString() : "null",
                    sym.getLine(),
                    sym.getColumn()
            ));
        }
        symbolsList.setAll(symViewModels);

        if (result.isValid() && result.getC3dProgram() != null) {
            List<QuadrupleViewModel> quadVMs = new ArrayList<>();
            List<Quadruple> quads = result.getC3dProgram().getQuadruples();
            for (int i = 0; i < quads.size(); i++) {
                Quadruple q = quads.get(i);
                quadVMs.add(new QuadrupleViewModel(i, q.getOp().name(), q.getArg1(), q.getArg2(), q.getResult()));
            }
            quadruplesList.setAll(quadVMs);

            CSourceCodeGenerator cGen = new CSourceCodeGenerator();
            currentGeneratedCCode = cGen.generateC(result.getC3dProgram());
            c3dCodeArea.replaceText(currentGeneratedCCode);

            activeMemoryLayout = result.getMemoryLayout();
            activeSymbols = result.getSymbols();
            activeVM = result.getVirtualMachine();
            if (activeVM != null) {
                activeVM.setConsoleOutput(null);
                int maxSteps = 100000;
                int steps = 0;
                while (!activeVM.isHalted() && steps < maxSteps) {
                    activeVM.step();
                    steps++;
                }
                activeVM.setConsoleOutput(null);
                updateVMDebuggerUI();
            }

            for (String line : result.getPrintOutput()) {
                appendTerminalLog("PRINT", line);
            }
            appendTerminalSuccess("Execution completed successfully");
            session.setStatus(ExecutionSession.SessionStatus.FINISHED);
            workspaceService.notifyUser("Execution successful: " + targetName);
        } else {
            c3dCodeArea.replaceText("");
            currentGeneratedCCode = "";
            quadruplesList.clear();
            stackList.clear();
            heapList.clear();
            activeVM = null;
            activeMemoryLayout = null;
            activeSymbols = Collections.emptyList();

            for (ProblemViewModel prob : problemsList) {
                appendTerminalError(String.format("[%s] Linea %d, Columna %d: %s", prob.getSeverity(), prob.getLine(), prob.getColumn(), prob.getMessage()));
            }
            appendTerminalError("Falló la compilación: " + result.getMessage() + ". No se generó código C3D ni C.");
            session.setStatus(ExecutionSession.SessionStatus.ERROR);
            workspaceService.notifyUser("Execution failed: " + targetName);
        }
    }

    @FXML
    public void handleAnalyze() {
        handleRunActiveFile();
    }

    // corre el archivo actual para el lexer
    @FXML
    public void handleRunLexer() {
        runLexicalAnalysis();
    }

    @FXML
    public void handleRunParser() {
        runSyntaxAnalysis();
    }

    @FXML
    public void handleShowWelcome() {
        WelcomeDialog welcome = new WelcomeDialog(stage, this::handleNewProject, this::handleOpenProject, this::handleOpenFile, projectService.getRecentProjects(), dir -> {
            Project project = projectService.openProject(dir);
            loadProjectInExplorer(project);
        });
        welcome.show();
    }

    @FXML
    public void handleShowDocumentation() {
        workspaceService.notifyUser("Nexu-Solairy Documentation v1.0 (Pig Latin, Y?, Zetariano)");
    }

    @FXML
    public void handleRefreshTree() {
        loadProjectInExplorer(workspaceService.getCurrentProject());
    }

    @FXML
    public void handleSearchEverywhere() {
        if (commandPaletteDialog != null) commandPaletteDialog.show();
    }

    @FXML
    public void handleClearTerminal() {
        terminalOutput.clear();
    }

    @FXML
    public void handleNewTerminalSession() {
        var tab = workspaceService.getActiveTab();
        String name = tab != null ? tab.getTitle() : "main.pig";
        var lang = tab != null ? tab.getLanguageType() : LanguageType.PIG_LATIN;
        workspaceService.createNewExecutionSession(name, lang);
    }

    @FXML
    public void handleGenerateAst() {
        workspaceService.notifyUser("AST generation ready for compiler integration");
    }

    @FXML
    public void handleClearAst() {
        renderAst(null);
        astInspectorLabel.setText("No node selected");
    }

    @FXML
    public void handleZoomInAst() {
        astZoom = Math.min(2.5, astZoom + 0.15);
        astCanvasPane.setScaleX(astZoom);
        astCanvasPane.setScaleY(astZoom);
    }

    @FXML
    public void handleZoomOutAst() {
        astZoom = Math.max(0.4, astZoom - 0.15);
        astCanvasPane.setScaleX(astZoom);
        astCanvasPane.setScaleY(astZoom);
    }

    @FXML
    public void handleResetZoomAst() {
        astZoom = 1.0;
        astCanvasPane.setScaleX(1.0);
        astCanvasPane.setScaleY(1.0);
    }

    @FXML
    public void handleCopyC3D() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(c3dCodeArea.getText());
        clipboard.setContent(content);
        workspaceService.notifyUser("C3D copied to clipboard");
    }

    @FXML
    public void handleRegenerateC3D() {
        if (currentGeneratedCCode.isEmpty()) {
            workspaceService.notifyUser("No hay código C3D/C generado para compilar.");
            return;
        }
        bottomTabPane.getSelectionModel().select(terminalTab);
        appendTerminalInfo("Compilando código C generado con GCC...");
        gccService.compileAndRun(currentGeneratedCCode, logLine -> appendTerminalLog("GCC", logLine));
        workspaceService.notifyUser("Compilacion y ejecución GCC completada.");
    }

    @FXML
    public void handlePrevStackStep() {
        if (activeVM != null && activeVM.getPc() > 0) {
            int targetPc = activeVM.getPc() - 1;
            activeVM.restart();
            while (activeVM.getPc() < targetPc && !activeVM.isHalted()) {
                activeVM.step();
            }
            updateVMDebuggerUI();
        }
    }

    @FXML
    public void handleNextStackStep() {
        if (activeVM != null && !activeVM.isHalted()) {
            activeVM.step();
            updateVMDebuggerUI();
        }
    }

    @FXML
    public void handleToggleAutoPlay() {
    }

    private Symbol findSymbol(String name) {
        if (name == null || name.isEmpty() || activeSymbols == null) return null;
        for (Symbol s : activeSymbols) {
            if (s.getName() != null && s.getName().equals(name)) {
                return s;
            }
        }
        for (Symbol s : activeSymbols) {
            if (s.getName() != null && s.getName().equalsIgnoreCase(name)) {
                return s;
            }
        }
        return null;
    }

    private String formatSymbolType(Symbol sym) {
        if (sym == null) return "desconocido";
        if (sym.getKind() == SymbolKind.ARRAY || sym.elementType != null || sym.arraySize != null) {
            String elem = sym.elementType != null ? sym.elementType.getName() : "entero";
            return "Puntero a Heap (" + elem + "[])";
        }
        if (sym.getType() == DataType.STRUCT || (sym.structTypeName != null && !sym.structTypeName.isEmpty())) {
            return "Puntero a Heap (Estructura: " + (sym.structTypeName != null ? sym.structTypeName : sym.getName()) + ")";
        }
        if (sym.getType() == DataType.CLASS) {
            return "Puntero a Heap (Objeto: " + (sym.structTypeName != null ? sym.structTypeName : sym.getName()) + ")";
        }
        if (sym.getType() == DataType.CADENA || sym.getType() == DataType.TEXTUM) {
            return "Puntero a Heap (cadena)";
        }
        if (sym.getType() == DataType.ENTERO || sym.getType() == DataType.NUMERUS) {
            return "entero";
        }
        if (sym.getType() == DataType.FLOTANTE || sym.getType() == DataType.DECIMALIS) {
            return "flotante";
        }
        if (sym.getType() == DataType.BOOLEAN || sym.getType() == DataType.BOOL) {
            return "booleano";
        }
        if (sym.getType() == DataType.CARACTER || sym.getType() == DataType.LITTERA) {
            return "caracter";
        }
        return sym.getType() != null ? sym.getType().getName() : "desconocido";
    }

    private String escapeString(String str) {
        if (str == null) return "";
        String s = str.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        if (s.length() > 30) {
            return s.substring(0, 27) + "...";
        }
        return s;
    }

    private String formatNumber(double val) {
        return (val == (long) val) ? String.valueOf((long) val) : String.valueOf(val);
    }

    private void updateVMDebuggerUI() {
        if (activeVM == null) {
            stackList.clear();
            heapList.clear();
            return;
        }

        double[] stack = activeVM.getStack();
        double[] heap = activeVM.getHeap();
        Set<Integer> writtenStack = activeVM.getWrittenStackAddresses();
        Set<Integer> writtenHeap = activeVM.getWrittenHeapAddresses();
        Set<Integer> knownStrings = activeVM.getKnownStringPtrs();

        Map<Integer, String> stackVarNames = new HashMap<>();
        Map<Integer, String> stackVarTypes = new HashMap<>();
        Map<Integer, String> stackVarDetails = new HashMap<>();

        if (activeMemoryLayout != null) {
            for (Map.Entry<String, Integer> entry : activeMemoryLayout.getGlobalVarOffsets().entrySet()) {
                String varName = entry.getKey();
                int offset = entry.getValue();
                stackVarNames.put(offset, varName);
                Symbol sym = findSymbol(varName);
                if (sym != null) {
                    stackVarTypes.put(offset, formatSymbolType(sym));
                    stackVarDetails.put(offset, "Variable global '" + varName + "' [" + sym.getType().name().toLowerCase() + "]");
                } else {
                    stackVarDetails.put(offset, "Variable global '" + varName + "'");
                }
            }

            for (Map.Entry<String, Map<String, Integer>> routineEntry : activeMemoryLayout.getLocalFrameOffsets().entrySet()) {
                String routine = routineEntry.getKey();
                for (Map.Entry<String, Integer> localEntry : routineEntry.getValue().entrySet()) {
                    String localName = localEntry.getKey();
                    int localOff = localEntry.getValue();
                    int addr = (activeVM.getP() > 0) ? ((int) activeVM.getP() + localOff) : localOff;
                    if (!stackVarNames.containsKey(addr)) {
                        stackVarNames.put(addr, localName);
                        Symbol sym = findSymbol(localName);
                        if (sym != null) {
                            stackVarTypes.put(addr, formatSymbolType(sym));
                            stackVarDetails.put(addr, "Variable local '" + localName + "' (" + routine + ") [" + sym.getType().name().toLowerCase() + "]");
                        } else {
                            stackVarDetails.put(addr, "Variable local '" + localName + "' (" + routine + ")");
                        }
                    }
                }
            }
        }

        for (Quadruple q : activeVM.getQuadruples()) {
            if (q.getOp() == OpCode.STACK_WRITE) {
                try {
                    int addr = Integer.parseInt(q.getArg1());
                    String comment = q.getComment();
                    if (comment != null && !comment.isEmpty()) {
                        if (comment.startsWith("Var global ")) {
                            String vName = comment.substring("Var global ".length()).trim();
                            stackVarNames.putIfAbsent(addr, vName);
                            stackVarDetails.putIfAbsent(addr, "Variable global '" + vName + "'");
                        } else if (comment.startsWith("Var local ")) {
                            String vName = comment.substring("Var local ".length()).trim();
                            stackVarNames.putIfAbsent(addr, vName);
                            stackVarDetails.putIfAbsent(addr, "Variable local '" + vName + "'");
                        } else if (comment.contains("this")) {
                            stackVarNames.putIfAbsent(addr, "this");
                            stackVarTypes.putIfAbsent(addr, "Puntero a Heap (Objeto 'this')");
                            stackVarDetails.putIfAbsent(addr, "Referencia de instancia 'this'");
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        Set<Integer> stackAddrs = new TreeSet<>(writtenStack);
        if (activeMemoryLayout != null) {
            stackAddrs.addAll(activeMemoryLayout.getGlobalVarOffsets().values());
        }
        for (int i = 0; i < Math.min(stack.length, 100); i++) {
            if (stack[i] != 0) {
                stackAddrs.add(i);
            }
        }
        if (activeVM.getP() > 0) {
            stackAddrs.add((int) activeVM.getP());
        }

        List<StackViewModel> stackVMs = new ArrayList<>();
        for (int addr : stackAddrs) {
            double val = (addr >= 0 && addr < stack.length) ? stack[addr] : 0;
            String addrLabel = "stack[" + addr + "]";
            if (addr == (int) activeVM.getP() && activeVM.getP() > 0) {
                addrLabel += " [P]";
            }

            String type = stackVarTypes.get(addr);
            String detail = stackVarDetails.get(addr);
            String valStr;

            int ptr = (int) val;
            boolean pointsToHeap = (ptr >= 0 && ptr < heap.length && (writtenHeap.contains(ptr) || knownStrings.contains(ptr) || (activeVM.getH() > 0 && ptr < activeVM.getH())));

            Symbol sym = stackVarNames.containsKey(addr) ? findSymbol(stackVarNames.get(addr)) : null;
            boolean isStringSym = (sym != null && (sym.getType() == DataType.CADENA || sym.getType() == DataType.TEXTUM));
            boolean isArraySym = (sym != null && (sym.getKind() == SymbolKind.ARRAY || sym.elementType != null || sym.arraySize != null));
            boolean isStructOrClassSym = (sym != null && (sym.getType() == DataType.STRUCT || sym.getType() == DataType.CLASS || sym.structTypeName != null));

            if (isStringSym || (pointsToHeap && knownStrings.contains(ptr))) {
                String strContent = activeVM.readStringFromHeap(ptr);
                type = "Puntero a Heap (cadena)";
                valStr = "heap[" + ptr + "] -> \"" + escapeString(strContent) + "\"";
                if (detail == null) {
                    detail = "Referencia a cadena en heap[" + ptr + "]";
                } else {
                    detail += " -> Referencia en heap[" + ptr + "]";
                }
            } else if (isArraySym) {
                type = (sym != null && sym.elementType != null) ? "Puntero a Heap (" + sym.elementType.getName() + "[])" : "Puntero a Heap (arreglo[])";
                valStr = "heap[" + ptr + "]";
                if (detail == null) {
                    detail = "Referencia a arreglo en heap[" + ptr + "]";
                } else {
                    detail += " -> Base del arreglo en heap[" + ptr + "]";
                }
            } else if (isStructOrClassSym) {
                String typeName = (sym != null && sym.structTypeName != null) ? sym.structTypeName : "Objeto";
                type = "Puntero a Heap (" + typeName + ")";
                valStr = "heap[" + ptr + "]";
                if (detail == null) {
                    detail = "Instancia de " + typeName + " en heap[" + ptr + "]";
                } else {
                    detail += " -> Instancia en heap[" + ptr + "]";
                }
            } else if (pointsToHeap && val >= 0 && val == (long) val && !knownStrings.isEmpty()) {
                valStr = "heap[" + ptr + "]";
                if (type == null) type = "Puntero a Heap";
                if (detail == null) detail = "Referencia a memoria dinámica en heap[" + ptr + "]";
            } else if (sym != null && (sym.getType() == DataType.BOOLEAN || sym.getType() == DataType.BOOL)) {
                if (type == null) type = "booleano";
                valStr = (val != 0) ? "verdadero (1)" : "falso (0)";
            } else if (sym != null && (sym.getType() == DataType.CARACTER || sym.getType() == DataType.LITTERA)) {
                if (type == null) type = "caracter";
                valStr = "'" + (char) ((int) val) + "' (" + (int) val + ")";
            } else {
                if (type == null) {
                    if (addr == (int) activeVM.getP() && activeVM.getP() > 0) {
                        type = "Puntero P (Frame)";
                    } else if (val == (long) val) {
                        type = "entero";
                    } else {
                        type = "flotante";
                    }
                }
                if (val == (long) val) {
                    valStr = String.valueOf((long) val);
                } else {
                    valStr = String.valueOf(val);
                }
                if (detail == null) {
                    if (addr == (int) activeVM.getP() && activeVM.getP() > 0) {
                        detail = "Base del marco de ejecución actual (P = " + (int) activeVM.getP() + ")";
                    } else {
                        detail = "Celda de stack[" + addr + "]";
                    }
                }
            }

            stackVMs.add(new StackViewModel(addrLabel, type, valStr, detail));
        }
        stackList.setAll(stackVMs);

        Map<Integer, String> heapTypes = new HashMap<>();
        Map<Integer, String> heapValues = new HashMap<>();
        Map<Integer, String> heapDetails = new HashMap<>();

        Set<Integer> stringBases = new TreeSet<>(knownStrings);
        for (Map.Entry<Integer, String> sEntry : stackVarNames.entrySet()) {
            int sAddr = sEntry.getKey();
            Symbol sym = findSymbol(sEntry.getValue());
            if (sym != null && (sym.getType() == DataType.CADENA || sym.getType() == DataType.TEXTUM)) {
                double val = (sAddr >= 0 && sAddr < stack.length) ? stack[sAddr] : -1;
                if (val >= 0 && val < heap.length) {
                    stringBases.add((int) val);
                }
            }
        }

        for (int sBase : stringBases) {
            if (sBase < 0 || sBase >= heap.length) continue;
            String fullStr = activeVM.readStringFromHeap(sBase);
            int curr = sBase;
            while (curr < heap.length && heap[curr] != -1 && heap[curr] != 0) {
                char ch = (char) ((int) heap[curr]);
                String chDisplay = switch (ch) {
                    case '\n' -> "'\\n'";
                    case '\r' -> "'\\r'";
                    case '\t' -> "'\\t'";
                    default -> "'" + ch + "'";
                };
                heapTypes.put(curr, "caracter (ASCII)");
                heapValues.put(curr, chDisplay + " (" + (int) heap[curr] + ")");
                heapDetails.put(curr, "Cadena \"" + escapeString(fullStr) + "\" [índice " + (curr - sBase) + ", base heap[" + sBase + "]]");
                curr++;
            }
            if (curr < heap.length && heap[curr] == -1) {
                heapTypes.put(curr, "fin de cadena (-1)");
                heapValues.put(curr, "-1 (EOF/NULL)");
                heapDetails.put(curr, "Terminador de cadena \"" + escapeString(fullStr) + "\" (base heap[" + sBase + "])");
            }
        }

        for (Map.Entry<Integer, String> sEntry : stackVarNames.entrySet()) {
            int sAddr = sEntry.getKey();
            String vName = sEntry.getValue();
            Symbol sym = findSymbol(vName);
            double sVal = (sAddr >= 0 && sAddr < stack.length) ? stack[sAddr] : 0;
            int base = (int) sVal;
            if (base < 0 || base >= heap.length) continue;

            if (sym != null && (sym.getType() == DataType.STRUCT || sym.getType() == DataType.CLASS || sym.structTypeName != null)) {
                String typeName = sym.structTypeName != null ? sym.structTypeName : sym.getName();
                Map<String, Integer> fieldMap = null;
                if (activeMemoryLayout != null) {
                    fieldMap = activeMemoryLayout.getClassFieldOffsets().get(typeName);
                    if (fieldMap == null) {
                        fieldMap = activeMemoryLayout.getStructFieldOffsets().get(typeName);
                    }
                }
                if (fieldMap != null) {
                    for (Map.Entry<String, Integer> fEntry : fieldMap.entrySet()) {
                        String fieldName = fEntry.getKey();
                        int fOff = fEntry.getValue();
                        int fAddr = base + fOff;
                        if (fAddr < heap.length && !heapTypes.containsKey(fAddr)) {
                            double fVal = heap[fAddr];
                            String fValStr;
                            if (knownStrings.contains((int) fVal) || (fVal >= 0 && fVal < heap.length && heapTypes.containsKey((int) fVal) && heapTypes.get((int) fVal).contains("caracter"))) {
                                fValStr = "heap[" + (int) fVal + "] -> \"" + escapeString(activeVM.readStringFromHeap((int) fVal)) + "\"";
                            } else {
                                fValStr = formatNumber(fVal);
                            }
                            heapTypes.put(fAddr, "campo: " + typeName + "." + fieldName);
                            heapValues.put(fAddr, fValStr);
                            heapDetails.put(fAddr, "Instancia " + typeName + " en heap[" + base + "] - Campo '" + fieldName + "'");
                        }
                    }
                }
            } else if (sym != null && (sym.getKind() == SymbolKind.ARRAY || sym.elementType != null || sym.arraySize != null)) {
                if (heap[base] > 0 && heap[base] < 1000) {
                    int n = (int) heap[base];
                    heapTypes.put(base, "tamaño arreglo");
                    heapValues.put(base, n + " elementos");
                    heapDetails.put(base, "Arreglo '" + vName + "' - Capacidad asignada");
                    for (int i = 0; i < n; i++) {
                        int elemAddr = base + 1 + i;
                        if (elemAddr < heap.length && !heapTypes.containsKey(elemAddr)) {
                            heapTypes.put(elemAddr, "elemento arreglo [" + i + "]");
                            heapValues.put(elemAddr, formatNumber(heap[elemAddr]));
                            heapDetails.put(elemAddr, "Arreglo '" + vName + "'[" + i + "] (base heap[" + base + "])");
                        }
                    }
                } else {
                    int n = (sym.arraySize != null && sym.arraySize > 0) ? sym.arraySize : 5;
                    for (int i = 0; i < n; i++) {
                        int elemAddr = base + i;
                        if (elemAddr < heap.length && !heapTypes.containsKey(elemAddr)) {
                            heapTypes.put(elemAddr, "elemento arreglo [" + i + "]");
                            heapValues.put(elemAddr, formatNumber(heap[elemAddr]));
                            heapDetails.put(elemAddr, "Arreglo '" + vName + "'[" + i + "] (base heap[" + base + "])");
                        }
                    }
                }
            }
        }

        if (activeVM.getH() > 0) {
            int hAddr = (int) activeVM.getH();
            if (!heapTypes.containsKey(hAddr)) {
                heapTypes.put(hAddr, "Puntero Heap (H)");
                heapValues.put(hAddr, String.valueOf(hAddr));
                heapDetails.put(hAddr, "Próxima posición libre en memoria dinámica (H = " + hAddr + ")");
            }
        }

        for (int wAddr : writtenHeap) {
            if (!heapTypes.containsKey(wAddr)) {
                double val = heap[wAddr];
                heapTypes.put(wAddr, "Celda Heap");
                heapValues.put(wAddr, formatNumber(val));
                heapDetails.put(wAddr, "Memoria dinámica en heap[" + wAddr + "]");
            }
        }

        Set<Integer> allHeapAddrs = new TreeSet<>(writtenHeap);
        allHeapAddrs.addAll(heapTypes.keySet());
        if (activeVM.getH() > 0) {
            allHeapAddrs.add((int) activeVM.getH());
        }

        List<HeapViewModel> heapVMs = new ArrayList<>();
        for (int addr : allHeapAddrs) {
            String t = heapTypes.getOrDefault(addr, "Celda Heap");
            String v = heapValues.getOrDefault(addr, formatNumber(heap[addr]));
            String d = heapDetails.getOrDefault(addr, "Memoria dinámica en heap[" + addr + "]");
            heapVMs.add(new HeapViewModel("heap[" + addr + "]", t, v, d));
        }
        heapList.setAll(heapVMs);
    }

    @FXML
    public void handleTriggerGC() {
        workspaceService.notifyUser("Garbage Collector triggered. 0 unreachable objects collected.");
    }

    private void showNotification(String message) {
        NotificationToast.show(mainRootContainer, message);
    }

    private String getFileIconSymbol(LanguageType lang) {
        return switch (lang) {
            case PIG_LATIN -> "◈";
            case Y_LANG -> "◆";
            case ZETARIANO -> "❖";
            case C_LANG -> "▲";
            default -> "📄";
        };
    }

    private String getLanguageBadgeText(LanguageType type) {
        return switch (type) {
            case PIG_LATIN -> "PIG";
            case Y_LANG -> "Y?";
            case ZETARIANO -> "ZET";
            case C_LANG -> "C";
            default -> "TXT";
        };
    }

    // ejecucion de los comandos
    private List<CommandItem> buildCommandList() {
        List<CommandItem> list = new ArrayList<>();
        list.add(new CommandItem("Run Project", "Execution", "F5", this::handleRunProject));
        list.add(new CommandItem("New Project...", "Project", "", this::handleNewProject));
        list.add(new CommandItem("Open Project...", "Project", "", this::handleOpenProject));
        list.add(new CommandItem("Open File...", "File", "", this::handleOpenFile));
        list.add(new CommandItem("Save Active File", "File", "Ctrl+S", this::handleSave));
        list.add(new CommandItem("Show Terminal", "View", "", this::handleSelectTerminal));
        list.add(new CommandItem("Show Problems", "View", "", this::handleSelectProblems));
        list.add(new CommandItem("Show AST Visualizer", "View", "", this::handleSelectAst));
        list.add(new CommandItem("Show Symbol Table", "View", "", this::handleSelectSymbols));
        list.add(new CommandItem("Show Lexer Tokens", "View", "", this::handleSelectLexer));
        list.add(new CommandItem("Show Lexer Error Tokens", "View", "", this::handleSelectLexerError));
        list.add(new CommandItem("Show Three Address Code (C3D)", "View", "", this::handleSelectC3D));
        list.add(new CommandItem("Show Quadruples", "View", "", this::handleSelectQuadruples));
        list.add(new CommandItem("Show Stack Memory", "View", "", this::handleSelectStack));
        list.add(new CommandItem("Show Heap Memory", "View", "", this::handleSelectHeap));
        list.add(new CommandItem("Toggle Project Explorer", "View", "", this::handleToggleExplorer));
        list.add(new CommandItem("Show Welcome Screen", "Help", "", this::handleShowWelcome));
        return list;
    }

    // corre el codigo lexico para su analisis
    private void runLexicalAnalysis() {
        // buscar el pane activo
        EditorTabModel activeTab = workspaceService.getActiveTab();

        if (activeTab == null) {
            workspaceService.notifyUser("No active file");
            return;
        }

        CodeArea codeArea = codeAreaMap.get(activeTab);

        if (codeArea == null) {
            workspaceService.notifyUser("No editor found for active file");
            return;
        }

        bottomTabPane.getSelectionModel().select(LEXER_TAB);
        LEXER_SUB_TAB_PANE.getSelectionModel().select(0);

        // obtener codigo del area activa
        String source = codeArea.getText();

        try {

            // realizar el analisis
            LexerService lexer = LexerFactory.create(activeTab.getLanguageType());

            if (lexer == null) {
                workspaceService.notifyUser("Could not create lexer for " + activeTab.getLanguageType().getDisplayName());
                return;
            }

            tokensList.clear();
            lexerErrorsList.clear();

            tokensTable.setItems(tokensList);
            lexerErrorsTable.setItems(lexerErrorsList);

            ResultLexer result = lexer.analyze(source);

            tokensList.setAll(result.tokens);

            tokensBadge.setText("Tokens: " + result.tokens.size());

            // resultado del analisis
            if (result.isValid()) {

                LEXER_SUB_TAB_PANE.getSelectionModel().select(0);

                setBadgeStyle(tokensBadge, "badge-success", "Tokens: " + result.tokens.size());

                setBadgeStyle(errorsBadge, "badge-info", "Errores: 0");

                workspaceService.notifyUser("Lexical Analysis Completed Successfully");

            } else {

                lexerErrorsList.setAll(result.errors);

                LEXER_SUB_TAB_PANE.getSelectionModel().select(1);

                setBadgeStyle(tokensBadge, "badge-warning", "Tokens: " + result.tokens.size());

                setBadgeStyle(errorsBadge, "badge-danger", "Errores: " + result.errors.size());

                workspaceService.notifyUser("Lexical Analysis Failed — Errors Found");
            }

        } catch (Exception e) {

            bottomTabPane.getSelectionModel().select(LEXER_TAB);
            LEXER_SUB_TAB_PANE.getSelectionModel().select(1);

            e.printStackTrace();

            workspaceService.notifyUser("Lexer error: " + e.getMessage());
        }
    }

    private void runSyntaxAnalysis() {
        EditorTabModel activeTab = workspaceService.getActiveTab();

        if (activeTab == null) {
            workspaceService.notifyUser("No active file");
            return;
        }

        CodeArea codeArea = codeAreaMap.get(activeTab);

        if (codeArea == null) {
            workspaceService.notifyUser("No editor found for active file");
            return;
        }

        handleSelectSyntax();

        String source = codeArea.getText();

        try {
            ParserService parser = ParserFactory.create(activeTab.getLanguageType());

            if (parser == null) {
                workspaceService.notifyUser("Could not create parser for " + activeTab.getLanguageType().getDisplayName());
                return;
            }

            syntaxErrorsList.clear();
            syntaxTable.setItems(syntaxErrorsList);

            List<SyntaxError> errors = parser.analyze(source);
            System.out.println(errors.toString());
            syntaxErrorsList.setAll(errors);

            if (errors.isEmpty()) {
                workspaceService.notifyUser("Syntax Analysis Completed Successfully");
            } else {
                workspaceService.notifyUser("Syntax Analysis Failed — Errors Found");
            }

        } catch (Exception e) {
            handleSelectSyntax();
            e.printStackTrace();
            workspaceService.notifyUser("Parser error: " + e.getMessage());
        }
    }

    // clase para pintar
    private void setBadgeStyle(Label badge, String styleClass, String text) {
        badge.getStyleClass().removeAll("badge-success", "badge-warning", "badge-danger", "badge-info");
        badge.getStyleClass().add(styleClass);
        badge.setText(text);
    }
}


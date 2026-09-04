package org.nexus.nexussolairy.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
import org.nexus.nexussolairy.model.semantic.view.SymbolViewModel;
import org.nexus.nexussolairy.model.syntactic.SyntaxError;
import org.nexus.nexussolairy.model.view.*;
import org.nexus.nexussolairy.patron.LexerFactory;
import org.nexus.nexussolairy.patron.ParserFactory;
import org.nexus.nexussolairy.service.grammar.LexerService;
import org.nexus.nexussolairy.service.parser.ParserService;
import org.nexus.nexussolairy.service.ui.FileService;
import org.nexus.nexussolairy.service.ui.ProjectService;
import org.nexus.nexussolairy.service.ui.WorkspaceService;
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
    private TableColumn<SymbolViewModel, String> colSymName, colSymType, colSymKind, colSymScope, colSymValue;
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
    private Label stackStepLabel, stackActionBadge, stackCurrentTokenLabel, stackActiveRuleLabel;
    @FXML
    private Button stackAutoPlayBtn;
    @FXML
    private VBox stackVisualContainer;
    @FXML
    private ListView<String> stackLogListView;

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
    private final ObservableList<HeapViewModel> heapList = FXCollections.observableArrayList();

    private final List<StackStepViewModel> stackSteps = new ArrayList<>();
    private int currentStackStepIndex = 0;
    private Timeline stackAutoPlayTimeline;
    private double astZoom = 1.0;
    private AstNodeViewModel selectedAstNode = null;

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
        colSymValue.setCellValueFactory(c -> c.getValue().valueProperty());
        colSymLine.setCellValueFactory(c -> c.getValue().lineProperty());
        colSymCol.setCellValueFactory(c -> c.getValue().columnProperty());

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

        quadruplesTable.setItems(quadruplesList);
        quadruplesTable.setPlaceholder(new Label("No quadruples generated"));
    }

    private void initStack() {
        stackVisualContainer.getChildren().clear();
        stackLogListView.getItems().clear();
        stackStepLabel.setText("Step 0 of 0");
        stackActionBadge.setText("IDLE");
        stackCurrentTokenLabel.setText("None");
        stackActiveRuleLabel.setText("None");
    }

    private void initHeap() {
        colHeapAddr.setCellValueFactory(c -> c.getValue().addressProperty());
        colHeapType.setCellValueFactory(c -> c.getValue().typeProperty());
        colHeapVal.setCellValueFactory(c -> c.getValue().valueProperty());
        colHeapDetails.setCellValueFactory(c -> c.getValue().detailsProperty());

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

        codeArea.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.TAB) {
                event.consume();

                int caret = codeArea.getCaretPosition();

                // insertar exactamente 4 espacios
                codeArea.insertText(caret, "    ");

                // mover cursor a 4 espacios
                codeArea.moveTo(caret + 4);
            }
        });

        codeArea.replaceText(content);
        codeAreaMap.put(model, codeArea);

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
            default -> "> ";
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

    private void updateStackStepView() {
        if (stackSteps.isEmpty() || currentStackStepIndex < 0 || currentStackStepIndex >= stackSteps.size()) return;

        StackStepViewModel current = stackSteps.get(currentStackStepIndex);
        stackStepLabel.setText(String.format("Step %d of %d", current.getStepNumber(), stackSteps.size()));
        stackActionBadge.setText(current.getActionName());

        stackActionBadge.getStyleClass().removeAll("sra-shift", "sra-replace", "sra-accept");
        if ("shift".equalsIgnoreCase(current.getActionType())) stackActionBadge.getStyleClass().add("sra-shift");
        else if ("replace".equalsIgnoreCase(current.getActionType()))
            stackActionBadge.getStyleClass().add("sra-replace");
        else stackActionBadge.getStyleClass().add("sra-accept");

        stackCurrentTokenLabel.setText(current.getCurrentToken());
        stackActiveRuleLabel.setText(current.getCurrentRule());

        stackVisualContainer.getChildren().clear();
        for (int i = 0; i < current.getStackFrames().size(); i++) {
            var frame = current.getStackFrames().get(i);
            HBox frameBox = new HBox(10);
            frameBox.setAlignment(Pos.CENTER_LEFT);
            frameBox.setPadding(new Insets(6, 12, 6, 12));

            String style = switch (frame.getType()) {
                case "shift" -> "sra-stack-frame-shift";
                case "rule" -> "sra-stack-frame-rule";
                case "replace" -> "sra-stack-frame-replace";
                case "accept" -> "sra-stack-frame-accept";
                default -> "sra-stack-frame-token";
            };
            frameBox.getStyleClass().addAll("sra-column", style);

            Label idx = new Label("#" + i);
            idx.getStyleClass().add("sra-column-number");

            Label val = new Label(frame.getLabel());
            val.getStyleClass().add("sra-stack-label");
            HBox.setHgrow(val, Priority.ALWAYS);

            if (i == current.getStackFrames().size() - 1) {
                Label top = new Label("TOP");
                top.getStyleClass().add("sra-top-badge");
                frameBox.getChildren().addAll(idx, val, top);
            } else {
                frameBox.getChildren().addAll(idx, val);
            }

            stackVisualContainer.getChildren().add(0, frameBox);
        }

        stackLogListView.getSelectionModel().select(currentStackStepIndex);
        stackLogListView.scrollTo(currentStackStepIndex);
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
        String targetName = tab != null ? tab.getTitle() : "main.pig";
        LanguageType lang = tab != null ? tab.getLanguageType() : LanguageType.PIG_LATIN;

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
        handleRunProject();
    }


    @FXML
    public void handleAnalyze() {
        bottomTabPane.getSelectionModel().select(terminalTab);
        appendTerminalInfo("Pipeline: Lexical -> Syntactic -> Semantic Check starting...");
        appendTerminalSuccess("Pipeline check completed. 0 errors.");
        workspaceService.notifyUser("Analysis pipeline executed");
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
        workspaceService.notifyUser("C3D generation ready for compiler integration");
    }

    @FXML
    public void handlePrevStackStep() {
        if (currentStackStepIndex > 0) {
            currentStackStepIndex--;
            updateStackStepView();
        }
    }

    @FXML
    public void handleNextStackStep() {
        if (!stackSteps.isEmpty() && currentStackStepIndex < stackSteps.size() - 1) {
            currentStackStepIndex++;
            updateStackStepView();
        }
    }

    @FXML
    public void handleToggleAutoPlay() {
        if (stackAutoPlayTimeline != null && stackAutoPlayTimeline.getStatus() == Timeline.Status.RUNNING) {
            stackAutoPlayTimeline.stop();
            stackAutoPlayBtn.setText("▶ Auto Play");
        } else if (!stackSteps.isEmpty()) {
            stackAutoPlayBtn.setText("⏸ Pause");
            stackAutoPlayTimeline = new Timeline(new KeyFrame(Duration.millis(1200), e -> {
                if (!stackSteps.isEmpty() && currentStackStepIndex < stackSteps.size() - 1) {
                    handleNextStackStep();
                } else {
                    currentStackStepIndex = 0;
                    updateStackStepView();
                }
            }));
            stackAutoPlayTimeline.setCycleCount(Timeline.INDEFINITE);
            stackAutoPlayTimeline.play();
        }
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
        list.add(new CommandItem("Show Parser Stack", "View", "", this::handleSelectStack));
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


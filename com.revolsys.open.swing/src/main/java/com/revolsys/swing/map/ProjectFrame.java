package com.revolsys.swing.map;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRootPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.tree.TreePath;

import com.revolsys.collection.set.Sets;
import com.revolsys.datatype.DataTypes;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.util.BoundingBoxUtil;
import com.revolsys.io.FileUtil;
import com.revolsys.io.file.FileConnectionManager;
import com.revolsys.io.file.FileNameExtensionFilter;
import com.revolsys.io.file.FolderConnectionRegistry;
import com.revolsys.io.file.Paths;
import com.revolsys.logging.Logs;
import com.revolsys.process.JavaProcess;
import com.revolsys.record.io.RecordStoreConnection;
import com.revolsys.record.io.RecordStoreConnectionManager;
import com.revolsys.record.io.RecordStoreConnectionRegistry;
import com.revolsys.spring.resource.PathResource;
import com.revolsys.swing.EventQueue;
import com.revolsys.swing.Icons;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.TabbedPane;
import com.revolsys.swing.action.RunnableAction;
import com.revolsys.swing.action.enablecheck.ObjectPropertyEnableCheck;
import com.revolsys.swing.component.BaseFrame;
import com.revolsys.swing.component.DnDTabbedPane;
import com.revolsys.swing.component.TabClosableTitle;
import com.revolsys.swing.logging.Log4jTableModel;
import com.revolsys.swing.map.form.RecordStoreConnectionForm;
import com.revolsys.swing.map.layer.Layer;
import com.revolsys.swing.map.layer.LayerGroup;
import com.revolsys.swing.map.layer.Project;
import com.revolsys.swing.map.overlay.MeasureOverlay;
import com.revolsys.swing.map.print.SinglePage;
import com.revolsys.swing.menu.MenuFactory;
import com.revolsys.swing.parallel.Invoke;
import com.revolsys.swing.parallel.SwingWorkerProgressBar;
import com.revolsys.swing.pdf.SaveAsPdf;
import com.revolsys.swing.preferences.PreferencesDialog;
import com.revolsys.swing.scripting.ScriptRunner;
import com.revolsys.swing.table.worker.SwingWorkerTableModel;
import com.revolsys.swing.tree.BaseTree;
import com.revolsys.swing.tree.BaseTreeNode;
import com.revolsys.swing.tree.node.ListTreeNode;
import com.revolsys.swing.tree.node.WebServiceConnectionTrees;
import com.revolsys.swing.tree.node.file.FolderConnectionsTrees;
import com.revolsys.swing.tree.node.file.PathTreeNode;
import com.revolsys.swing.tree.node.layer.ProjectTreeNode;
import com.revolsys.swing.tree.node.record.RecordStoreConnectionTrees;
import com.revolsys.util.OS;
import com.revolsys.util.PreferencesUtil;
import com.revolsys.util.Property;
import com.revolsys.webservice.WebServiceConnectionManager;
import com.revolsys.webservice.WebServiceConnectionRegistry;

public class ProjectFrame extends BaseFrame {
  private static final String BOTTOM_TAB = "INTERNAL_bottomTab";

  private static final String BOTTOM_TAB_LISTENER = "INTERNAL_bottomTabListener";

  public static final String PROJECT_FRAME = "INTERNAL_projectFrame";

  public static final String SAVE_CHANGES_KEY = "Save Changes";

  public static final String SAVE_PROJECT_KEY = "Save Project";

  private static final long serialVersionUID = 1L;

  static {
    RecordStoreConnectionManager.setInvalidRecordStoreFunction((connection, exception) -> {
      return Invoke.andWait(() -> {
        final RecordStoreConnectionRegistry registry = connection.getRegistry();
        final RecordStoreConnectionForm form = new RecordStoreConnectionForm(registry, connection,
          exception);
        return form.showDialog();
      });
    });

    RecordStoreConnectionManager.setMissingRecordStoreFunction((name) -> {
      final RecordStoreConnectionRegistry registry = RecordStoreConnectionManager.get()
        .getUserConnectionRegistry();
      Invoke.andWait(() -> {
        final RecordStoreConnectionForm form = new RecordStoreConnectionForm(registry, name);
        form.showDialog();
      });
      final RecordStoreConnection connection = registry.getConnection(name);
      if (connection == null) {
        return null;
      } else {
        return connection.getRecordStore();
      }
    });
  }

  public static void addSaveActions(final JComponent component, final Project project) {
    final InputMap inputMap = component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK),
      SAVE_PROJECT_KEY);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.META_DOWN_MASK),
      SAVE_PROJECT_KEY);

    inputMap.put(
      KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK),
      SAVE_CHANGES_KEY);
    inputMap.put(
      KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.META_DOWN_MASK | InputEvent.ALT_DOWN_MASK),
      SAVE_CHANGES_KEY);

    final ActionMap actionMap = component.getActionMap();
    actionMap.put(SAVE_PROJECT_KEY, new RunnableAction(SAVE_PROJECT_KEY, project::saveAllSettings));
    actionMap.put(SAVE_CHANGES_KEY, new RunnableAction(SAVE_CHANGES_KEY, project::saveChanges));
  }

  public static ProjectFrame get(final Layer layer) {
    if (layer == null) {
      return null;
    } else {
      final LayerGroup project = layer.getProject();
      if (project == null) {
        return null;
      } else {
        return project.getProperty(PROJECT_FRAME);
      }
    }
  }

  public static void init() {
  }

  private Set<String> bottomTabLayerPaths = new LinkedHashSet<>();

  private DnDTabbedPane bottomTabs = new DnDTabbedPane();

  private BaseTree catalogTree;

  private boolean exitOnClose = true;

  private final String frameTitle;

  private JSplitPane leftRightSplit;

  private TabbedPane leftTabs = new TabbedPane();

  private MapPanel mapPanel;

  private final MenuFactory openRecentMenu = new MenuFactory("Open Recent Project");

  private Project project = new Project();

  private BaseTree tocTree;

  private JSplitPane topBottomSplit;

  private Path projectPath;

  public ProjectFrame(final String title, final Path projectPath) {
    this(title, projectPath, true);
  }

  public ProjectFrame(final String title, final Path projectPath, final boolean initialize) {
    super(title, false);
    this.frameTitle = title;
    this.projectPath = projectPath;
    if (initialize) {
      initUi();
      loadProject();
    }
  }

  private void actionNewProject() {
    if (this.project != null && this.project.saveWithPrompt()) {
      this.project.reset();
      super.setTitle("NEW - " + this.frameTitle);
    }
  }

  private void actionOpenProject() {
    if (this.project != null && this.project.saveWithPrompt()) {

      final JFileChooser fileChooser = SwingUtil.newFileChooser("Open Project",
        "com.revolsys.swing.map.project", "directory");

      final FileNameExtensionFilter filter = new FileNameExtensionFilter("Project (*.rgmap)",
        "rgmap");
      fileChooser.setAcceptAllFileFilterUsed(true);
      fileChooser.addChoosableFileFilter(filter);
      fileChooser.setFileFilter(filter);
      if (!OS.isMac()) {
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      }
      final int returnVal = fileChooser.showOpenDialog(this);
      if (returnVal == JFileChooser.APPROVE_OPTION) {
        final File projectDirectory = fileChooser.getSelectedFile();
        openProject(projectDirectory.toPath());
      }
    }
  }

  private void actionRunScript() {
    final File logDirectory = getLogDirectory();
    final JavaProcess javaProcess = newJavaProcess();
    ScriptRunner.runScriptProcess(this, logDirectory, javaProcess);
  }

  public void actionSaveProjectAs() {
    final Path path = this.project.saveAllSettingsAs();
    if (path != null) {
      addToRecentProjects(path);
      Invoke.later(() -> {
        final Project project = getProject();
        setTitle(project.getName() + " - " + getFrameTitle());
      });
    }
  }

  @SuppressWarnings("unchecked")
  public <C extends Component> C addBottomTab(final ProjectFramePanel panel,
    final Map<String, Object> config) {
    final TabbedPane tabs = getBottomTabs();

    final Object tableView = panel.getProperty(BOTTOM_TAB);
    Component component = null;
    if (tableView instanceof Component) {
      component = (Component)tableView;
      if (component.getParent() != tabs) {
        component = null;
      }
    }
    if (component == null) {
      component = panel.newPanelComponent(config);

      if (component != null) {
        final Component panelComponent = component;
        panel.activatePanelComponent(panelComponent, config);
        final int tabIndex = tabs.getTabCount();
        final String name = panel.getName();
        final Icon icon = panel.getIcon();

        panel.setPropertyWeak(BOTTOM_TAB, panelComponent);
        final PropertyChangeListener listener = EventQueue.addPropertyChange(panel, "name", () -> {
          final int index = tabs.indexOfComponent(panelComponent);
          if (index != -1) {
            final String newName = panel.getName();
            tabs.setTitleAt(index, newName);
          }
        });
        panel.setPropertyWeak(BOTTOM_TAB_LISTENER, listener);

        final String layerPath = panel.getPath();
        final Runnable closeAction = () -> {
          removeBottomTab(panel);
          synchronized (this.bottomTabLayerPaths) {
            this.bottomTabLayerPaths.remove(layerPath);
          }
        };
        synchronized (this.bottomTabLayerPaths) {
          this.bottomTabLayerPaths.add(layerPath);
        }
        final TabClosableTitle tab = tabs.addClosableTab(name, icon, panelComponent, closeAction);
        tab.setMenu(panel);

        tabs.setSelectedIndex(tabIndex);
      }
    } else {
      panel.activatePanelComponent(component, config);
      tabs.setSelectedComponent(component);
    }
    return (C)component;
  }

  protected void addMenu(final JMenuBar menuBar, final MenuFactory menuFactory) {
    if (menuFactory != null) {
      final JMenu menu = menuFactory.newComponent();
      menuBar.add(menu, menuBar.getMenuCount() - 1);
    }
  }

  private void addToRecentProjects(final Path projectPath) {
    final List<String> recentProjects = getRecentProjectPaths();
    final String filePath = projectPath.toAbsolutePath().toString();
    recentProjects.remove(filePath);
    recentProjects.add(0, filePath);
    while (recentProjects.size() > 10) {
      recentProjects.remove(recentProjects.size() - 1);
    }
    OS.setPreference("com.revolsys.gis", "/com/revolsys/gis/project", "recentProjects",
      recentProjects);
    OS.setPreference("com.revolsys.gis", "/com/revolsys/gis/project", "recentProject", filePath);
    updateRecentMenu();
  }

  @Override
  protected void close() {
    Property.removeAllListeners(this);
    setVisible(false);
    super.close();
    setRootPane(new JRootPane());
    removeAll();
    setMenuBar(null);
    if (this.project != null) {
      this.project.setProperty(PROJECT_FRAME, null);
      Project.clearProject(this.project);
    }
    if (this.bottomTabs != null) {
      for (final ContainerListener listener : this.bottomTabs.getContainerListeners()) {
        this.bottomTabs.removeContainerListener(listener);
      }
    }
    if (this.catalogTree != null) {
      this.catalogTree.setRoot(null);
    }
    if (this.mapPanel != null) {
      this.mapPanel.destroy();
    }
    if (this.project != null) {
      final RecordStoreConnectionRegistry recordStores = this.project.getRecordStores();
      RecordStoreConnectionManager.get().removeConnectionRegistry(recordStores);
      if (Project.get() == this.project) {
        Project.set(null);
      }
      this.project.delete();
    }
    if (this.tocTree != null) {
      this.tocTree.setRoot(null);
    }
    this.bottomTabs = null;
    this.catalogTree = null;
    this.leftRightSplit = null;
    this.leftTabs = null;
    this.mapPanel = null;
    this.project = null;
    this.tocTree = null;
    this.topBottomSplit = null;

    final ActionMap actionMap = getRootPane().getActionMap();
    actionMap.put(SAVE_PROJECT_KEY, null);
    actionMap.put(SAVE_CHANGES_KEY, null);
  }

  public void exit() {
    final Project project = getProject();
    if (project != null && project.saveWithPrompt()) {
      final Window[] windows = Window.getOwnerlessWindows();
      for (final Window window : windows) {
        SwingUtil.dispose(window);

      }
      System.exit(0);
    }
  }

  public void expandLayers(final Layer layer) {
    if (layer != null) {
      Invoke.later(() -> {
        final LayerGroup group;
        if (layer instanceof LayerGroup) {
          group = (LayerGroup)layer;
        } else {
          group = layer.getLayerGroup();
        }
        if (group != null) {
          final List<Layer> layerPath = group.getPathList();
          this.tocTree.expandPath(layerPath);
        }
      });
    }
  }

  public TabbedPane getBottomTabs() {
    return this.bottomTabs;
  }

  public double getControlWidth() {
    return 0.20;
  }

  protected BoundingBox getDefaultBoundingBox() {
    return BoundingBox.empty();
  }

  public String getFrameTitle() {
    return this.frameTitle;
  }

  public TabbedPane getLeftTabs() {
    return this.leftTabs;
  }

  public File getLogDirectory() {
    return FileUtil.getDirectory("log");
  }

  public MapPanel getMapPanel() {
    return this.mapPanel;
  }

  public Project getProject() {
    return this.project;
  }

  public Path getProjectPath() {
    return this.projectPath;
  }

  private List<String> getRecentProjectPaths() {
    final List<String> recentProjects = OS.getPreference("com.revolsys.gis",
      "/com/revolsys/gis/project", "recentProjects", new ArrayList<String>());
    for (int i = 0; i < recentProjects.size();) {
      final String filePath = recentProjects.get(i);
      final File file = FileUtil.getFile(filePath);
      if (file.exists()) {
        i++;
      } else {
        recentProjects.remove(i);
      }
    }
    OS.setPreference("com.revolsys.gis", "/com/revolsys/gis/project", "recentProjects",
      recentProjects);
    return recentProjects;
  }

  public BaseTreeNode getTreeNode(final Layer layer) {
    final List<Layer> layerPath = layer.getPathList();
    final TreePath treePath = this.tocTree.getTreePath(layerPath);
    if (treePath == null) {
      return null;
    } else {
      return (BaseTreeNode)treePath.getLastPathComponent();
    }
  }

  // public void expandConnectionManagers(final PropertyChangeEvent event) {
  // final Object newValue = event.getNewValue();
  // if (newValue instanceof ConnectionRegistry) {
  // final ConnectionRegistry<?> registry = (ConnectionRegistry<?>)newValue;
  // final ConnectionRegistryManager<?> connectionManager =
  // registry.getConnectionManager();
  // if (connectionManager != null) {
  // final List<?> connectionRegistries =
  // connectionManager.getConnectionRegistries();
  // if (connectionRegistries != null) {
  // final ObjectTree tree = catalogPanel.getTree();
  // tree.expandPath(connectionRegistries, connectionManager, registry);
  // }
  // }
  // }
  // }

  @Override
  protected void initUi() {
    setMinimumSize(new Dimension(600, 500));

    final JRootPane rootPane = getRootPane();

    addSaveActions(rootPane, this.project);

    final BoundingBox defaultBoundingBox = getDefaultBoundingBox();
    this.project.setViewBoundingBoxAndGeometryFactory(defaultBoundingBox);
    Project.set(this.project);
    this.project.setPropertyWeak(PROJECT_FRAME, this);

    newMapPanel();

    this.leftTabs.setMinimumSize(new Dimension(100, 300));
    this.leftTabs.setPreferredSize(new Dimension(300, 700));

    this.mapPanel.setMinimumSize(new Dimension(300, 300));
    this.mapPanel.setPreferredSize(new Dimension(700, 700));
    this.leftRightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.leftTabs, this.mapPanel);

    this.leftRightSplit.setBorder(BorderFactory.createEmptyBorder());
    this.bottomTabs.setBorder(BorderFactory.createEmptyBorder());
    this.bottomTabs.setPreferredSize(new Dimension(700, 200));
    final ContainerListener listener = new ContainerAdapter() {
      @Override
      public void componentRemoved(final ContainerEvent e) {
        final Component eventComponent = e.getChild();
        if (eventComponent instanceof ProjectFramePanel) {
          final ProjectFramePanel panel = (ProjectFramePanel)eventComponent;
          panel.setProperty(BOTTOM_TAB, null);
        }
      }
    };
    this.bottomTabs.addContainerListener(listener);

    this.topBottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, this.leftRightSplit,
      this.bottomTabs);
    this.bottomTabs.setMinimumSize(new Dimension(600, 100));

    this.topBottomSplit.setResizeWeight(1);

    add(this.topBottomSplit, BorderLayout.CENTER);

    newTabLeftTableOfContents();
    newTabLeftCatalogPanel();

    newTabBottomTasksPanel();
    Log4jTableModel.addNewTabPane(this.bottomTabs);
    setBounds((Object)null, false);

    super.initUi();
  }

  protected final void loadProject() {
    final Path projectPath = getProjectPath();
    if (projectPath == null) {
      getMapPanel().setInitializing(false);
    } else {
      Invoke.background("Load Project: " + projectPath, () -> {
        loadProject(projectPath);
        getMapPanel().setInitializing(false);
        loadProjectAfter();
      });
    }
  }

  protected void loadProject(final Path projectPath) {
    final PathResource resource = new PathResource(projectPath);
    this.project.readProject(resource);
    Invoke.later(() -> setTitle(this.project.getName() + " - " + this.frameTitle));

    final Object frameBoundsObject = this.project.getProperty("frameBounds");
    setBounds(frameBoundsObject, true);
    setVisible(true);

    final RecordStoreConnectionManager recordStoreConnectionManager = RecordStoreConnectionManager
      .get();
    recordStoreConnectionManager.removeConnectionRegistry("Project");
    final RecordStoreConnectionRegistry recordStores = this.project.getRecordStores();
    recordStoreConnectionManager.addConnectionRegistry(recordStores);

    final FileConnectionManager fileConnectionManager = FileConnectionManager.get();
    fileConnectionManager.removeConnectionRegistry("Project");
    final FolderConnectionRegistry folderConnections = this.project.getFolderConnections();
    fileConnectionManager.addConnectionRegistry(folderConnections);

    final WebServiceConnectionManager webServiceConnectionManager = WebServiceConnectionManager
      .get();
    webServiceConnectionManager.removeConnectionRegistry("Project");
    final WebServiceConnectionRegistry webServices = this.project.getWebServices();
    webServiceConnectionManager.addConnectionRegistry(webServices);

    final MapPanel mapPanel = getMapPanel();
    final BoundingBox initialBoundingBox = this.project.getInitialBoundingBox();
    final Viewport2D viewport = mapPanel.getViewport();
    if (!BoundingBoxUtil.isEmpty(initialBoundingBox)) {
      this.project.setViewBoundingBoxAndGeometryFactory(initialBoundingBox);
      viewport.setBoundingBoxAndGeometryFactory(initialBoundingBox);
    }
    viewport.setInitialized(true);
  }

  protected void loadProjectAfter() {
    this.bottomTabLayerPaths = Sets
      .newLinkedHash(this.project.<Collection<String>> getProperty("bottomTabLayerPaths"));
    this.project.setProperty("bottomTabLayerPaths", this.bottomTabLayerPaths);
    for (final String layerPath : this.bottomTabLayerPaths) {
      final Layer layer = this.project.getLayerByPath(layerPath);
      if (layer != null) {
        Invoke.later(layer::showTableView);
      }
    }
  }

  public JavaProcess newJavaProcess() {
    return new JavaProcess();
  }

  protected MapPanel newMapPanel() {
    this.mapPanel = new MapPanel(this.project);
    if (OS.isMac()) {
      // Make border on right/bottom to match the JTabbedPane UI on a mac
      this.mapPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 9, 9));
    }
    return this.mapPanel;
  }

  @Override
  protected JMenuBar newMenuBar() {
    final JMenuBar menuBar = super.newMenuBar();
    addMenu(menuBar, newMenuFile());

    final MenuFactory tools = newMenuTools();

    if (OS.isWindows()) {
      tools.addMenuItem("options", "Options...", "Options...", (String)null,
        PreferencesDialog.get()::showPanel);
    }
    addMenu(menuBar, tools);
    return menuBar;
  }

  protected MenuFactory newMenuFile() {
    final MenuFactory file = new MenuFactory("File");

    file.addMenuItemTitleIcon("projectOpen", "New Project", "layout_add", this::actionNewProject)
      .setAcceleratorControlKey(KeyEvent.VK_N);

    file
      .addMenuItemTitleIcon("projectOpen", "Open Project...", "layout_add", this::actionOpenProject)
      .setAcceleratorControlKey(KeyEvent.VK_O);

    file.addComponentFactory("projectOpen", this.openRecentMenu);
    updateRecentMenu();

    file.addMenuItemTitleIcon("projectSave", "Save Project", "layout_save",
      this.project::saveAllSettings).setAcceleratorControlKey(KeyEvent.VK_S);

    file.addMenuItemTitleIcon("projectSave", "Save Project As...", "layout_save",
      this::actionSaveProjectAs).setAcceleratorShiftControlKey(KeyEvent.VK_S);

    file.addMenuItemTitleIcon("save", "Save as PDF", "save_pdf", SaveAsPdf::save);

    file.addMenuItemTitleIcon("print", "Print", "printer", SinglePage::print)
      .setAcceleratorControlKey(KeyEvent.VK_P);

    if (OS.isWindows()) {
      file.addMenuItemTitleIcon("exit", "Exit", null, this::exit)
        .setAcceleratorKey(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_MASK));
    } else if (OS.isUnix()) {
      file.addMenuItemTitleIcon("exit", "Exit", null, this::exit)
        .setAcceleratorKey(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
    }

    return file;
  }

  protected MenuFactory newMenuTools() {
    final MenuFactory tools = new MenuFactory("Tools");
    final MapPanel map = getMapPanel();
    final MeasureOverlay measureOverlay = map.getMapOverlay(MeasureOverlay.class);

    tools.addCheckboxMenuItem("map",
      new RunnableAction("Measure Length", Icons.getIcon("ruler_line"),
        () -> measureOverlay.toggleMeasureMode(DataTypes.LINE_STRING)),
      new ObjectPropertyEnableCheck(measureOverlay, "measureDataType", DataTypes.LINE_STRING));

    tools.addCheckboxMenuItem("map",
      new RunnableAction("Measure Area", Icons.getIcon("ruler_polygon"),
        () -> measureOverlay.toggleMeasureMode(DataTypes.POLYGON)),
      new ObjectPropertyEnableCheck(measureOverlay, "measureDataType", DataTypes.POLYGON));

    tools.addMenuItem("script", "Run Script...", "script_go", this::actionRunScript);
    return tools;
  }

  protected void newTabBottomTasksPanel() {
    final int tabIndex = SwingWorkerTableModel.addNewTabPanel(this.bottomTabs);

    final SwingWorkerProgressBar progressBar = this.mapPanel.getProgressBar();
    final JButton viewTasksAction = RunnableAction.newButton(null, "View Running Tasks",
      Icons.getIcon("time_go"), () -> this.bottomTabs.setSelectedIndex(tabIndex));
    viewTasksAction.setBorderPainted(false);
    viewTasksAction.setBorder(null);
    progressBar.add(viewTasksAction, BorderLayout.EAST);
  }

  protected void newTabLeftCatalogPanel() {
    final BaseTreeNode recordStores = RecordStoreConnectionTrees
      .newRecordStoreConnectionsTreeNode();

    final BaseTreeNode fileSystems = PathTreeNode.newFileSystemsTreeNode();

    final BaseTreeNode folderConnections = FolderConnectionsTrees.newFolderConnectionsTreeNode();

    final BaseTreeNode webServices = WebServiceConnectionTrees.newWebServiceConnectionsTreeNode();

    final ListTreeNode root = new ListTreeNode("/", recordStores, fileSystems, folderConnections,
      webServices);

    final BaseTree tree = new BaseTree(root);
    tree.setRootVisible(false);

    recordStores.expandChildren();
    fileSystems.expand();
    folderConnections.expandChildren();
    webServices.expandChildren();

    this.catalogTree = tree;

    final Icon icon = Icons.getIconWithBadge("folder", "tree");
    final TabbedPane tabs = this.leftTabs;
    final Component component = this.catalogTree;
    tabs.addTab(icon, "Catalog", component, true);
  }

  protected void newTabLeftTableOfContents() {
    final Project project = getProject();
    this.tocTree = ProjectTreeNode.newTree(project);
    this.leftTabs.addTabIcon("tree_layers", "TOC", this.tocTree, true);
  }

  public void openProject(final Path projectPath) {
    if (Files.exists(projectPath)) {
      this.projectPath = projectPath;
      try {
        addToRecentProjects(projectPath);

        PreferencesUtil.setUserString("com.revolsys.swing.map.project", "directory",
          projectPath.getParent().toString());
        this.project.reset();
        final Runnable task = this::loadProject;
        Invoke.background("Load project", task);
      } catch (final Throwable e) {
        Logs.error(this, "Unable to open project:" + projectPath, e);
      }
    }
  }

  public void removeBottomTab(final ProjectFramePanel panel) {
    final JTabbedPane tabs = getBottomTabs();
    final PropertyChangeListener listener = panel.getProperty(BOTTOM_TAB_LISTENER);
    if (listener != null) {
      Property.removeListener(panel, listener);
    }

    final Component component = panel.getProperty(BOTTOM_TAB);
    if (component != null) {
      if (tabs != null) {
        tabs.remove(component);
      }
      panel.deletePanelComponent(component);
    }
    panel.setProperty(BOTTOM_TAB, null);
    panel.setProperty(BOTTOM_TAB_LISTENER, null);
  }

  public void setBounds(final Object frameBoundsObject, final boolean visible) {
    Invoke.later(() -> {
      boolean sizeSet = false;
      if (frameBoundsObject instanceof List) {
        try {
          @SuppressWarnings("unchecked")
          final List<Number> frameBoundsList = (List<Number>)frameBoundsObject;
          if (frameBoundsList.size() == 4) {
            int x = frameBoundsList.get(0).intValue();
            int y = frameBoundsList.get(1).intValue();
            int width = frameBoundsList.get(2).intValue();
            int height = frameBoundsList.get(3).intValue();

            final Rectangle screenBounds = SwingUtil.getScreenBounds(x, y);

            width = Math.min(width, screenBounds.width);
            height = Math.min(height, screenBounds.height);
            setSize(width, height);

            if (x < screenBounds.x || x > screenBounds.x + screenBounds.width) {
              x = 0;
            } else {
              x = Math.min(x, screenBounds.x + screenBounds.width - width);
            }
            if (y < screenBounds.y || x > screenBounds.y + screenBounds.height) {
              y = 0;
            } else {
              y = Math.min(y, screenBounds.y + screenBounds.height - height);
            }
            setLocation(x, y);
            sizeSet = true;
          }
        } catch (final Throwable t) {
        }
      }
      if (!sizeSet) {
        final Rectangle screenBounds = SwingUtil.getScreenBounds();
        setLocation(screenBounds.x + 10, screenBounds.y + 10);
        setSize(screenBounds.width - 20, screenBounds.height - 20);
      }
      final int leftRightDividerLocation = (int)(getWidth() * 0.2);
      this.leftRightSplit.setDividerLocation(leftRightDividerLocation);

      final int topBottomDividerLocation = (int)(getHeight() * 0.75);
      this.topBottomSplit.setDividerLocation(topBottomDividerLocation);
      if (visible) {
        setVisible(true);
      }
    });
  }

  public void setExitOnClose(final boolean exitOnClose) {
    this.exitOnClose = exitOnClose;
  }

  protected void setProjectPath(final Path projectPath) {
    this.projectPath = projectPath;
  }

  public void updateRecentMenu() {
    final List<String> recentProjects = getRecentProjectPaths();

    this.openRecentMenu.clear();
    for (final String filePath : recentProjects) {
      final Path file = Paths.getPath(filePath);
      final String fileName = Paths.getFileName(file);
      final String path = file.getParent().toString();
      this.openRecentMenu.addMenuItem("default", fileName + " - " + path, "layout_add",
        () -> openProject(file));
    }
  }

  @Override
  public void windowClosing(final WindowEvent e) {
    if (this.exitOnClose) {
      exit();
    } else {
      dispose();
    }
  }

}

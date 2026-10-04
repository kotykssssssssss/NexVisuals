package dev.nexvisuals.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.nexvisuals.client.render.Draw;
import dev.nexvisuals.client.integration.ShaderIntegration;
import dev.nexvisuals.core.config.*;
import dev.nexvisuals.core.module.*;
import dev.nexvisuals.core.setting.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import java.util.function.*;

/** Category windows compose the existing metadata-driven settings editor; no module-specific UI. */
public final class NexVisualsScreen extends Screen {
    private final Screen parent;
    private final ModuleRegistry registry;
    private final GlobalSettings globals;
    private final Runnable save;
    private final ProfileManager profiles;
    private final GuiState state;
    private CategoryBoardLayout board;
    private final List<CategoryPanel> panels=new ArrayList<>();
    private ScrollPane settingPane;
    private EditBox search;
    private NexButton inspectorClose;
    private KeybindSetting capturing;
    private boolean worldPreview, focusSearch;
    private final Runnable rebuild = this::rebuildWidgets;
    private NexButton runtimeInfo;
    private Supplier<String> runtimeInfoLabel;
    private String runtimeInfoText;
    private ChoicePopup choicePopup;
    private CategoryPanel dragged;
    private double dragX,dragY,dragStartX,dragStartY;
    private boolean dragMoved;
    private final Map<String,Integer> presetSelections=new HashMap<>();
    private List<Setting<?>> inspectedSettings=List.of();
    private long visibilityRevision,visibilityMask;

    public NexVisualsScreen(Screen parent,ModuleRegistry registry,GlobalSettings globals,Runnable save) {
        this(parent,registry,globals,null,save);
    }
    public NexVisualsScreen(Screen parent,ModuleRegistry registry,GlobalSettings globals,ProfileManager profiles,Runnable save) {
        super(Component.literal("NexVisuals"));
        this.parent=parent; this.registry=registry; this.globals=globals; this.profiles=profiles; this.save=save;
        state=new GuiState(globals);
    }
    @Override protected void init() {
        inspectedSettings=List.of();
        choicePopup=null; dragged=null; panels.clear(); settingPane=null; inspectorClose=null; runtimeInfo=null;
        setDragging(false);
        List<Category> categories=Arrays.stream(Category.values()).filter(c->state.query.isBlank() || !state.matching(registry,c).isEmpty()).toList();
        board=CategoryBoardLayout.of(width,height,categories.size(),state.boardPage);
        state.boardPage=board.page();
        int margin=board.workspace().x();
        int right=width-margin;
        int buttonY=9;
        NexButton general=addRenderableWidget(button(right-64,buttonY,64,21,()->"General",()->state.globalSettings,()->{
            state.globalSettings=true; state.narrowDetails=true; state.settingScroll=0; requestRebuild();
        },"Interface colors, opacity and motion"));
        general.active=!state.narrowDetails;
        int buttonsLeft=right-70;
        if(profiles!=null) {
            NexButton profile=addRenderableWidget(button(buttonsLeft-66,buttonY,66,21,()->"Profiles",()->false,
                    ()->minecraft.setScreen(new ProfilesScreen(this,profiles,globals,()->{
                        state.drafts.clear(); state.invalidDrafts.clear();
                        state.panelPositions.clear(); state.panelPositions.putAll(globals.panelPositions());
                        state.panelScroll.clear(); save.run();
                    })),"Save / load local visual profiles"));
            profile.active=!state.narrowDetails; buttonsLeft-=72;
        }
        NexButton hud=addRenderableWidget(button(buttonsLeft-67,buttonY,67,21,()->"HUD editor",()->false,
                ()->minecraft.setScreen(new HudEditorScreen(this,registry,globals,save)),"Drag and scale your HUD elements"));
        hud.active=!state.narrowDetails && minecraft.level!=null && minecraft.player!=null;
        search=new EditBox(font,margin+4,38,Math.max(24,board.workspace().width()-8),20,Component.literal("Search all visual modules"));
        search.setMaxLength(80); search.setHint(Component.literal("Search modules...  Ctrl+F")); search.setValue(state.query);
        search.setResponder(value->{if(state.updateQuery(value)) requestRebuild();});
        search.active=!state.narrowDetails; addRenderableWidget(search);
        int footer=height-25;
        NexButton arrange=addRenderableWidget(button(margin,footer,72,19,()->"Arrange",()->false,()->{
            state.panelPositions.clear(); state.panelScroll.clear();
            state.panelOrder.clear(); state.panelOrder.addAll(List.of(Category.values())); requestRebuild();
        },"Restore automatic category positions and unfold all panels"));
        arrange.active=!state.narrowDetails;
        NexButton pages=addRenderableWidget(button(margin+78,footer,85,19,()->"Panels "+(board.page()+1)+"/"+board.pages(),()->false,()->{
            rememberScroll(); state.boardPage=(state.boardPage+1)%board.pages(); requestRebuild();
        },"Next category page. Small windows keep every module reachable."));
        pages.active=!state.narrowDetails && board.pages()>1;
        NexButton preview=addRenderableWidget(button(right-139,footer,63,19,()->"Preview",()->false,()->worldPreview=true,"F4 / Esc returns; gameplay input stays inside this screen"));
        preview.active=minecraft.level!=null && !state.narrowDetails;
        NexButton done=addRenderableWidget(button(right-70,footer,70,19,()->"Done",()->false,this::onClose,"Save and return to Minecraft"));
        done.active=!state.narrowDetails;
        for(int i=0;i<board.slots().size();i++) {
            Category c=categories.get(board.page()*board.capacity()+i);
            panels.add(new CategoryPanel(c,board.slots().get(i),board.workspace(),state.panelPositions.get(c),state.panelScroll.getOrDefault(c,0),
                    state.matching(registry,c),globals,!state.narrowDetails,this::registerControl,this::openSettings,
                    position->state.panelPositions.put(c,position),this::requestRebuild));
        }
        panels.sort(Comparator.comparingInt(panel->state.panelOrder.indexOf(panel.category)));
        if(state.narrowDetails) {
            buildSettings();
            var r=board.inspector();
            inspectorClose=button(r.right()-30,r.y()+7,22,20,()->"X",()->false,this::closeSettings,"Close settings (Esc)");
            addWidget(inspectorClose);
        }
    }
    @Override protected void setInitialFocus() {
        super.setInitialFocus(state.narrowDetails?inspectorClose:search);
    }
    @Override protected void rebuildWidgets() {
        boolean restore=search!=null && getFocused()==search && !state.narrowDetails;
        int cursor=restore?search.getCursorPosition():state.query.length();
        super.rebuildWidgets();
        if(restore || focusSearch) { setFocused(search); search.moveCursorTo(cursor,false); focusSearch=false; }
    }
    private void openSettings(String id) {
        rememberScroll(); state.selectedId=id; state.globalSettings=false; state.narrowDetails=true; state.settingScroll=0; requestRebuild();
    }
    private void closeSettings() {
        rememberScroll(); capturing=null; state.narrowDetails=false; state.globalSettings=false; requestRebuild();
    }
    private void buildSettings() {
        settingPane = new ScrollPane(new GuiLayout.Rect(board.inspector().x(), board.inspector().y()+32, board.inspector().width(), board.inspector().height()-38), state.settingScroll);
        VisualModule selected = registry.find(state.selectedId).orElse(null);
        if (selected == null && !state.globalSettings) {
            selected = state.matching(registry).stream().findFirst().orElse(null);
            if (selected != null) state.selectedId = selected.id();
        }
        if (selected == null && !state.globalSettings) {
            settingPane.decorate(12, 30, (graphics, y) -> Draw.text(graphics, font, "Choose a module to customize",
                    settingPane.area.x() + 8, y, 0xFF8896B0, false));
            return;
        }
        VisualModule module = selected;
        String name = state.globalSettings ? "General settings" : module.name();
        List<Setting<?>> settings = state.globalSettings ? globals.settings() : module.settings();
        List<Setting<?>> allSettings = settings;
        inspectedSettings=allSettings;
        visibilityRevision=settingRevision();visibilityMask=settingVisibility();
        settingPane.decorate(9, 17, (graphics, y) -> Draw.text(graphics, font,
                font.plainSubstrByWidth(name, settingPane.area.width() - 16), settingPane.area.x() + 8, y, 0xFFE9EDF7, false));
        int x = settingPane.area.x() + 8;
        int controlWidth = (settingPane.area.width() - 22) / 2;
        if (!state.globalSettings) {
            NexButton toggle = button(x, 0, controlWidth, 20, () -> module.enabled() ? "Enabled" : "Disabled",
                    module::enabled, module::toggle, module.description());
            addWidget(toggle);
            settingPane.add(toggle, 27);
        }
        NexButton reset = button(state.globalSettings ? x : x + controlWidth + 6, 0, controlWidth, 20,
                () -> "Reset settings", () -> false, () -> {
                    allSettings.forEach(setting -> { setting.reset(); state.drafts.remove(setting); state.invalidDrafts.remove(setting); });
                    requestRebuild();
                }, "Restore all settings of the selected module to their defaults");
        addWidget(reset);
        settingPane.add(reset, 27);
        SettingControls factory = new SettingControls(font, globals, state, this::registerControl,
                setting -> capturing = setting, color -> minecraft.setScreen(new ColorPickerScreen(this, color, globals, () -> {
                    state.drafts.remove(color); state.invalidDrafts.remove(color); requestRebuild();
                })), this::requestRebuild,this::showChoices);
        int y = 56;
        if (state.globalSettings && ShaderIntegration.available()) {
            NexButton shaders = button(x, 0, settingPane.area.width() - 16, 20,
                    () -> "Open Iris shader settings", () -> false, () -> ShaderIntegration.openSettings(this),
                    "Open the installed Iris public settings screen. NexVisuals never downloads or chooses shader packs.");
            addWidget(shaders); settingPane.add(shaders, y); y += 24;
        }
        if (!state.globalSettings) {
            if(!module.runtimeStatus().isEmpty()) {
                runtimeInfoLabel=module::runtimeStatus;
                runtimeInfoText=runtimeInfoLabel.get();
                runtimeInfo=button(x,0,settingPane.area.width()-16,20,runtimeInfoLabel,()->false,()->{},runtimeInfoText);
                runtimeInfo.active=false; addWidget(runtimeInfo); settingPane.add(runtimeInfo,y); y+=24;
            }
            if (!module.presets().isEmpty()) {
                settingPane.decorate(y,15,(graphics,rowY)->Draw.text(graphics,font,"Current: "+module.currentPresetName(),x,rowY+3,0xFF8796B2,false));
                y+=19;
                int index = presetSelections.getOrDefault(module.id(), 0) % module.presets().size();
                var preset = module.presets().get(index);
                NexButton choose = new NexButton(x, 0, settingPane.area.width() - 73, 20,
                        () -> preset.name() + " v", () -> false, button -> showChoices(button,
                        module.presets().stream().map(p->new ChoicePopup.Option(p.name(),p.description())).toList(),index,
                        selectedIndex->presetSelections.put(module.id(),selectedIndex)),globals);
                choose.setTooltip(Tooltip.create(Component.literal("Choose a built-in style, then Apply. "+preset.description())));
                NexButton apply = button(x + settingPane.area.width() - 68, 0, 52, 20,
                        () -> "Apply", () -> false, () -> {
                            module.applyPreset(preset);
                            state.drafts.clear(); state.invalidDrafts.clear(); requestRebuild();
                        }, preset.description());
                addWidget(choose); settingPane.add(choose, y);
                addWidget(apply); settingPane.add(apply, y); y += 24;
            }
            for (var action : module.actions()) {
                NexButton command = button(x, 0, settingPane.area.width() - 16, 20,
                        action::name, () -> false, () -> {
                            action.run().run(); state.drafts.clear(); state.invalidDrafts.clear(); requestRebuild();
                        }, action.description());
                addWidget(command); settingPane.add(command, y); y += 24;
            }
            if(!module.groups().isEmpty()) {
                int section=state.sections.getOrDefault(module.id(),0)%(module.groups().size()+1);
                String label=section==module.groups().size()?"All settings":module.groups().get(section).name();
                var sections=new java.util.ArrayList<ChoicePopup.Option>();
                module.groups().forEach(g->sections.add(new ChoicePopup.Option(g.name(),"Show this settings section")));
                sections.add(new ChoicePopup.Option("All settings","Show every setting of this module"));
                NexButton chooseSection=new NexButton(x,0,settingPane.area.width()-16,20,()->"Section: "+label+" v",()->false,
                        button->showChoices(button,sections,section,selectedIndex->{
                            state.sections.put(module.id(),selectedIndex);state.settingScroll=0;
                        }),globals);
                addWidget(chooseSection); settingPane.add(chooseSection,y); y+=24;
                if(section<module.groups().size()) settings=module.groups().get(section).settings();
            }
        }
        for (Setting<?> setting : settings) y = factory.add(settingPane, setting, y);
    }

    private void registerControl(AbstractWidget widget) { addWidget(widget); }
    private NexButton button(int x,int y,int w,int h,Supplier<String> label,BooleanSupplier selected,Runnable action,String description) {
        NexButton button=new NexButton(x,y,w,h,label,selected,action,globals);
        if(!description.isBlank()) button.setTooltip(Tooltip.create(Component.literal(description)));
        return button;
    }
    private void showChoices(AbstractWidget anchor,List<ChoicePopup.Option> options,int selected,IntConsumer choose) {
        if(options.isEmpty()) return;
        choicePopup=new ChoicePopup(anchor,width,height,options,selected,index->{choicePopup=null;choose.accept(index);requestRebuild();},
                ()->{choicePopup=null;setFocused(anchor);},globals);
    }
    private void requestRebuild() { state.requestRebuild(); }
    private long settingRevision() { long sum=0;for(var s:inspectedSettings) sum+=s.revision();return sum; }
    private long settingVisibility() { long mask=1;for(var s:inspectedSettings) mask=31*mask+(s.visible()?1:0);return mask; }
    private void prepareWidgets() {
        // Sliders can reveal dependent controls too. Rebuild once after a drag ends, never each drag frame.
        long revision=settingRevision();
        if(!isDragging() && revision!=visibilityRevision) {
            visibilityRevision=revision;long mask=settingVisibility();
            if(mask!=visibilityMask) { visibilityMask=mask;rememberScroll();requestRebuild(); }
        }
        state.rebuildIfRequested(rebuild);
    }
    private void rememberScroll() {
        for(CategoryPanel panel:panels) state.panelScroll.put(panel.category,panel.scroll());
        if(settingPane!=null) state.settingScroll=settingPane.scroll();
    }
    @Override public void tick() {
        prepareWidgets();
        if(runtimeInfo!=null) {
            String current=runtimeInfoLabel.get();
            if(!current.equals(runtimeInfoText)) { runtimeInfoText=current;runtimeInfo.setTooltip(Tooltip.create(Component.literal(current))); }
        }
        if(minecraft.level==null) worldPreview=false;
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        prepareWidgets();
        if(worldPreview) {
            String hint="Preview: F4 / Esc returns to NexVisuals";
            Draw.roundedRect(g,8,8,font.width(hint)+12,20,4,0xAA101725); Draw.text(g,font,hint,14,14,0xFFE9EDF7,false); return;
        }
        var mod=dev.nexvisuals.client.NexVisualsClient.instance();
        boolean live=mod!=null && mod.liveBackground().render(this,g);
        Draw.rect(g,0,0,width,height,live?0x280A0E18:0x650A0E18);
        int margin=board.workspace().x();
        Draw.text(g,font,font.plainSubstrByWidth("NEXVISUALS",Math.max(18,width-margin*2-218)),margin+4,15,globals.accentColor.get(),false);
        if (width >= 500) Draw.text(g,font,"Cosmetics / visual settings",margin+4,27,0xFF8998B1,false);
        CategoryPanel hovered=null;
        if(!state.narrowDetails) for(int i=panels.size()-1;i>=0;i--) if(panels.get(i).contains(mx,my)) { hovered=panels.get(i);break; }
        for(CategoryPanel panel:panels) {
            g.nextStratum();
            panel.render(g,panel==hovered?mx:-1000,panel==hovered?my:-1000,delta);
        }
        if(panels.isEmpty()) Draw.text(g,font,"No matching modules. Clear the search to show all categories.",margin+8,85,0xFFB8C4D8,false);
        g.nextStratum();
        super.render(g,state.narrowDetails?-1000:mx,state.narrowDetails?-1000:my,delta);
        if(state.narrowDetails) {
            g.nextStratum(); Draw.rect(g,0,0,width,height,0x80040810);
            var r=board.inspector();
            Draw.roundedRect(g,r.x(),r.y(),r.width(),r.height(),7,0xF5101725);
            Draw.border(g,r.x(),r.y(),r.width(),r.height(),1,Draw.withAlpha(globals.accentColor.get(),.65f));
            Draw.text(g,font,"NexVisuals / Settings",r.x()+12,r.y()+13,0xFFE9EDF7,false);
            settingPane.render(g,mx,my,delta); inspectorClose.render(g,mx,my,delta);
            if (capturing != null) {
                g.nextStratum();
                Draw.roundedRect(g,r.x()+8,r.bottom()-25,r.width()-16,20,4,0xFF1B2640);
                Draw.text(g,font,"Press a key / mouse button. Esc cancels.",r.x()+14,r.bottom()-19,0xFFE9EDF7,false);
            }
        }
        if(choicePopup!=null) choicePopup.render(g,mx,my,delta);
    }
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta) { }
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical) {
        prepareWidgets();
        if(choicePopup!=null) return choicePopup.scroll(vertical);
        if(worldPreview) return true;
        if(state.narrowDetails) {
            if(settingPane!=null && settingPane.area.contains(mx,my)) { settingPane.scrollBy((int)(-vertical*28));rememberScroll(); }
            return true;
        }
        for(int i=panels.size()-1;i>=0;i--) if(panels.get(i).contains(mx,my)) {
            panels.get(i).scrollBy((int)(-vertical*24)); rememberScroll(); return true;
        }
        return super.mouseScrolled(mx,my,horizontal,vertical);
    }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean twice) {
        prepareWidgets();
        if(choicePopup!=null) return choicePopup.mouseClicked(e,twice);
        if(worldPreview) return true;
        if(capturing!=null) { capturing.set(InputConstants.Type.MOUSE.getOrCreate(e.button()).getName());capturing=null;return true; }
        if(state.narrowDetails) {
            if(!board.inspector().contains(e.x(),e.y())) { closeSettings();return true; }
            // Native sliders/EditBoxes do not know the pane scissor. Route input through its bounds.
            var clicked=inspectorClose.mouseClicked(e,twice)?inspectorClose:settingPane.click(e,twice);
            if(clicked!=null) { setFocused(clicked);setDragging(e.button()==0); }
            return true;
        }
        for(int i=panels.size()-1;i>=0;i--) {
            CategoryPanel panel=panels.get(i);
            if(!panel.contains(e.x(),e.y())) continue;
            if(panel.headerArea().contains(e.x(),e.y())) {
                if(e.button()==1) { panel.toggle(); return true; }
                if(e.button()==0) {
                    dragged=panel;dragX=dragStartX=e.x();dragY=dragStartY=e.y();dragMoved=false;
                    state.panelOrder.remove(panel.category);state.panelOrder.add(panel.category);
                    panels.remove(i);panels.add(panel);setFocused(panel.header);setDragging(true);return true;
                }
            }
            var clicked=panel.click(e,twice);
            if(clicked!=null) { setFocused(clicked); return true; }
            return true;
        }
        return super.mouseClicked(e,twice);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy) {
        prepareWidgets();
        if(choicePopup!=null || worldPreview) return true;
        if(dragged!=null) {
            dragged.move((int)Math.round(e.x()-dragX),(int)Math.round(e.y()-dragY));
            dragX=e.x();dragY=e.y();dragMoved|=Math.abs(e.x()-dragStartX)+Math.abs(e.y()-dragStartY)>3;return true;
        }
        return super.mouseDragged(e,dx,dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) {
        prepareWidgets();
        if(choicePopup!=null || worldPreview) return true;
        if(dragged!=null) { if(!dragMoved) dragged.toggle(); dragged=null; setDragging(false); return true; }
        return super.mouseReleased(e);
    }
    @Override public boolean charTyped(CharacterEvent e) {
        prepareWidgets();
        return choicePopup!=null || worldPreview || super.charTyped(e);
    }
    @Override public boolean keyReleased(KeyEvent e) {
        prepareWidgets();
        return choicePopup!=null || worldPreview || super.keyReleased(e);
    }
    @Override public boolean keyPressed(KeyEvent e) {
        prepareWidgets();
        if(choicePopup!=null) return choicePopup.keyPressed(e);
        if(worldPreview) { if(e.key()==GLFW.GLFW_KEY_ESCAPE || e.key()==GLFW.GLFW_KEY_F4) worldPreview=false;return true; }
        if(capturing!=null) {
            if(e.key()!=GLFW.GLFW_KEY_UNKNOWN && e.key()!=GLFW.GLFW_KEY_ESCAPE) capturing.set(InputConstants.getKey(e).getName());
            capturing=null;return true;
        }
        if(e.key()==GLFW.GLFW_KEY_F4 && minecraft.level!=null) { worldPreview=true;return true; }
        if(e.key()==GLFW.GLFW_KEY_F && (e.modifiers()&GLFW.GLFW_MOD_CONTROL)!=0) {
            if(state.narrowDetails) { closeSettings();focusSearch=true; } else setFocused(search);
            return true;
        }
        if(e.key()==GLFW.GLFW_KEY_ESCAPE && state.narrowDetails) { closeSettings();return true; }
        if((e.key()==GLFW.GLFW_KEY_PAGE_DOWN || e.key()==GLFW.GLFW_KEY_PAGE_UP) && settingPane!=null) {
            settingPane.scrollBy((e.key()==GLFW.GLFW_KEY_PAGE_DOWN?1:-1)*Math.max(28,settingPane.area.height()-25));rememberScroll();return true;
        }
        boolean handled=super.keyPressed(e);
        for(CategoryPanel panel:panels) panel.revealFocused();
        if(settingPane!=null) settingPane.revealFocused(); rememberScroll();return handled;
    }
    @Override public void resize(int w,int h) { rememberScroll();super.resize(w,h); }
    public void selectModule(String id) {
        state.selectedId=id;registry.find(id).ifPresent(module->state.category=module.category());
        state.query="";state.globalSettings=false;state.narrowDetails=true;state.settingScroll=0;
        requestRebuild();
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public void removed() { rememberScroll();state.remember(globals);save.run(); }
    @Override public boolean isPauseScreen() { return false; }
}

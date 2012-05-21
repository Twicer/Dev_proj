package com.wisemapping.ncontroller;


import com.wisemapping.exceptions.WiseMappingException;
import com.wisemapping.filter.UserAgent;
import com.wisemapping.model.MindMap;
import com.wisemapping.model.MindmapUser;
import com.wisemapping.model.User;
import com.wisemapping.security.Utils;
import com.wisemapping.service.MindmapService;
import com.wisemapping.view.MindMapBean;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/c/")
public class MindmapController {
    @Autowired
    private MindmapService mindmapService;

    @RequestMapping(value = "map/{id}/export")
    public ModelAndView export(@PathVariable int id) throws IOException {
        final MindMapBean modelObject = findMindmapBean(id);
        return new ModelAndView("mindmapExport", "mindmap", modelObject);
    }

    @RequestMapping(value = "map/{id}/details")
    public ModelAndView showDetails(@PathVariable int id) {
        final MindMapBean modelObject = findMindmapBean(id);
        final ModelAndView view = new ModelAndView("mindmapDetail", "wisemapDetail", modelObject);
        view.addObject("user", Utils.getUser());
        return view;
    }

    @RequestMapping(value = "map/{id}/print")
    public ModelAndView showPrintPage(@PathVariable int id) {
        final MindMap mindmap = findMindmap(id);
        final ModelAndView view = new ModelAndView("mindmapPrint", "mindmap", mindmap);
        view.addObject("user", Utils.getUser());
        return view;
    }

    @RequestMapping(value = "map/{id}/publish")
    public ModelAndView showPublishPage(@PathVariable int id) {
        final MindMap mindmap = findMindmap(id);
        final ModelAndView view = new ModelAndView("mindmapPublish", "mindmap", mindmap);
        view.addObject("user", Utils.getUser());
        return view;
    }

    @RequestMapping(value = "map/{id}/edit")
    public ModelAndView editMap(@PathVariable int id, @NotNull HttpServletRequest request) {
        ModelAndView view;
        final UserAgent userAgent = UserAgent.create(request);
        if (userAgent.needsGCF()) {
            view = new ModelAndView("gcfPluginNeeded");
//            view.addObject(MINDMAP_ID_PARAMETER, mindmapId);
        } else {

            final MindMap mindmap = mindmapService.getMindmapById(id);
            view = new ModelAndView("mindmapEditor", "mindmap", mindmap);
            view.addObject("editorTryMode", false);
            final boolean showHelp = isWelcomeMap(mindmap);
            view.addObject("showHelp", showHelp);
            view.addObject("user", Utils.getUser());
        }
        return view;
    }

    @RequestMapping(value = "collaborator")
    public ModelAndView showCollaborator(@RequestParam(required = true) long mapId) {
        final MindMapBean modelObject = findMindmapBean(mapId);
        return new ModelAndView("mindmapCollaborator", "mindmap", modelObject);
    }

    @RequestMapping(value = "viewer")
    public ModelAndView viewer(@RequestParam(required = true) long mapId) {
        final MindMapBean modelObject = findMindmapBean(mapId);
        return new ModelAndView("mindmapViewer", "wisemapsList", modelObject);
    }

    @RequestMapping(value = "changeStatus")
    public ModelAndView changeStatus(@RequestParam(required = true) long mapId) throws WiseMappingException {
        final MindMap mindmap = findMindmap(mapId);
        boolean isPublic = !mindmap.isPublic();
        mindmap.setPublic(isPublic);
        mindmapService.updateMindmap(mindmap, false);
        return new ModelAndView("mindmapDetail", "wisemapDetail", new MindMapBean(mindmap));
    }

    @RequestMapping(value = "mymaps")
    public ModelAndView list(@NotNull HttpServletRequest request) {
        final HttpSession session = request.getSession(false);

        // Try to loaded from the request ...
        UserAgent userAgent = null;
        if (session != null) {
            userAgent = (UserAgent) session.getAttribute(USER_AGENT);
        }

        // I could not loaded. I will create a new one...
        if (userAgent == null) {
            userAgent = UserAgent.create(request);
            if (session != null) {
                session.setAttribute(USER_AGENT, userAgent);
            }
        }

        // It's a supported browser ?.
        final UserAgent.OS os = userAgent.getOs();

        final User user = Utils.getUser();
        final ModelAndView view = new ModelAndView("mindmapList", "wisemapsList", findMindMapBeanList(user));
        view.addObject("isMAC", os == UserAgent.OS.MAC);
        view.addObject("user", user);
        return view;
    }

    @RequestMapping(value = "updateMindmap")
    public ModelAndView updateMindmap(@RequestParam(required = true) long mapId, @RequestParam(required = true) String title, @RequestParam(required = true) String description, @NotNull HttpServletRequest request) throws WiseMappingException {
        final MindMap mindmap = findMindmap(mapId);
        mindmap.setTitle(title);
        mindmap.setDescription(description);

        mindmapService.updateMindmap(mindmap, false);
        return list(request);
    }

    private MindMap findMindmap(long mapId) {
        final MindMap mindmap = mindmapService.getMindmapById((int) mapId);
        if (mindmap == null) {
            throw new IllegalArgumentException("Mindmap could not be found");
        }
        return mindmap;
    }

    private List<MindMapBean> findMindMapBeanList(@NotNull User user) {
        final List<MindmapUser> userMindmaps = mindmapService.getMindmapUserByUser(user);

        final List<MindMapBean> mindMapBeans = new ArrayList<MindMapBean>(userMindmaps.size());
        for (MindmapUser mindmap : userMindmaps) {
            mindMapBeans.add(new MindMapBean(mindmap.getMindMap()));
        }
        return mindMapBeans;
    }

    private MindMapBean findMindmapBean(long mapId) {
        return new MindMapBean(findMindmap(mapId));
    }

    private boolean isWelcomeMap(MindMap map) {
        return map.getTitle().startsWith("Welcome ");
    }


    private static final String USER_AGENT = "wisemapping.userAgent";
}

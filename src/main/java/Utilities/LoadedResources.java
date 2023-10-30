package Utilities;

import Command.LoadWebsite;

import java.util.ArrayList;
import java.util.List;

public class LoadedResources {
    private List<String> resources = new ArrayList<>();

    public List<String> getResources() {
        return resources;
    }

    public void setResources(List<String> resources) {
        this.resources = resources;
    }

    public boolean alreadyLoaded(String res) {
        return getResources().contains(res);
    }
    public void addResource(String res) {
        getResources().add(res);
    }
}
